import utils.Tokens;
import utils.Variable;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorBajoNivel  {
    private List<TablaToken> tokenLista;
    private List<Variable> variables;
    private StringBuilder codigo;
    private TablaToken tokenActual;
    private int index;
    private int ifLabel;
    private int writeLabel;
    private int readLabel;
    public AnalizadorBajoNivel(){
        tokenLista = new ArrayList<>();
        variables = new ArrayList<>();
    }

    private void siguienteToken(){
        index++;
        tokenActual = index < tokenLista.size() ? tokenLista.get(index) : null;
    }
    private boolean esToken(Tokens tokenEsperado) {
        return tokenActual != null && tokenActual.getTokens() == tokenEsperado;
    }

    public void prepararAnalizadorBajoNivel(List<TablaToken> tokenLista, List<Variable> variables)  {
        this.variables = variables;
        this.tokenLista = tokenLista;
        codigo = new StringBuilder();
        index = 0;
        ifLabel = 0;
        writeLabel = 0;
        readLabel = 0;
        tokenActual = tokenLista.get(index);
    }

    public void analizar(){
        codigo.append(".model small\n")
                .append(".stack 100h\n");
        puntoData();
        puntoCode();
    }

    private void puntoData() {
        codigo.append(".DATA\n");
        for (Variable v : variables ){
            switch (v.getTipo()){
                case "int"-> codigo.append("\t").append(v.getNombre()).append(" dw ?\n");
                case "float"-> codigo.append("\t").append(v.getNombre()).append(" dd ?\n");
                default -> codigo.append("\t").append(v.getNombre()).append(" db 256 Dup (\"$\")\n");
            }
        }
        codigo.append("\t").append("salto db 0Dh,0Ah, \"$\"\n").append("\n");
    }
    private void puntoCode() {
        codigo.append(".CODE\n");
        codigo.append("inicio:\n");
        codigo.append("\t").append("MOV AX, @DATA").append("\n");
        codigo.append("\t").append("MOV DS, AX").append("\n");

        while (tokenActual != null){
            recorrerTokens();
            siguienteToken();
        }

        codigo.append("\t").append("MOV AH, 4Ch").append("\n")
                .append("\t").append("INT 21h").append("\n");

        codigo.append("END inicio");
    }

    private void recorrerTokens() {
        if(esToken(Tokens.PARENTESIS_ABIERTO) || esToken(Tokens.PARENTESIS_CERRADO)){
            siguienteToken();
            return;
        }
        if(esToken(Tokens.PR)){
            if(tokenActual.getNombre().equals("int") || tokenActual.getNombre().equals("float") ||  tokenActual.getNombre().equals("string")) {
                while (tokenActual != null && tokenActual.getTokens() != Tokens.PUNTO_Y_COMA) {
                    siguienteToken();
                }
            }
            else if("read".equals(tokenActual.getNombre())){
                siguienteToken();
                siguienteToken();
                Variable variable = variables.stream().filter(v -> v.getNombre().equals(tokenActual.getNombre())).findFirst().orElse(null);
                if (variable == null) return;
                if (variable.getTipo().equals("string")) {
                    codigo.append(readString(variable.getNombre()));
                }else{
                    codigo.append(readNum(variable.getNombre()));
                }
            }else if("write".equals(tokenActual.getNombre())){
                siguienteToken();
                siguienteToken();
                Variable variable = variables.stream().filter(v -> v.getNombre().equals(tokenActual.getNombre())).findFirst().orElse(null);
                if (variable == null) return;
                if(variable.getTipo().equals("string")){
                    codigo.append(imprimirString(variable.getNombre()));
                }else{
                    ++writeLabel;
                    codigo.append(imprimirNum(variable.getNombre()));
                }
            }else if("if".equals(tokenActual.getNombre())){
                siguienteToken();
                siguienteToken();

                String valor1 = tokenActual.getNombre();
                siguienteToken();
                String operador = tokenActual.getNombre();
                siguienteToken();
                String valor2 = tokenActual.getNombre();
                siguienteToken();
                siguienteToken();

                codigo.append("\t").append("CMP ").append(valor1).append(", ").append(valor2).append("\n");
                codigo.append("\t");
                switch (operador){
                    case "==":
                        codigo.append("JNE ");
                        break;
                    case "<>":
                        codigo.append("JE ");
                        break;
                    case ">":
                        codigo.append("JLE ");
                        break;
                    case "<":
                        codigo.append("JGE ");
                        break;
                    case ">=":
                        codigo.append("JL ");
                        break;
                    case "<=":
                        codigo.append("JG ");
                        break;
                }
                codigo.append("ELSE").append(ifLabel).append("\n");
                while (!esToken(Tokens.LLAVE_CERRADA)){
                    siguienteToken();
                    recorrerTokens();
                }
                codigo.append("\t").append("JMP END_IF").append(ifLabel).append("\n");
                codigo.append("ELSE").append(ifLabel).append(":\n");
                siguienteToken();
                if(esToken(Tokens.PR) && "else".equals(tokenActual.getNombre())){
                    siguienteToken();
                    siguienteToken();
                    while (!esToken(Tokens.LLAVE_CERRADA)){
                        recorrerTokens();
                        siguienteToken();
                    }
                }

                codigo.append("END_IF").append(ifLabel).append(":\n");

            }
        }
        else if(esToken(Tokens.IDENTIFICADOR)) {
            Variable variable = variables.stream().filter(v -> v.getNombre().equals(tokenActual.getNombre())).findFirst().orElse(null);
            if(variable == null) return;
            siguienteToken();
            siguienteToken();
            if(esToken(Tokens.VALOR_CADENA)){
                char[] characters = variable.getValor().toCharArray();
                for (int j = 0; j < characters.length; j++) {
                    codigo.append("\t").append("MOV ").append(variable.getNombre()).append("[").append(j).append("], '").append(characters[j]).append("'").append("\n");
                }
            }else{
                String asignacionNum = AsignacionNum(variable);
                codigo.append(asignacionNum).append("\n");
            }
        }
    }

    private static String AsignacionNum(Variable variable) {
        String codigoOperacion = "";
        if (!variable.getValor().contains("+") && !variable.getValor().contains("-") && !variable.getValor().contains("*") && !variable.getValor().contains("/")) {
            codigoOperacion += "\tMOV " + variable.getNombre()+", " + variable.getValor() + "\n";
        }else{
            if (variable.getValor().contains("+")) {
                String[] parts = variable.getValor().split("\\+");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoOperacion += "\tADD AX, " + parts[1].trim() + "\n";
            } else if (variable.getValor().contains("-")) {
                String[] parts = variable.getValor().split("-");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoOperacion += "\tSUB AX, " + parts[1].trim() + "\n";
            } else if (variable.getValor().contains("*")) {
                String[] parts = variable.getValor().split("\\*");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoOperacion += "\tMOV BX, " + parts[1].trim() + "\n";
                codigoOperacion += "\tIMUL BX\n";
            } else if (variable.getValor().contains("/")) {
                String[] parts = variable.getValor().split("/");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoOperacion += "\tXOR DX, DX\n";
                codigoOperacion += "\tMOV BX, " + parts[1].trim() + "\n";
                codigoOperacion += "\tIDIV BX\n";
            }
            codigoOperacion += "\tMOV " + variable.getNombre() + ", AX\n";
        }
        return codigoOperacion;
    }

    private String imprimirString(String variable){
        return "\t" + "MOV DX, OFFSET " + variable + "\n" +
                "\t" + "MOV AH, 09h\n" +
                "\t" + "INT 21h\n" +
                saltoLinea();
    }

    private String imprimirNum(String variable){
        return "\t" + "MOV AX, " + variable + "\n" +
                "\t" + "XOR CX, CX\n" +
                "CICLO" + writeLabel + ":\n" +
                "\t" + "XOR DX, DX\n" +
                "\t" + "MOV BX, 10\n" +
                "\t" + "DIV BX\n" +
                "\t" + "ADD DL, 30h\n" +
                "\t" + "PUSH DX\n" +
                "\t" + "INC CX\n" +
                "\t" + "CMP AX, 0\n" +
                "\t" + "JNE CICLO" + writeLabel + "\n" +
                "MOSTRAR" + writeLabel + ":\n" +
                "\t" + "XOR DX, DX\n" +
                "\t" + "POP DX\n" +
                "\t" + "MOV AH, 2\n" +
                "\t" + "INT 21h\n" +
                "\t" + "LOOP MOSTRAR" + writeLabel + "\n" +
                saltoLinea();
    }

    private String readString(String variable){
        return "\t" + "MOV SI, 0\n" +
                "\t" + "LEER" + readLabel + ":\n" +
                "\t" + "MOV AH, 01h\n" +
                "\t" + "INT 21h\n" +
                "\t" + "CMP AL, 0Dh\n" +
                "\t" + "JE FIN" + readLabel + "\n" +
                "\t" + "MOV " + variable + "[SI], AL\n" +
                "\t" + "INC SI\n" +
                "\t" + "JMP LEER" + readLabel + "\n" +
                "\t" + "FIN" + readLabel + ":\n" +
                "\t" + "MOV " + variable + "[SI], '$'\n" +
                saltoLinea();
    }

    private String readNum(String variable){
        return "\t" + "MOV BX, 0\n" +
                "\t" + "LEERNUM" + readLabel + ":\n" +
                "\t" + "MOV AH, 01h\n" +
                "\t" + "INT 21h\n" +
                "\t" + "CMP AL, 0Dh\n" +
                "\t" + "JE FINNUM" + readLabel + "\n" +
                "\t" + "SUB AL, 48\n" +
                "\t" + "MOV AH, 0\n" +
                "\t" + "MOV CX, AX\n" +
                "\t" + "MOV AX, 10\n" +
                "\t" + "MUL BX\n" +
                "\t" + "MOV BX, AX\n" +
                "\t" + "ADD BX, CX\n" +
                "\t" + "JMP LEERNUM" + readLabel + "\n" +
                "\t" + "FINNUM" + readLabel + ":\n" +
                "\t" + "MOV " + variable + ", BX\n" +
                saltoLinea();
    }

    private String saltoLinea(){
        return "\t" +  "MOV DX, OFFSET salto\n" +
                "\t" + "MOV AH, 09h\n" +
                "\t" + "INT 21h\n";
    }

    public String getCode(){
        return codigo.toString();
    }
}
