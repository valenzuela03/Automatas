import utils.Tokens;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;

public class Interfaz extends JFrame {
    private JTextArea inputArea;
    private JTextArea outputTokensArea;
    private JTextArea outputSintacticoArea;
    private JTextArea outputSemanticoArea;
    private JTextArea outputBajoNivelArea;
    private JButton analyzeButton;
    private JButton sintaticButton;
    private JButton semanticoButton;
    private JButton bajoNivelButton;
    private AnalizadorLexico analizador;
    private AnalizadorSintactico analizadorSintactico;
    private AnalizadorSemantico analizadorSemantico;
    private AnalizadorBajoNivel analizadorBajoNivel;
    private List<TablaToken> tablaToken;

    public Interfaz() {
        setTitle("CeVaBe");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        analizador = new AnalizadorLexico();

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton fileButton = new JButton("Archivo");
        JPopupMenu fileMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Abrir");
        JMenuItem saveItem = new JMenuItem("Guardar");
        fileMenu.add(openItem);
        fileMenu.add(saveItem);

        fileButton.addActionListener(e -> fileMenu.show(fileButton, 0, fileButton.getHeight()));
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile());

        analyzeButton = new JButton("Analizar");
        analyzeButton.addActionListener(_ -> analizarCodigo());

        sintaticButton = new JButton("Sintatico");
        sintaticButton.setEnabled(false);
        sintaticButton.addActionListener(_ -> analizarSintactico());

        semanticoButton = new JButton("Semantico");
        semanticoButton.setEnabled(false);
        semanticoButton.addActionListener(_ -> analizarSemantico());

        bajoNivelButton = new JButton("Bajo Nivel");
        bajoNivelButton.setEnabled(false);
        bajoNivelButton.addActionListener(_ -> analizarBajoNivel());

        topPanel.add(fileButton);
        topPanel.add(analyzeButton);
        topPanel.add(sintaticButton);
        topPanel.add(semanticoButton);
        topPanel.add(bajoNivelButton);

        add(topPanel, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(4, 1));

        inputArea = new JTextArea(10, 40);
        outputTokensArea = new JTextArea(10, 40);
        outputSintacticoArea = new JTextArea(3, 40);
        outputSemanticoArea = new JTextArea(3, 40);
        outputBajoNivelArea = new JTextArea(3, 40);

        outputTokensArea.setEditable(false);
        outputSintacticoArea.setEditable(false);
        outputSemanticoArea.setEditable(false);
        outputBajoNivelArea.setEditable(false);

        panel.add(createPanel("Programa", inputArea));
        panel.add(createPanel("Analisis Lexico", outputTokensArea));
        panel.add(createPanel("Analisis Sintactico", outputSintacticoArea));
        panel.add(createPanel("Analisis Semantico", outputSemanticoArea));
        panel.add(createPanel("Analisis Bajo Nivel", outputBajoNivelArea));

        add(panel, BorderLayout.CENTER);
    }

    private JPanel createPanel(String title, JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private void analizarCodigo() {
        String codigo = inputArea.getText();
        analizador.limpiarTable();
        analizador.analyze(codigo);
        tablaToken = analizador.getTablaToken();

        StringBuilder resultado = new StringBuilder();
        for (TablaToken token : tablaToken) {
            resultado.append(token.getTokens()).append(" -> ").append(token.getNombre()).append("\n");
        }

        outputTokensArea.setText(resultado.toString());
        outputSintacticoArea.setText("");
        outputSemanticoArea.setText("");
        sintaticButton.setEnabled(true);
    }

    private void analizarSintactico() {
        analizadorSintactico = new AnalizadorSintactico();
        analizadorSintactico.prepararAnalizadorSintactico(tablaToken);
        boolean resultado = analizadorSintactico.analizar();
        outputSintacticoArea.setText(resultado ? "Codigo correcto" : "Codigo incorrecto");
        semanticoButton.setEnabled(true);
    }

    private void analizarSemantico() {
        analizadorSemantico = new AnalizadorSemantico();
        analizadorSemantico.prepararAnalizadorSemantico(tablaToken);
        boolean resultado = analizadorSemantico.analizar();
        outputSemanticoArea.setText(resultado ? "Codigo correcto" : "Codigo incorrecto");
        bajoNivelButton.setEnabled(true);
    }

    private void analizarBajoNivel() {
        analizadorBajoNivel = new AnalizadorBajoNivel();
        analizadorBajoNivel.prepararAnalizadorBajoNivel(tablaToken, analizadorSemantico.getVariables());
        analizadorBajoNivel.analizar();
        outputBajoNivelArea.setText(analizadorBajoNivel.getCode());
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                inputArea.read(reader, null);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                inputArea.write(writer);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
