package net.sf.rails.game.ai.playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Analyzes all '_features.tsv' files in a directory to find the
 * Human Strategic Trade-off (Tau).
 *
 * It filters for "Investment Lays" (where dR <= 0 and dSigmaC > 0)
 * and calculates the average dR (Tau) and average dSigmaC for those moves.
 *
 * Usage: java TauFinder <path_to_game_logs_directory>
 */
public class TauFinder {

    private static final Logger log = LoggerFactory.getLogger(TauFinder.class);

    // A simple data class to hold the parsed feature row
    private static class FeatureRow {
        int deltaR;
        int deltaSigmaC;

        FeatureRow(int deltaR, int deltaSigmaC) {
            this.deltaR = deltaR;
            this.deltaSigmaC = deltaSigmaC;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            log.error("Usage: java TauFinder <path_to_game_logs_directory>");
            return;
        }

        File logsFolder = new File(args[0]);
        if (!logsFolder.isDirectory()) {
            log.error("Error: Path is not a valid directory: {}", logsFolder.getAbsolutePath());
            return;
        }

        // 1. Find all feature files
        File[] featureFiles = logsFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("_features.tsv");
            }
        });

        if (featureFiles == null || featureFiles.length == 0) {
            log.warn("No '_features.tsv' files found in directory: {}", logsFolder.getAbsolutePath());
            return;
        }

        log.info("Found {} feature files. Aggregating data...", featureFiles.length);

        List<FeatureRow> allInvestmentLays = new ArrayList<>();
        int totalTileLays = 0;

        // 2. Read and parse every file
        for (File tsvFile : featureFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(tsvFile))) {
                String line;
                boolean isHeader = true;

                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false; // Skip the header row
                        continue;
                    }

                    totalTileLays++;
                    String[] parts = line.split("\t"); // Split by tab
                    
                    // TSV format: ActionID, Company, Hex, Tile, Phase, Delta_R, Delta_SigmaC
                    int deltaR = Integer.parseInt(parts[5]);
                    int deltaSigmaC = Integer.parseInt(parts[6]);

                    // This is our critical filter
                    if (deltaR <= 0 && deltaSigmaC > 0) {
                        allInvestmentLays.add(new FeatureRow(deltaR, deltaSigmaC));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to read or parse file: {}. Skipping.", tsvFile.getName(), e);
            }
        }

        // 3. Calculate and print the final results
        log.info("---------------------------------");
        log.info("---    TAU ANALYSIS COMPLETE    ---");
        log.info("---------------------------------");
        log.info("Total games analyzed:     {}", featureFiles.length);
        log.info("Total tile lays found:    {}", totalTileLays);
        log.info("Total 'Investment Lays':  {}", allInvestmentLays.size());
        log.info("---------------------------------");

        if (allInvestmentLays.isEmpty()) {
            log.warn("No 'Investment Lays' (where dR <= 0 and dSigmaC > 0) were found.");
            log.warn("Cannot calculate Tau. This may indicate a data error or very conservative human play.");
            return;
        }

        // Calculate averages
        double sumDeltaR = 0;
        double sumDeltaSigmaC = 0;
        for (FeatureRow row : allInvestmentLays) {
            sumDeltaR += row.deltaR;
            sumDeltaSigmaC += row.deltaSigmaC;
        }

        double averageTau = sumDeltaR / allInvestmentLays.size();
        double averageGain = sumDeltaSigmaC / allInvestmentLays.size();

        log.info(String.format(Locale.ROOT, "Average Strategic Cost (Tau):     %.2fM", averageTau));
        log.info(String.format(Locale.ROOT, "Average Strategic Gain (Sigma C): %.2fM", averageGain));
        log.info("---------------------------------");
        log.info("Rule: Humans are willing to sacrifice an average of {}M in immediate revenue", (int)Math.abs(averageTau));
        log.info("      to gain an average of {}M in future potential.", (int)averageGain);
    }
}