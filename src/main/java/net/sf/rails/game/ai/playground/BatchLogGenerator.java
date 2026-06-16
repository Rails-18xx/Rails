package net.sf.rails.game.ai.playground;

import net.sf.rails.util.GameLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException; // Keep this import
import java.io.File;
import java.io.FilenameFilter;

/**
 * Main orchestration tool for batch analysis.
 *
 * This tool scans an input directory for .rails files and orchestrates
 * the full analysis pipeline for each one:
 * 1. Generates a subdirectory of state snapshots (e.g., "game_name_history").
 * 2. Calls FeatureExtractor to create a final features database (e.g., "game_name_features.tsv").
 *
 * Usage: java net.sf.rails.game.ai.playground.BatchLogGenerator <input_folder> <output_folder>
 */
public class BatchLogGenerator {

    private static final Logger log = LoggerFactory.getLogger(BatchLogGenerator.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            log.error("Usage: java net.sf.rails.game.ai.playground.BatchLogGenerator <input_folder_path> <output_folder_path>");
            return;
        }

        File inputFolder = new File(args[0]);
        File outputFolder = new File(args[1]); // This is "game_logs"

        // Check 1: Input folder must exist
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            log.error("--- ERROR: Input folder not found ---");
            log.error("Path: {}", inputFolder.getAbsolutePath());
            log.error("Please ensure this directory exists and contains your .rails files.");
            return;
        }

        // Check 2: Output folder must exist (or be creatable)
        if (!outputFolder.exists()) {
            log.info("Output folder not found. Creating directory...");
            if (!outputFolder.mkdirs()) {
                log.error("--- ERROR: Could not create output folder ---");
                log.error("Path: {}", outputFolder.getAbsolutePath());
                log.error("Please check permissions.");
                return;
            }
        }

        log.info("Starting batch process...");
        log.info("Input folder: {}", inputFolder.getAbsolutePath());
        log.info("Output folder: {}", outputFolder.getAbsolutePath());

        File[] railFiles = inputFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".rails");
            }
        });

        if (railFiles == null || railFiles.length == 0) {
            log.warn("No .rails files found in input folder.");
            return;
        }

        log.info("Found {} .rails files to process. Beginning sequential generation and merge.", railFiles.length);

        // Process each game file
        for (File inputFile : railFiles) {
            log.info("--- Processing Game: {} ---", inputFile.getName());
            String baseName = inputFile.getName().replaceFirst("(?i)\\.rails$", "");
            
            // Path 1: Snapshot directory (e.g., game_logs/1835_..._history)
            File snapshotDir = new File(outputFolder, baseName + "_history");
            
            // Path 2: Final features file (e.g., game_logs/1835_..._features.tsv)
            File featureFile = new File(outputFolder, baseName + "_features.tsv");

            // --- STAGE 1: SNAPSHOT GENERATION ---
            if (!snapshotDir.exists()) {
                log.info("Generating snapshots for {}...", inputFile.getName());
                try {
                    GameLoader.createAndLogFromFile(inputFile, snapshotDir);
                } catch (Exception e) {
                    log.error("CRITICAL: Snapshot generation failed for {}.", inputFile.getName(), e);
                    continue; // Skip this game
                }
            } else {
                log.info("Snapshots for {} already exist. Skipping generation.", inputFile.getName());
            }

            // --- STAGE 2: FEATURE EXTRACTION ---
            if (!featureFile.exists()) {
                log.info("Extracting features for {}...", inputFile.getName());
                try {
                    // Build the args for the other main method
                    String[] extractorArgs = new String[] {
                        inputFile.getAbsolutePath(),
                        snapshotDir.getAbsolutePath(),
                        featureFile.getAbsolutePath()
                    };
                    // Call the FeatureExtractor's main method
                    net.sf.rails.game.ai.playground.FeatureExtractor.main(extractorArgs);
                } catch (Exception e) {
                    log.error("CRITICAL: Feature extraction failed for {}.", inputFile.getName(), e);
                }
            } else {
                log.warn("Feature file {} already exists. Skipping analysis.", featureFile.getName());
            }

            // --- STAGE 3: CLEANUP (DISABLED) ---
            // We are leaving the snapshot directories for now.
            /*
            try {
                log.info("Cleaning up temporary snapshot directory: {}", snapshotDir.getName());
                if (snapshotDir.exists()) {
                     File[] contents = snapshotDir.listFiles();
                     if (contents != null) {
                         for (File f : contents) { f.delete(); }
                     }
                     snapshotDir.delete();
                }
            } catch (Exception e) {
                 log.error("Cleanup failed unexpectedly for {}.", snapshotDir.getName(), e);
            }
            */
        }

        log.info("Batch process complete.");
    }
}