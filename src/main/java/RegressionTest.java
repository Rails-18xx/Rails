import net.sf.rails.util.GameLoader;
import net.sf.rails.ui.swing.GameSetupController; 

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RegressionTest {

    public static void main(String[] args) {
        // --- START FIX ---
        // 1. FORCE CONSISTENT ENVIRONMENT
        // Fixes desynchronization in tie-breakers (e.g., Prussia vs Saxony)
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        if (args.length < 1) {
            System.err.println("Usage: java RegressionTest <path_to_rails_file>");
            System.exit(1);
        }

        String railsFilePath = args[0];
        File railsFile = new File(railsFilePath);
        String filename = railsFile.getName();
        
        // Find the associated .txt file (e.g., PrussiaV7.rails -> PrussiaV7.txt)
        String txtFilePath = railsFilePath.replaceAll("\\.rails$", ".txt");
        File txtFile = new File(txtFilePath);

        // 2. PRE-LOAD TEXT FILE DUMP
        // If a .txt file exists with the same name, print it now
        if (txtFile.exists()) {
            System.out.println("LOG FILE CONTENT: " + txtFile.getName());
            System.out.println("---------------------------------------------------");
            try {
                List<String> fileLines = Files.readAllLines(Paths.get(txtFilePath));
                for (String line : fileLines) {
                    System.out.println(line);
                }
            } catch (Exception e) {
                System.out.println("[Error reading .txt file: " + e.getMessage() + "]");
            }
            System.out.println("---------------------------------------------------");
        } else {
            System.out.println("[No matching .txt file found for " + filename + "]");
        }
        
        System.out.println("STARTING GAME ENGINE REPLAY...");

        // 3. SETUP CAPTURE
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(outputBuffer);

        // Redirect all subsequent engine output to buffer
        System.setOut(captureStream);
        System.setErr(captureStream);

        boolean loaderSuccess = false;

        try {
            if (!railsFile.exists()) throw new IllegalArgumentException("Error: File not found: " + railsFilePath);

            System.setProperty("java.awt.headless", "true"); 
            try {
                GameSetupController.getInstance().prepareGameUIInit();
            } catch (Throwable t) { /* Ignore UI init */ }

            // Re-assert capture (GameSetupController reset check)
            System.setOut(captureStream);
            System.setErr(captureStream);

            GameLoader loader = new GameLoader();
            
            // Runs: load -> convert -> start -> replay
            loaderSuccess = loader.createFromFile(railsFile);

        } catch (Throwable e) {
            System.err.println("CRITICAL EXECUTION FAILURE: " + e.getMessage());
            e.printStackTrace();
            loaderSuccess = false;
        } finally {
            // Restore streams for reporting
            System.setOut(originalOut);
            System.setErr(originalErr);

            String fullLog = outputBuffer.toString();
            String[] lines = fullLog.split("\\r?\\n");
            
            boolean errorFound = false;
            int firstErrorIndex = -1;

            // 4. SCAN FOR FIRST ERROR
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains("SLF4J:")) continue;

                if (line.contains("FATAL") || 
                    line.contains("VALIDATION FAILURE") || 
                    line.contains("Exception") || 
                    line.contains("CRITICAL")) {
                    
                    errorFound = true;
                    firstErrorIndex = i;
                    break; 
                }
            }

            // 5. CONDITIONAL LOG DUMP (Only if failed)
            if (errorFound || !loaderSuccess) {
                System.out.println("\n!!! REGRESSION FAILURE DETECTED !!!");
                System.out.println("File: " + filename);
                System.out.println("---------------------------------------------------");
                
                // Show logs leading up to the error
                int contextStart = Math.max(0, firstErrorIndex - 15);
                for (int i = contextStart; i <= firstErrorIndex && i < lines.length; i++) {
                    String line = lines[i];
                    if (!line.contains("SLF4J:") && !line.trim().isEmpty()) {
                        if (i == firstErrorIndex) {
                            System.out.println(">> " + line);
                        } else {
                            System.out.println("   " + line);
                        }
                    }
                }

                // Show 20 lines of post-error details
                for (int j = 1; j <= 20 && (firstErrorIndex + j) < lines.length; j++) {
                    System.out.println("   " + lines[firstErrorIndex + j]);
                }
                System.out.println("---------------------------------------------------");
                System.exit(255);
            } else {
                // Success: Silent (except for the .txt dump above)
                System.exit(0);
            }
        }
        // --- END FIX ---
    }
}