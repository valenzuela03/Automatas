import utils.Tokens;

public class TablaToken {
    private Tokens tokens;
    private String valor;

    public TablaToken(String valor, Tokens tokens) {
        this.valor = valor;
        this.tokens = tokens;
    }
    public String getNombre() {
        return valor;
    }
    public Tokens getTokens() {
        return tokens;
    }


}
