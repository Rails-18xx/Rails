package net.sf.rails.game.ai.playground;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.ai.snapshot.GameStateData;
import net.sf.rails.game.ai.snapshot.GameStateRestorer;
import net.sf.rails.util.GameLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.BuyStartItem;
import rails.game.action.PossibleAction;
import rails.game.action.NullAction;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.*;

public class InitialRoundAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(InitialRoundAnalyzer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class GameAnalysis {
        String gameId;
        List<Pick> draftPicks = new ArrayList<>();
        Map<String, Integer> finalPlayerWorths = new HashMap<>(); 
        GameAnalysis(String gameId) { this.gameId = gameId; }
    }

    private static class Pick {
        int pickNumber;
        String playerId;
        String assetId;
        Pick(int pickNumber, String playerId, String assetId) {
            this.pickNumber = pickNumber;
            this.playerId = playerId;
            this.assetId = assetId;
        }
    }

    private static class AnalysisOutput {
        Map<String, Map<String, Double>> pickPopularity; 
        Map<String, Double> assetPerformance;          
        Map<String, Double> synergyPerformance;        
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            log.error("Usage: java InitialRoundAnalyzer <old_games> <game_logs> <output.json>");
            return;
        }

        File oldGamesFolder = new File(args[0]);
        File gameLogsFolder = new File(args[1]);
        File outputFile = new File(args[2]);

        log.info("Directory Search: {}", gameLogsFolder.getAbsolutePath());
        if (!gameLogsFolder.exists()) {
            log.error("CRITICAL: The directory {} does not exist!", gameLogsFolder.getAbsolutePath());
            return;
        }
        // ---------------------

        log.info("Starting Initial Round analysis...");
        
        List<GameAnalysis> allGameData = new ArrayList<>();

        // 1. Find all snapshot directories (Using robust FilenameFilter)
        File[] historyDirs = gameLogsFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory() && name.endsWith("_history");
            }
        });

        if (historyDirs == null || historyDirs.length == 0) {
            log.warn("No '_history' directories found in {}. (Total files in dir: {})", 
                gameLogsFolder.getName(), 
                gameLogsFolder.list() == null ? 0 : gameLogsFolder.list().length);
            
            log.info("Tip: Did you run 'BatchLogGenerator'?");
            return;
        }

        log.info("Found {} game histories to process.", historyDirs.length);

        // 2. Process each game
        for (File snapshotDir : historyDirs) {
            String baseName = snapshotDir.getName().replace("_history", "");
            File originalRailsFile = new File(oldGamesFolder, baseName + ".rails");

            if (!originalRailsFile.isFile()) {
                // log.warn("Skipping {} (No matching .rails file)", baseName);
                continue;
            }

            // log.info("Analyzing: {}", baseName);
            GameAnalysis gameAnalysis = new GameAnalysis(baseName);

            try {
                List<PossibleAction> actions = GameLoader.loadActionsFromFile(originalRailsFile);
                File lastStateFile = findLastStateFile(snapshotDir);
                
                if (lastStateFile == null) continue;

                GameStateData lastState = GSON.fromJson(new FileReader(lastStateFile), GameStateData.class);
                for (GameStateData.PlayerData player : lastState.players) {
                    int finalWorth = getPlayerWorthFromState(lastState, player.id);
                    gameAnalysis.finalPlayerWorths.put(player.id, finalWorth);
                }

                int pickCount = 1;
                int itemsFound = 0;
                final int TOTAL_START_ITEMS = 13; // Standard 1835 count

                for (PossibleAction action : actions) {
                    if (action instanceof BuyStartItem) {
                        BuyStartItem pick = (BuyStartItem) action;
                        String itemName = "Unknown";
                        if (pick.getStartItem() != null) itemName = pick.getStartItem().getId();
                        
                        gameAnalysis.draftPicks.add(new Pick(pickCount++, pick.getPlayerName(), itemName));
                        itemsFound++;
                    } else if (action instanceof NullAction) {
                        continue; // Ignore pass
                    }

                    if (itemsFound >= TOTAL_START_ITEMS) break;
                }
                
                if (!gameAnalysis.draftPicks.isEmpty()) {
                    allGameData.add(gameAnalysis);
                }

            } catch (Exception e) {
                log.error("Error analyzing {}: {}", baseName, e.getMessage());
            }
        }

        // 3. Aggregate and Save
        if (!allGameData.isEmpty()) {
            AnalysisOutput results = performAggregation(allGameData);
            saveResults(results, outputFile);
        } else {
            log.warn("No game data was successfully processed.");
        }
    }

    private static File findLastStateFile(File snapshotDir) {
        File[] states = snapshotDir.listFiles((dir, name) -> name.startsWith("state_") && name.endsWith(".json"));
        if (states == null || states.length == 0) return null;
        Arrays.sort(states, Collections.reverseOrder());
        return states[0];
    }

    private static int getPlayerWorthFromState(GameStateData state, String playerId) {
        // Fallback: read directly from JSON struct if restoring fails, 
        // but using Restorer is safer for logic consistency.
        try {
            GameStateRestorer restorer = new GameStateRestorer();
            RailsRoot root = restorer.restoreState(state);
            for (Player p : root.getPlayerManager().getPlayers()) {
                if (p.getId().equals(playerId)) return p.getWorth();
            }
        } catch (Exception e) {
            // Simplified Fallback: Just assume cash if worth calc fails
            for (GameStateData.PlayerData pd : state.players) {
                if (pd.id.equals(playerId)) return pd.cash;
            }
        }
        return 0;
    }

    private static AnalysisOutput performAggregation(List<GameAnalysis> allGameData) {
        AnalysisOutput output = new AnalysisOutput();
        output.pickPopularity = new HashMap<>();
        output.assetPerformance = new HashMap<>();
        output.synergyPerformance = new HashMap<>();

        Map<String, List<Integer>> assetWorths = new HashMap<>();
        Map<String, List<Integer>> synergyWorths = new HashMap<>();
        Map<Integer, Map<String, Integer>> pickCounts = new HashMap<>();

        for (GameAnalysis game : allGameData) {
            Map<String, Integer> finalWorths = game.finalPlayerWorths;
            Map<String, Set<String>> playerAssets = new HashMap<>();
            for (String pid : finalWorths.keySet()) playerAssets.put(pid, new HashSet<>());

            for (Pick pick : game.draftPicks) {
                pickCounts.computeIfAbsent(pick.pickNumber, k -> new HashMap<>())
                          .merge(pick.assetId, 1, Integer::sum);

                int worth = finalWorths.getOrDefault(pick.playerId, 0);
                assetWorths.computeIfAbsent(pick.assetId, k -> new ArrayList<>()).add(worth);
                
                if (playerAssets.containsKey(pick.playerId)) {
                    playerAssets.get(pick.playerId).add(pick.assetId);
                }
            }

            // Synergy
            for (Map.Entry<String, Set<String>> entry : playerAssets.entrySet()) {
                List<String> assets = new ArrayList<>(entry.getValue());
                int worth = finalWorths.getOrDefault(entry.getKey(), 0);
                
                for (int i = 0; i < assets.size(); i++) {
                    for (int j = i + 1; j < assets.size(); j++) {
                        String key = generateSortedKey(assets.get(i), assets.get(j));
                        synergyWorths.computeIfAbsent(key, k -> new ArrayList<>()).add(worth);
                    }
                }
            }
        }

        // Calculate Stats
        for (var entry : pickCounts.entrySet()) {
            String key = "Pick_" + entry.getKey();
            double total = entry.getValue().values().stream().mapToInt(i->i).sum();
            Map<String, Double> freq = new HashMap<>();
            entry.getValue().forEach((k, v) -> freq.put(k, (v / total) * 100.0));
            output.pickPopularity.put(key, freq);
        }

        assetWorths.forEach((k, v) -> output.assetPerformance.put(k, v.stream().mapToInt(i->i).average().orElse(0.0)));
        
        synergyWorths.forEach((k, v) -> {
            if (v.size() >= 3) {
                output.synergyPerformance.put(k, v.stream().mapToInt(i->i).average().orElse(0.0));
            }
        });

        return output;
    }

    private static String generateSortedKey(String a, String b) {
        if (a.compareTo(b) < 0) return a + "+" + b;
        return b + "+" + a;
    }

    private static void saveResults(AnalysisOutput results, File outputFile) {
        try (FileWriter writer = new FileWriter(outputFile)) {
            GSON.toJson(results, writer);
            log.info("Successfully saved analysis to {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("FATAL: Could not write final analysis JSON.", e);
        }
    }
}