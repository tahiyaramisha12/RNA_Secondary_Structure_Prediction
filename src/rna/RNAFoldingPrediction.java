package rna;

import javax.swing.*;
import java.awt.*;
import java.awt.Font;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.awt.Desktop;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

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

    // Function to predict RNA folding using Nussinov's algorithm
    public static String nussinov(String sequence) {
        int n = sequence.length();
        int[][] dp = new int[n][n];

        // Initialize the DP table
        for (int i = 0; i < n; i++) {
            dp[i][i] = 0; // No base pairing for single nucleotides
            if (i < n - 1) {
                dp[i][i + 1] = 0; // No base pairing for sequences of length 1
            }
        }

        // Fill the DP table (bottom-up)
        for (int length = 2; length < n; length++) {
            for (int i = 0; i < n - length; i++) {
                int j = i + length;

                // Case 1: No pairing at j
                dp[i][j] = dp[i][j - 1];

                // Case 2: Try pairing i and j
                if (isBasePair(sequence.charAt(i), sequence.charAt(j))) {
                    dp[i][j] = Math.max(dp[i][j], dp[i + 1][j - 1] + 1);
                }

                // Case 3: Try splitting the sequence into two parts
                for (int k = i; k < j; k++) {
                    dp[i][j] = Math.max(dp[i][j], dp[i][k] + dp[k + 1][j]);
                }
            }
        }

        // Backtrack to form the folding structure
        char[] foldingStructure = new char[n];
        for (int i = 0; i < n; i++) {
            foldingStructure[i] = '.';
        }

        // Perform traceback to reconstruct the structure
        performTraceback(sequence, 0, n - 1, dp, foldingStructure);

        return new String(foldingStructure);
    }

    // Helper function to check if two bases form a valid pair
    private static boolean isBasePair(char base1, char base2) {
        return (base1 == 'A' && base2 == 'U') || (base1 == 'U' && base2 == 'A') ||
               (base1 == 'G' && base2 == 'C') || (base1 == 'C' && base2 == 'G');
    }

    // Backtracking function to reconstruct the base pairing
    private static void performTraceback(String sequence, int i, int j, int[][] dp, char[] foldingStructure) {
        if (i >= j) {
            return; // Base case
        }

        if (dp[i][j] == dp[i + 1][j]) {
            // No pairing at i
            performTraceback(sequence, i + 1, j, dp, foldingStructure);
        } else if (dp[i][j] == dp[i][j - 1]) {
            // No pairing at j
            performTraceback(sequence, i, j - 1, dp, foldingStructure);
        } else if (isBasePair(sequence.charAt(i), sequence.charAt(j)) && dp[i][j] == dp[i + 1][j - 1] + 1) {
            // Pairing between i and j
            foldingStructure[i] = '(';
            foldingStructure[j] = ')';
            performTraceback(sequence, i + 1, j - 1, dp, foldingStructure);
        } else {
            // Split the sequence into two parts
            for (int k = i + 1; k < j; k++) {
                if (dp[i][j] == dp[i][k] + dp[k + 1][j]) {
                    performTraceback(sequence, i, k, dp, foldingStructure);
                    performTraceback(sequence, k + 1, j, dp, foldingStructure);
                    return;
                }
            }
        }
    }

    // Function to analyze the predicted structure and provide suggestions
    private static String analyzeStructure(String foldingStructure) {
        int pairedRegions = 0;
        int unpairedRegions = 0;
        int maxUnpairedLength = 0;
        int currentUnpairedLength = 0;

        // Count paired and unpaired regions
        for (char c : foldingStructure.toCharArray()) {
            if (c == '(' || c == ')') {
                pairedRegions++;
                if (currentUnpairedLength > maxUnpairedLength) {
                    maxUnpairedLength = currentUnpairedLength;
                }
                currentUnpairedLength = 0;
            } else if (c == '.') {
                unpairedRegions++;
                currentUnpairedLength++;
            }
        }

        // Check for long unpaired regions
        if (currentUnpairedLength > maxUnpairedLength) {
            maxUnpairedLength = currentUnpairedLength;
        }

        // Generate analysis comments
        StringBuilder analysis = new StringBuilder();
        if (pairedRegions > unpairedRegions) {
            analysis.append("1. This RNA has a stable structure with strong pairing. It is likely functional for binding or catalysis.\n");
        } else {
            analysis.append("2. The structure contains long unpaired regions, which may affect stability. Further validation is needed.\n");
        }

        // Add additional details
        analysis.append("\nAnalysis Details:\n");
        analysis.append("- Total paired bases: ").append(pairedRegions).append("\n");
        analysis.append("- Total unpaired bases: ").append(unpairedRegions).append("\n");
        analysis.append("- Longest unpaired region: ").append(maxUnpairedLength).append(" bases\n");

        return analysis.toString();
    }

    // Function to export results to Excel with color coding
    private static void exportToExcel(String sequence, String foldingStructure, String analysis) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("RNA Folding Results");

            // Create styles for paired and unpaired bases
            XSSFCellStyle pairedStyle = workbook.createCellStyle();
            pairedStyle.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 144, (byte) 238, (byte) 144 }, null)); // Light green
            pairedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle unpairedStyle = workbook.createCellStyle();
            unpairedStyle.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 255, (byte) 182, (byte) 193 }, null)); // Light red
            unpairedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create a header row
            XSSFRow headerRow = sheet.createRow(0);
            XSSFCell headerCell = headerRow.createCell(0);
            headerCell.setCellValue("RNA Sequence and Folding Structure");

            // Create a row for the sequence
            XSSFRow sequenceRow = sheet.createRow(1);
            for (int i = 0; i < sequence.length(); i++) {
                XSSFCell cell = sequenceRow.createCell(i);
                cell.setCellValue(String.valueOf(sequence.charAt(i)));
                if (foldingStructure.charAt(i) == '(' || foldingStructure.charAt(i) == ')') {
                    cell.setCellStyle(pairedStyle);
                } else {
                    cell.setCellStyle(unpairedStyle);
                }
            }

            // Create a row for the folding structure
            XSSFRow foldingRow = sheet.createRow(2);
            for (int i = 0; i < foldingStructure.length(); i++) {
                XSSFCell cell = foldingRow.createCell(i);
                cell.setCellValue(String.valueOf(foldingStructure.charAt(i)));
                if (foldingStructure.charAt(i) == '(' || foldingStructure.charAt(i) == ')') {
                    cell.setCellStyle(pairedStyle);
                } else {
                    cell.setCellStyle(unpairedStyle);
                }
            }

            // Create a row for the analysis
            XSSFRow analysisRow = sheet.createRow(4);
            XSSFCell analysisCell = analysisRow.createCell(0);
            analysisCell.setCellValue(analysis);

            // Auto-size columns
            for (int i = 0; i < sequence.length(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Open file chooser dialog to select export location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Export Location");
            fileChooser.setSelectedFile(new File("RNA_Folding_Results.xlsx"));
            int userChoice = fileChooser.showSaveDialog(null);
            if (userChoice == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();

                // Write the output to the selected file
                try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                    workbook.write(fileOut);
                }

                JOptionPane.showMessageDialog(null, "Results exported to " + outputFile.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error exporting to Excel: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Load RNA sequences from file
        String filePath = "rna_sequences.txt";
        Map<String, String> rnaSequences = loadRNASequences(filePath);

        // GUI
        JFrame frame = new JFrame("RNA Secondary Structure Prediction");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // Increased size to accommodate analysis
        frame.setLayout(new BorderLayout());

        // Dropdown box at the top
        JComboBox<String> rnaComboBox = new JComboBox<>(rnaSequences.keySet().toArray(new String[0]));
        frame.add(rnaComboBox, BorderLayout.NORTH);

        // Text area to display the output (Initially Empty)
        JTextArea resultTextArea = new JTextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        frame.add(new JScrollPane(resultTextArea), BorderLayout.CENTER);

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton predictButton = new JButton("Predict");
        JButton exportButton = new JButton("Export to Excel");
        exportButton.setEnabled(false); // Disabled initially
        buttonPanel.add(predictButton);
        buttonPanel.add(exportButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Variables to store prediction results
        String[] currentPrediction = new String[3]; // sequence, foldingStructure, analysis

        // Action Listener for Predict Button
        predictButton.addActionListener(e -> {
            String selectedRNA = (String) rnaComboBox.getSelectedItem();
            if (selectedRNA != null) {
                String rnaSequence = rnaSequences.get(selectedRNA);
                int sequenceLength = rnaSequence.length();
                String foldingStructure = nussinov(rnaSequence);
                String analysis = analyzeStructure(foldingStructure);

                // Store results for export
                currentPrediction[0] = rnaSequence;
                currentPrediction[1] = foldingStructure;
                currentPrediction[2] = analysis;

                // Update UI
                resultTextArea.setText(
                        "Selected RNA: " + selectedRNA + "\n\n" +
                        "RNA Sequence:\n" + rnaSequence + "\n\n" +
                        "Predicted Folding Structure:\n" + foldingStructure + "\n\n" +
                        "Length: " + sequenceLength + " nucleotides\n\n" +
                        "Analysis:\n" + analysis);

                exportButton.setEnabled(true); // Enable the Export button
            }
        });

        // Action Listener for Export Button
        exportButton.addActionListener(e -> {
            String sequence = currentPrediction[0];
            String foldingStructure = currentPrediction[1];
            String analysis = currentPrediction[2];
            exportToExcel(sequence, foldingStructure, analysis);
        });

        // Show the frame
        frame.setVisible(true);
    }
}
