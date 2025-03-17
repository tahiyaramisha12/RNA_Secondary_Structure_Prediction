package rna;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RNAFoldingPrediction {

    // Load RNA sequences from the file
    private static Map<String, String> loadRNASequences(String filePath) {
        Map<String, String> rnaSequences = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    rnaSequences.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading RNA sequences file: " + e.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
        return rnaSequences;
    }

    public static void main(String[] args) {
        // Load RNA sequences from file
        String filePath = "rna_sequences.txt";
        Map<String, String> rnaSequences = loadRNASequences(filePath);

        // GUI
        JFrame frame = new JFrame("RNA Secondary Structure Prediction");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200); 
        frame.setLayout(new BorderLayout());

        // Dropdown box at the top
        JComboBox<String> rnaComboBox = new JComboBox<>(rnaSequences.keySet().toArray(new String[0]));
        frame.add(rnaComboBox, BorderLayout.NORTH);

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton predictButton = new JButton("Predict");
        JButton exportButton = new JButton("Export to Excel");
        exportButton.setEnabled(false); 
        buttonPanel.add(predictButton);
        buttonPanel.add(exportButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Action Listener for Predict Button
        predictButton.addActionListener(e -> {
            String selectedRNA = (String) rnaComboBox.getSelectedItem();
            if (selectedRNA != null) {
                String rnaSequence = rnaSequences.get(selectedRNA);
                exportButton.setEnabled(true); 
            }
        });

        // Action Listener for Export Button
        exportButton.addActionListener(e -> {
        });

        // Show the frame
        frame.setVisible(true);
    }
}
