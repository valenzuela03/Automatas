package utils;

public class Variable {
    private String tipo;
    private String nombre;
    private String valor;

    public Variable(String tipo, String nombre, String valor) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    @Override
    public String toString() {
        return "Variable{" +
                "tipo='" + tipo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", valor='" + valor + '\'' +
                '}';
    }
}
