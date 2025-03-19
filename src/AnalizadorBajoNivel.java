import utils.Variable;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorBajoNivel  {
    private List<TablaToken> tokenLista;
    private List<Variable> variables;
    private StringBuilder codigo;
    public AnalizadorBajoNivel(){
        tokenLista = new ArrayList<>();
        variables = new ArrayList<>();
    }

    public void prepararAnalizadorBajoNivel(List<TablaToken> tokenLista, List<Variable> variables)  {
        this.variables = variables;
        this.tokenLista = tokenLista;
        codigo = new StringBuilder();
    }

    public void analizar(){
        puntoData();
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
    }
    public String getCode(){
        return codigo.toString();
    }
}
