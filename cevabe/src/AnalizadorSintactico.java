import java.util.ArrayList;
import java.util.List;

public class AnalizadorSintactico {
    private List<TablaToken> tokenLista;
    private int indice;
    private TablaToken tokenActual;
    private boolean error;

    public AnalizadorSintactico() {
        tokenLista = new ArrayList<>();
    }

    public void prepararAnalizadorSintactico(List<TablaToken> tokenLista) {
        this.tokenLista = tokenLista;
        indice = 0;
        error = false;
        tokenActual = tokenLista.get(indice);
    }

    public boolean analizar(){
        programa();
        return !error;
    }

    private void programa(){
        if(esToken(Tokens.PR) && tokenActual.getNombre().equals("Principio")){
            siguienteToken();
            listaInstrucciones();
            if(esToken(Tokens.PR) && tokenActual.getNombre().equals("Final")) {
                siguienteToken();
                    if (indice < tokenLista.size()) {
                        error = true;
                    }
            }else{
                error = true;
            }
        }else{
            error = true;
        }
    }

    private void listaInstrucciones() {
        if (error) return;
        // Mientras no haya un error y no sea el final
        while (!error && tokenActual != null && !("Final".equals(tokenActual.getNombre()))) {
            instruccion();
            if (tokenActual != null && !esToken(Tokens.PUNTO_Y_COMA)) {
                error = true;
                break;
            }
            siguienteToken();
        }
    }

    private void instruccion(){
        if(error) return;
        if(esToken(Tokens.IDENTIFICADOR)){
            siguienteToken();
            if(esToken(Tokens.IGUAL)){
                siguienteToken();
                expresion();
            }else{
                error = true;
            }
        } else if(esToken(Tokens.PR)){
            switch (tokenActual.getNombre()) {
                case "int", "float", "string" -> {
                    siguienteToken();
                    declaracion();
                }
                case "if" -> comparacionIf();
                case "read" -> read();
                case "write" -> write();
                default -> error=true;
            }
        }else{
            error = true;
        }
    }

    private void declaracion(){
        if(error) return;
        if(esToken(Tokens.IDENTIFICADOR)) {
            siguienteToken();
        }else{
            error = true;
        }
    }

    private void comparacionIf(){
        if(error) return;
        siguienteToken();
        if(esToken(Tokens.PARENTESIS_ABIERTO)){
            siguienteToken();
            comparacion();
            if(esToken(Tokens.PARENTESIS_CERRADO)){
                siguienteToken();
                codigoBloque();

                if(esToken(Tokens.PR) && "else".equals(tokenActual.getNombre())){
                    siguienteToken();
                    codigoBloque();
                }
            }else{
                error = true;
            }
        }else{
            error = true;
        }
    }

    private void codigoBloque(){
        if(error) return;
        if(esToken(Tokens.LLAVE_ABIERTA)){
            siguienteToken();
            while (!error && tokenActual != null && !esToken(Tokens.LLAVE_CERRADA)){
                instruccion();
                siguienteToken();
            }
            if(esToken(Tokens.LLAVE_CERRADA)){
                siguienteToken();
            }
        }
    }

    private void comparacion(){
        if(error) return;
        expresion();
        if(operadorComparativo(tokenActual)){
            siguienteToken();
            expresion();
        }else{
            error = true;
        }
    }

    private void expresion(){
        if(esToken(Tokens.IDENTIFICADOR) || esToken(Tokens.VALOR_NUMERO) || esToken(Tokens.VALOR_CADENA) || esToken(Tokens.VALOR_FLOAT)){
            siguienteToken();
            if(operadorAritmetico(tokenActual)){
                siguienteToken();
                expresion();
            }
        }else{
            error = true;
        }
    }

    private void read(){
        if(error) return;
        siguienteToken();
        if(esToken(Tokens.PARENTESIS_ABIERTO)){
            siguienteToken();
            if(esToken(Tokens.IDENTIFICADOR)){
                siguienteToken();
                if(esToken(Tokens.PARENTESIS_CERRADO)){
                    siguienteToken();
                }else{
                    error = true;
                }
            }else{
                error = true;
            }
        }else {
            error = true;
        }
    }

    private void write(){
        if(error) return;
        siguienteToken();
        if(esToken(Tokens.PARENTESIS_ABIERTO)){
            siguienteToken();
            if(esToken(Tokens.IDENTIFICADOR) || esToken(Tokens.VALOR_CADENA)){
                siguienteToken();
                if(esToken(Tokens.PARENTESIS_CERRADO)){
                    siguienteToken();
                }else{
                    error = true;
                }
            }else{
                error = true;
            }
        }else {
            error = true;
        }
    }

    private void siguienteToken() {
        if(!error){
            tokenActual = (++indice < tokenLista.size() ? tokenLista.get(indice): null);
        }
    }

    private boolean esToken(Tokens tokenEsperado) {
        return tokenActual != null && tokenActual.getTokens() == tokenEsperado;
    }

    private boolean operadorAritmetico(TablaToken token){
        return token != null && switch (token.getTokens()){
            case SUMA, RESTA, MULTIPLICACION, DIVISION -> true;
            default -> false;
        };
    }

    private boolean operadorComparativo(TablaToken token){
        return token != null && switch (token.getTokens()){
          case  IGUALDAD, MAYOR, MENOR -> true;
            default -> false;
        };
    }

}
