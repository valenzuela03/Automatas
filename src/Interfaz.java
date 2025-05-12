import utils.Tokens;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;

public class Interfaz extends JFrame {
    private JTextArea inputArea, outputTokensArea, outputSintacticoArea, outputSemanticoArea, outputBajoNivelArea, outputObjetoArea;
    private JButton analyzeButton, sintaticButton, semanticoButton, bajoNivelButton, objetoButton;
    private AnalizadorLexico analizador;
    private AnalizadorSintactico analizadorSintactico;
    private AnalizadorSemantico analizadorSemantico;
    private AnalizadorBajoNivel analizadorBajoNivel;
    private List<TablaToken> tablaToken;

    public Interfaz() {
        setTitle("CeVaBe");
        setSize(1000, 700);
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

        sintaticButton = new JButton("Sintáctico");
        sintaticButton.setEnabled(false);
        sintaticButton.addActionListener(_ -> analizarSintactico());

        semanticoButton = new JButton("Semántico");
        semanticoButton.setEnabled(false);
        semanticoButton.addActionListener(_ -> analizarSemantico());

        bajoNivelButton = new JButton("Bajo Nivel");
        bajoNivelButton.setEnabled(false);
        bajoNivelButton.addActionListener(_ -> analizarBajoNivel());

        objetoButton = new JButton("Código Objeto");
        objetoButton.setEnabled(false);
        objetoButton.addActionListener(_ -> mostrarCodigoObjeto());

        topPanel.add(fileButton);
        topPanel.add(analyzeButton);
        topPanel.add(sintaticButton);
        topPanel.add(semanticoButton);
        topPanel.add(bajoNivelButton);
        topPanel.add(objetoButton);

        add(topPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        inputArea = new JTextArea();
        outputTokensArea = new JTextArea();
        outputSintacticoArea = new JTextArea();
        outputSemanticoArea = new JTextArea();
        outputBajoNivelArea = new JTextArea();
        outputObjetoArea = new JTextArea();

        outputTokensArea.setEditable(false);
        outputSintacticoArea.setEditable(false);
        outputSemanticoArea.setEditable(false);
        outputBajoNivelArea.setEditable(false);
        outputObjetoArea.setEditable(false);

        // Paneles de contenido
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.33; gbc.weighty = 0.5;
        mainPanel.add(createPanel("Programa", inputArea), gbc);

        gbc.gridx = 1;
        mainPanel.add(createPanel("Análisis Léxico", outputTokensArea), gbc);

        gbc.gridx = 2;
        mainPanel.add(createPanel("Análisis Sintáctico", outputSintacticoArea), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(createPanel("Análisis Semántico", outputSemanticoArea), gbc);

        gbc.gridx = 1;
        mainPanel.add(createPanel("Análisis Bajo Nivel", outputBajoNivelArea), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.66; // Más ancho para el panel de "Código Objeto"
        mainPanel.add(createPanel("Código Objeto", outputObjetoArea), gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createPanel(String title, JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
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
        outputBajoNivelArea.setText("");
        outputObjetoArea.setText("");
        sintaticButton.setEnabled(true);
        semanticoButton.setEnabled(false);
        bajoNivelButton.setEnabled(false);
        objetoButton.setEnabled(false);
    }

    private void analizarSintactico() {
        analizadorSintactico = new AnalizadorSintactico();
        analizadorSintactico.prepararAnalizadorSintactico(tablaToken);
        boolean resultado = analizadorSintactico.analizar();
        outputSintacticoArea.setText(resultado ? "Código correcto" : "Código incorrecto");
        semanticoButton.setEnabled(true);
        bajoNivelButton.setEnabled(false);
        objetoButton.setEnabled(false);
    }

    private void analizarSemantico() {
        analizadorSemantico = new AnalizadorSemantico();
        analizadorSemantico.prepararAnalizadorSemantico(tablaToken);
        boolean resultado = analizadorSemantico.analizar();
        outputSemanticoArea.setText(resultado ? "Código correcto" : "Código incorrecto");
        bajoNivelButton.setEnabled(true);
        objetoButton.setEnabled(false);
    }

    private void analizarBajoNivel() {
        analizadorBajoNivel = new AnalizadorBajoNivel();
        analizadorBajoNivel.prepararAnalizadorBajoNivel(tablaToken, analizadorSemantico.getVariables());
        analizadorBajoNivel.analizar();
        outputBajoNivelArea.setText(analizadorBajoNivel.getCode());
        objetoButton.setEnabled(true);
    }

    private void mostrarCodigoObjeto() {
        if (analizadorBajoNivel != null) {
            outputObjetoArea.setText(analizadorBajoNivel.getCodigoObjeto());
        } else {
            outputObjetoArea.setText("Primero realiza el análisis de bajo nivel.");
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Interfaz().setVisible(true));
    }
}
