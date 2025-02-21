import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

class AnalizadorLexico {
    private final HashMap<String, Tokens> map;
    private ArrayList<Tokens> tokenLista;
    private List<TablaToken> tablaToken = new ArrayList<>();

    public AnalizadorLexico() {
        map = new HashMap<>();
        map.put("Principio", Tokens.PR);
        map.put("Final", Tokens.PR);

        map.put("int", Tokens.PR);
        map.put("float", Tokens.PR);
        map.put("string", Tokens.PR);

        map.put("read", Tokens.PR);
        map.put("write", Tokens.PR);

        map.put("if", Tokens.PR);
        map.put("else", Tokens.PR);

        map.put("=", Tokens.IGUAL);
        map.put("+", Tokens.SUMA);
        map.put("-", Tokens.RESTA);
        map.put("*", Tokens.MULTIPLICACION);
        map.put("/", Tokens.DIVISION);

        map.put("==", Tokens.IGUALDAD);
        map.put(">", Tokens.MAYOR);
        map.put("<", Tokens.MENOR);

        map.put("(", Tokens.PARENTESIS_ABIERTO);
        map.put(")", Tokens.PARENTESIS_CERRADO);
        map.put("{", Tokens.LLAVE_ABIERTA);
        map.put("}", Tokens.LLAVE_CERRADA);
        map.put(";", Tokens.PUNTO_Y_COMA);
    }

    public void analyze(String line) {
        tokenLista = new ArrayList<>();
        int fila = 1, columna = 1, i = 0;
        int len = line.length();

        while (i < len) {
            char c = line.charAt(i);

            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    fila++;
                    columna = 1;
                } else {
                    columna++;
                }
                i++;
                continue;
            }

            String singleChar = String.valueOf(c);
            if (map.containsKey(singleChar)) {
                tokenLista.add(map.get(singleChar));
                tablaToken.add(new TablaToken(singleChar, map.get(singleChar)));
                i++;
                columna++;
                continue;
            }

            if (i + 1 < len) {
                String doubleChar = line.substring(i, i + 2);
                if (map.containsKey(doubleChar)) {
                    tokenLista.add(map.get(doubleChar));
                    tablaToken.add(new TablaToken(doubleChar, map.get(doubleChar)));
                    i += 2;
                    columna += 2;
                    continue;
                }
            }

            if (Character.isDigit(c)) {
                boolean hasDecimal = false;
                int start = i;

                while (i < len && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '.')) {
                    if (line.charAt(i) == '.') {
                        if (hasDecimal) break;
                        hasDecimal = true;
                    }
                    i++;
                    columna++;
                }

                tokenLista.add(hasDecimal ? Tokens.VALOR_FLOAT : Tokens.VALOR_NUMERO);
                tablaToken.add(new TablaToken(line.substring(start, i), hasDecimal ? Tokens.VALOR_FLOAT : Tokens.VALOR_NUMERO));
                continue;
            }

            if (Character.isLetter(c)) {
                StringBuilder id = new StringBuilder();
                while (i < len && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                    id.append(line.charAt(i));
                    i++;
                    columna++;
                }

                tokenLista.add(esReservado(id.toString()) ? map.get(id.toString()) : Tokens.IDENTIFICADOR);
                tablaToken.add(new TablaToken(id.toString(), esReservado(id.toString()) ? map.get(id.toString()) : Tokens.IDENTIFICADOR));
                continue;
            }

            if (c == '"' || c == '“') {
                i++;
                columna++;

                while (i < len && line.charAt(i) != '"' && line.charAt(i) != '”') {
                    i++;
                    columna++;
                }

                if (i < len) {
                    i++;
                    columna++;
                }

                tokenLista.add(Tokens.VALOR_CADENA);
                tablaToken.add(new TablaToken(line.substring(i, i), Tokens.VALOR_CADENA));
                continue;
            }
            tablaToken.add(new TablaToken(String.valueOf(c), Tokens.ERROR));
            i++;
            columna++;
        }
    }


    public List<Tokens> getToken() {
        return tokenLista;
    }
    public List<TablaToken> getTablaToken() {
        return tablaToken;
    }

    private boolean esReservado(String palabra){
        return map.containsKey(palabra);
    }

    public void limpiarTable(){
        tablaToken.clear();
    }
}
