import utils.Tokens;
import utils.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AnalizadorBajoNivel  {
    private List<TablaToken> tokenLista;
    private List<Variable> variables;
    private StringBuilder codigo;
    private TablaToken tokenActual;
    private int index;
    private int ifLabel;
    private int writeLabel;
    private int readLabel;


    private StringBuilder codigoObjeto;
    private String segmentoDeMemoriaData;
    private String segmentoDeMemoriaCodigo;
    private HashMap<String, String> mapaMemoriaVariablesInfo;
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
        codigoObjeto = new StringBuilder();
        segmentoDeMemoriaData = "0000 0000 0000 0000";
        segmentoDeMemoriaCodigo = "0000 0000 0000 0000";
        mapaMemoriaVariablesInfo = new HashMap<>();
    }

    public void analizar(){
        codigo.append(".model small\n")
                .append(".stack 100h\n");
        puntoData();
        puntoCode();
    }

    private void puntoData() {
        codigo.append(".DATA\n");
        codigoObjeto.append(".DATA\n");
        for (Variable v : variables) {
            switch (v.getTipo()) {
                case "int":
                    codigo.append("\t").append(v.getNombre()).append(" dw ?\n");
                    mapaMemoriaVariablesInfo.put(v.getNombre(), segmentoDeMemoriaData);
                    codigoObjeto.append(segmentoDeMemoriaData).append("\t").append("0000 0000 0000 0000\n");
                    segmentoDeMemoriaData = incrementarSegmentoMemoria(segmentoDeMemoriaData, 2);
                    break;
                case "float":
                    codigo.append("\t").append(v.getNombre()).append(" dd ?\n");
                    mapaMemoriaVariablesInfo.put(v.getNombre(), segmentoDeMemoriaData);
                    codigoObjeto.append(segmentoDeMemoriaData).append("\t").append("0000 0000 0000 0000\n");
                    segmentoDeMemoriaData = incrementarSegmentoMemoria(segmentoDeMemoriaData, 2);
                    break;
                default:
                    codigo.append("\t").append(v.getNombre()).append(" db 256 Dup (\"$\")\n");
                    mapaMemoriaVariablesInfo.put(v.getNombre(), segmentoDeMemoriaData);
                    for(int i=0; i<256; i++){
                        codigoObjeto.append(segmentoDeMemoriaData).append("\t").append("0010 0100\n");
                        segmentoDeMemoriaData = incrementarSegmentoMemoria(segmentoDeMemoriaData, 1);
                    }
                    break;
            }
        }
        codigo.append("\t").append("salto db 0Dh,0Ah, \"$\"\n").append("\n");
        mapaMemoriaVariablesInfo.put("salto", segmentoDeMemoriaData);
        codigoObjeto.append(segmentoDeMemoriaData).append("\t").append("0000 1101").append("\n");
        segmentoDeMemoriaData = incrementarSegmentoMemoria(segmentoDeMemoriaData, 1);
        codigoObjeto.append(segmentoDeMemoriaData).append("\t").append("0000 1010").append("\n");
        segmentoDeMemoriaData = incrementarSegmentoMemoria(segmentoDeMemoriaData, 1);
        codigoObjeto.append(segmentoDeMemoriaData).append("\t").append("0010 0100").append("\n");
        segmentoDeMemoriaData = incrementarSegmentoMemoria(segmentoDeMemoriaData, 1);
    }
    private void puntoCode() {
        codigo.append(".CODE\n");
        codigoObjeto.append(".CODE\n");
        codigo.append("inicio:\n");
        codigo.append("\t").append("MOV AX, @DATA").append("\n");
        codigo.append("\t").append("MOV DS, AX").append("\n");

        while (tokenActual != null){
            recorrerTokens();
            siguienteToken();
        }

        codigo.append("\t").append("MOV AH, 4Ch").append("\n")
                .append("\t").append("INT 21h").append("\n");
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0100 0100 1100\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 1101 0010 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

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
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 0001 0011 1110 ").append(obtenerVariableEnMemoria(valor1)).append(numeroABinario(valor2)).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 6);
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t");
                switch (operador){
                    case "==":
                        codigo.append("JNE ");
                        codigoObjeto.append("0111 0101 ");
                        break;
                    case "<>":
                        codigo.append("JE ");
                        codigoObjeto.append("0111 0100 ");
                        break;
                    case ">":
                        codigo.append("JLE ");
                        codigoObjeto.append("0111 1110 ");
                        break;
                    case "<":
                        codigo.append("JGE ");
                        codigoObjeto.append("0111 1101 ");
                        break;
                    case ">=":
                        codigo.append("JL ");
                        codigoObjeto.append("0111 1100 ");
                        break;
                    case "<=":
                        codigo.append("JG ");
                        codigoObjeto.append("0111 1111 ");
                        break;
                }
                codigo.append("ELSE").append(ifLabel).append("\n");
                codigoObjeto.append("0000 1011").append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);
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
            String memoriaActual = obtenerVariableEnMemoria(variable.getNombre());
            if(esToken(Tokens.VALOR_CADENA)){
                char[] characters = variable.getValor().toCharArray();
                for (int j = 0; j < characters.length; j++) {
                    codigo.append("\t").append("MOV ").append(variable.getNombre()).append("[").append(j).append("], '").append(characters[j]).append("'").append("\n");
                    codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 0110 0000 0110 ").append(memoriaActual).append(caracterABinario(characters[j])).append("\n");
                    memoriaActual = incrementarSegmentoMemoria(memoriaActual, 1);
                    segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 5);
                }
            }else{
                String asignacionNum = AsignacionNum(variable);
                codigo.append(asignacionNum).append("\n");
            }
        }

    }
    private String caracterABinario(char character){
        int ascii = character;
        String binary = String.format("%8s", Integer.toBinaryString(ascii)).replace(' ', '0');
        return " " + binary.substring(0, 4) + " " + binary.substring(4);
    }
    private String AsignacionNum(Variable variable) {
        String codigoOperacion = "";
        if (!variable.getValor().contains("+") && !variable.getValor().contains("-") && !variable.getValor().contains("*") && !variable.getValor().contains("/")) {
            codigoOperacion += "\tMOV " + variable.getNombre()+", " + variable.getValor() + "\n";
            codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 0111 0000 0110 ").append(obtenerVariableEnMemoria(variable.getNombre())).append(" ").append(numeroABinario(variable.getValor())).append("\n");
            segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 6);
        }else{
            if (variable.getValor().contains("+")) {
                String[] parts = variable.getValor().split("\\+");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1000 ").append(numeroABinario(parts[0].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
                codigoOperacion += "\tADD AX, " + parts[1].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0000 0101 ").append(numeroABinario(parts[1].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
            } else if (variable.getValor().contains("-")) {
                String[] parts = variable.getValor().split("-");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1000 ").append(numeroABinario(parts[0].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
                codigoOperacion += "\tSUB AX, " + parts[1].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0010 1101 ").append(numeroABinario(parts[1].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
            } else if (variable.getValor().contains("*")) {
                String[] parts = variable.getValor().split("\\*");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1000 ").append(numeroABinario(parts[0].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
                codigoOperacion += "\tMOV BX, " + parts[1].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1011 ").append(numeroABinario(parts[1].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
                codigoOperacion += "\tIMUL BX\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1111 0111 1110 0011 ").append(obtenerVariableEnMemoria(variable.getNombre())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);
            } else if (variable.getValor().contains("/")) {
                String[] parts = variable.getValor().split("/");
                codigoOperacion += "\tMOV AX, " + parts[0].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1000 ").append(numeroABinario(parts[0].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
                codigoOperacion += "\tXOR DX, DX\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 0011 1101 0010\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);
                codigoOperacion += "\tMOV BX, " + parts[1].trim() + "\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1011 ").append(numeroABinario(parts[1].trim())).append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);
                codigoOperacion += "\tIDIV BX\n";
                codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1111 0111 1111 1011 ").append("\n");
                segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);
            }
            codigoOperacion += "\tMOV " + variable.getNombre() + ", AX\n";
            codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1011 0001 0000 ").append(obtenerVariableEnMemoria(variable.getNombre())).append("\n");
            segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);
        }
        return codigoOperacion;
    }

    private String imprimirString(String variable){
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1101 0001 0110 ")
                .append(obtenerVariableEnMemoria(variable)).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0100 0000 1001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 1101 0010 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        return "\t" + "LEA DX, " + variable + "\n" +
                "\t" + "MOV AH, 09h\n" +
                "\t" + "INT 21h\n" +
                saltoLinea();
    }

    private String imprimirNum(String variable){
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1011 0000 0110  ")
                .append(obtenerVariableEnMemoria(variable)).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 0011 1100 1001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 0011 1101 0010\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1011 ")
                .append(numeroABinario("10")).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1111 0111 1111 0011\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 0010 1100 0010 ")
                .append(numeroABinario("30")).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0101 0010\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 1);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0100 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 1);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 1101 0000 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0111 0101 1000 1011\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 0011 1101 0010\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0101 1010\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 1);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0010 0000 0010\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 1101 0010 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1110 0010 1000 0101\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

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
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1110 0000 0000 0000 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0100 0000 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 1101 0010 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 1100 0000 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0111 0100 1000 1011\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1000 1000 0100 ")
                .append(obtenerVariableEnMemoria(variable)).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0100 0110\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 1);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1110 1011 1000 1101\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 0111 1000 0100 ")
                .append(obtenerVariableEnMemoria(variable)).append(" 0010 0100\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);


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
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1011 0000 0000 0000 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0100 0000 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 1101 0010 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0011 1100 0000 1101\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 0000 1110 1000 0011 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0100 0000 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1011 1100 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 1000 0000 1010 0000 0000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 3);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1111 0111 1101 1011\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1011 1100 0011\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("0000 0011 1100 1011\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1110 1011 1001 1000\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1001 0001 1110 ")
                .append(obtenerVariableEnMemoria(variable)).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);

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
        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1000 1101 0001 0110 ").append(obtenerVariableEnMemoria("new_line")).append("\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 4);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1011 0010 0000 1001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        codigoObjeto.append(segmentoDeMemoriaCodigo).append("\t").append("1100 1101 0010 0001\n");
        segmentoDeMemoriaCodigo = incrementarSegmentoMemoria(segmentoDeMemoriaCodigo, 2);

        return "\t" +  "LEA DX, salto\n" +
                "\t" + "MOV AH, 09h\n" +
                "\t" + "INT 21h\n";
    }

    public String getCode(){
        return codigo.toString();
    }

    public String getCodigoObjeto(){
        return codigoObjeto.toString();
    }

    public String incrementarSegmentoMemoria(String segmento, int bytesAAgregar) {
        String binarioLimpio = segmento.replace(" ", "");
        int actual = Integer.parseUnsignedInt(binarioLimpio, 2);
        actual += bytesAAgregar;
        actual &= 0xFFFF;
        String nuevoBinario = String.format("%16s", Integer.toBinaryString(actual)).replace(' ', '0');
        return nuevoBinario.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private String obtenerVariableEnMemoria(String nombreVar){
        return mapaMemoriaVariablesInfo.getOrDefault(nombreVar, "0000 0000 0000 0000");
    }

    private String numeroABinario(String numero) {
        int num = Integer.parseInt(numero);
        String binario = Integer.toBinaryString(num);

        StringBuilder binarioCompleto = new StringBuilder(binario);
        while (binarioCompleto.length() < 16) {
            binarioCompleto.insert(0, "0");
        }

        String nibble1 = binarioCompleto.substring(0, 4);
        String nibble2 = binarioCompleto.substring(4, 8);
        String nibble3 = binarioCompleto.substring(8, 12);
        String nibble4 = binarioCompleto.substring(12, 16);

        return nibble3 + " " + nibble4 + " " + nibble1 + " " + nibble2;
    }



}
