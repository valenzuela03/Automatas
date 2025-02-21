import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

public class Interfaz extends JFrame {
    private JTextArea inputArea;
    private JTextArea outputTokensArea;
    private JTextArea outputSemanticoArea;
    private JButton analyzeButton;
    private JButton sintaticButton;
    private AnalizadorLexico analizador;
    private AnalizadorSintactico analizadorSintactico;
    private List<TablaToken> tablaToken;

    public Interfaz() {
        setTitle("CeVaBe");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

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
        sintaticButton.addActionListener(_ -> analizarSemantico());

        topPanel.add(fileButton);
        topPanel.add(analyzeButton);
        topPanel.add(sintaticButton);

        add(topPanel, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(3, 1));

        JPanel programaPanel = new JPanel(new BorderLayout());
        programaPanel.add(new JLabel("Programa"), BorderLayout.NORTH);
        inputArea = new JTextArea(10, 40);
        programaPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        JPanel tokensPanel = new JPanel(new BorderLayout());
        tokensPanel.add(new JLabel("Analisis Lexico"), BorderLayout.NORTH);
        outputTokensArea = new JTextArea(10, 40);
        outputTokensArea.setEditable(false);
        tokensPanel.add(new JScrollPane(outputTokensArea), BorderLayout.CENTER);

        JPanel semanticoPanel = new JPanel(new BorderLayout());
        semanticoPanel.add(new JLabel("Analisis Sintatico"), BorderLayout.NORTH);
        outputSemanticoArea = new JTextArea(3, 40);
        outputSemanticoArea.setEditable(false);
        semanticoPanel.add(new JScrollPane(outputSemanticoArea), BorderLayout.CENTER);

        panel.add(programaPanel);
        panel.add(tokensPanel);
        panel.add(semanticoPanel);

        add(panel, BorderLayout.CENTER);
    }

    private void analizarCodigo() {
        String codigo = inputArea.getText();
        analizador.limpiarTable();
        analizador.analyze(codigo);
        List<Tokens> tokens = analizador.getToken();
        tablaToken = analizador.getTablaToken();

        StringBuilder resultado = new StringBuilder();
        for (TablaToken token : tablaToken) {
            resultado.append(token.getTokens()).append(" -> ").append(token.getNombre()).append("\n");
        }

        outputTokensArea.setText(resultado.toString());
        outputSemanticoArea.setText(""); // Limpiar el área semántica hasta que se presione el botón
        sintaticButton.setEnabled(true);
    }

    private void analizarSemantico() {
        analizadorSintactico = new AnalizadorSintactico();
        analizadorSintactico.prepararAnalizadorSintactico(tablaToken);
        boolean resultado = analizadorSintactico.analizar();
        String mensajeSintaxis = resultado ? "Codigo correcto" : "Codigo incorrecto";
        outputSemanticoArea.setText(mensajeSintaxis);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                inputArea.setText("");
                String line;
                while ((line = reader.readLine()) != null) {
                    inputArea.append(line + "\n");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(inputArea.getText());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}