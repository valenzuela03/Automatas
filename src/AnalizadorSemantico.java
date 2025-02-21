import utils.Tokens;
import utils.Variable;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorSemantico {
    private List<TablaToken> tokenLista;
    private int indice;
    private TablaToken tokenActual;
    private List<Variable> variables;
    private boolean error;

    public AnalizadorSemantico(){
        tokenLista = new ArrayList<>();
        variables = new ArrayList<>();
    }

    public void prepararAnalizadorSemantico(List<TablaToken> tokenLista) {
        variables.clear();
        this.tokenLista = tokenLista;
        indice = 0;
        error = false;
        tokenActual = tokenLista.get(indice);
    }
    private void siguienteToken() {
        if(!error){
            tokenActual = (++indice < tokenLista.size() ? tokenLista.get(indice): null);
        }
    }

    private boolean esToken(Tokens tokenEsperado) {
        return tokenActual != null && tokenActual.getTokens() == tokenEsperado;
    }

    private boolean operadorComparativo(TablaToken token){
        return token != null && switch (token.getTokens()){
            case  IGUALDAD, MAYOR, MENOR -> true;
            default -> false;
        };
    }

    public boolean analizar(){
        programa();
        return !error;
    }

    private void programa(){
        siguienteToken();
        while (!error && tokenActual != null) {
            if(esToken(Tokens.PR) && tokenActual.getNombre().equals("int") || tokenActual.getNombre().equals("float") ||  tokenActual.getNombre().equals("string")){
                String tipo = tokenActual.getNombre();
                siguienteToken();
                String nombre = tokenActual.getNombre();
                if(existe(nombre)){
                    error = true;
                    return;
                }
                variables.add(new Variable(tipo, nombre, ""));
            } else if(esToken(Tokens.PR) && tokenActual.getNombre().equals("if")){
                validarIf();
            }
            else if(esToken(Tokens.IDENTIFICADOR)){
                String nombre = tokenActual.getNombre();
                if(!existe(nombre)){
                    error = true;
                    return;
                }
                Variable variable = obtenerVariable(nombre);
                if (variable == null) return;
                siguienteToken();
                siguienteToken();
                StringBuilder valor = new StringBuilder();
                boolean esperoOperador = true;
                String tipo = variable.getTipo();
                while(!esToken(Tokens.PUNTO_Y_COMA) && tokenActual != null){
                    System.out.println(tokenActual.getTokens());
                    if(esperoOperador){
                        if(!esOperacionValida(tipo)){
                            error = true;
                            return;
                        }
                        esperoOperador = false;
                    }else{
                        if(!operadorAritmetico(tokenActual)){
                            error = true;
                            return;
                        }
                        esperoOperador = true;
                    }
                    valor.append(tokenActual.getNombre());
                    siguienteToken();
                }
                if(esperoOperador){
                    error = true;
                    return;
                }
                variable.setValor(valor.toString());
            }else if(esToken(Tokens.PR) && (tokenActual.getNombre().equals("write") || tokenActual.getNombre().equals("read"))){
                siguienteToken();
                siguienteToken();
                if(esToken(Tokens.IDENTIFICADOR)){
                    if(!existe(tokenActual.getNombre())){
                        error = true;
                        return;
                    }
                }
            }
            siguienteToken();
        }
    }

    private void validarIf(){
        siguienteToken();
        siguienteToken();
        if(esToken(Tokens.IDENTIFICADOR)){
            if(!existe(tokenActual.getNombre())){
                error = true;
                return;
            }
            siguienteToken();
        }
        siguienteToken();
        if(esToken(Tokens.IDENTIFICADOR)){
            if(!existe(tokenActual.getNombre())){
                error = true;
                return;
            }
            siguienteToken();
        }
    }

    private boolean esOperacionValida(String tipo){
        return switch (tipo) {
            case "int" -> esToken(Tokens.VALOR_NUMERO);
            case "float" -> esToken(Tokens.VALOR_FLOAT);
            case "string" -> esToken(Tokens.VALOR_CADENA);
            default -> false;
        };
    }

    private boolean existe(String nombre){
        for( Variable v : variables ){
            if(v.getNombre().equals(nombre)){
                return true;
            }
        }
        return false;
    }

    private Variable obtenerVariable(String nombre){
        for( Variable v : variables ){
            if(v.getNombre().equals(nombre)){
                return v;
            }
        }
        return null;
    }

    private boolean operadorAritmetico(TablaToken token){
        return token != null && switch (token.getTokens()){
            case SUMA, RESTA, MULTIPLICACION, DIVISION -> true;
            default -> false;
        };
    }

    public void imprimir(){
        for( Variable v : variables){
            System.out.println(v.toString());
        }
    }

}
