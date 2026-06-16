package net.sf.rails.game.ai.playground;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.ai.snapshot.GameStateData;
import net.sf.rails.util.GameLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.LayTile;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Builds the expertstrategies1835.json catalogue from historical game data.
 *
 * It scans all game_log history directories, loads the corresponding .rails files,
 * and aggregates every human tile lay into a weighted list of moves based on
 * the game state context (company, phase, friendly companies).
 *
 * Usage: java StrategyCatalogueBuilder <old_games_folder> <game_logs_folder> <output_json_file>
 */
public class StrategyCatalogueBuilder {

    private static final Logger log = LoggerFactory.getLogger(StrategyCatalogueBuilder.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Define the "important pairings"
    // Key = Company, Value = Set of "important friends"
    private static final Map<String, Set<String>> IMPORTANT_PAIRS = createImportantPairsMap();

    private static Map<String, Set<String>> createImportantPairsMap() {
        Map<String, Set<String>> m = new HashMap<>();
        
        // Helper to add a reciprocal pair
        BiConsumer<String, String> addPair = (c1, c2) -> {
            m.computeIfAbsent(c1, k -> new HashSet<>()).add(c2);
            m.computeIfAbsent(c2, k -> new HashSet<>()).add(c1);
        };

        // Add the pairs you listed:
// Bavaria
        addPair.accept("BY", "OBB");
        addPair.accept("BY", "NF");
        addPair.accept("BY", "M1"); // Late game West-South connection

        // The North/East Axis
        addPair.accept("SX", "M3"); // Leipzig <-> Magdeburg (Crucial)
        addPair.accept("SX", "M5"); // Leipzig <-> Berlin
        addPair.accept("M2", "M3"); // Berlin <-> Magdeburg
        addPair.accept("M2", "M5"); // Berlin Internal
        addPair.accept("M3", "M6"); // Magdeburg <-> Hamburg (North-South Link)
        addPair.accept("M5", "M6"); // Berlin <-> Hamburg

        // The West (Ruhr)
        addPair.accept("M1", "M4"); // Bergisch <-> Koln-Mindener
        addPair.accept("M1", "BA"); // Bergisch <-> Koln-Mindener
        addPair.accept("M1", "WT"); // Bergisch <-> Koln-Mindener
        addPair.accept("M4", "WT"); // Bergisch <-> Koln-Mindener
        addPair.accept("M4", "BA"); // Bergisch <-> Koln-Mindener

        // The South
        addPair.accept("BA", "HE"); // Baden <-> Hessen
        addPair.accept("WT", "HE"); // Wurttemburg <-> Hessen
        addPair.accept("BA", "WT"); // Baden <-> Wurttemburg

        return m;
    }


    // --- POJO Classes for the expertstrategies1835.json structure ---

    public static class StrategyCatalogue {
        List<StrategyEntry> tileLayingStrategies = new ArrayList<>();
        // In the future, we can add:
        // List<StrategyEntry> tokenLayingStrategies = new ArrayList<>();
        // List<StrategyEntry> trainBuyingStrategies = new ArrayList<>();
    }

    public static class StrategyEntry {
        StrategyContext context;
        List<StrategyMove> moves = new ArrayList<>();

        public StrategyEntry(StrategyContext context) {
            this.context = context;
        }
    }

    public static class StrategyContext {
        String companyId;
        String phase;
        List<String> friendlyCompanyIds; // Sorted list of companies with same president

        public StrategyContext(String companyId, String phase, List<String> friendlyCompanyIds) {
            this.companyId = companyId;
            this.phase = phase;
            this.friendlyCompanyIds = friendlyCompanyIds;
            // Sort for consistent matching
            Collections.sort(this.friendlyCompanyIds);
        }

        // Auto-generated equals/hashCode to allow use as a Map key
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StrategyContext context = (StrategyContext) o;
            return Objects.equals(companyId, context.companyId) &&
                   Objects.equals(phase, context.phase) &&
                   Objects.equals(friendlyCompanyIds, context.friendlyCompanyIds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, phase, friendlyCompanyIds);
        }
    }

    public static class StrategyMove {
        String hexId;
        String tileId;
        String rotation;
        int weight;

        public StrategyMove(String hexId, String tileId, String rotation) {
            this.hexId = hexId;
            this.tileId = tileId;
            this.rotation = rotation;
            this.weight = 1; // Start with a weight of 1
        }

        // Auto-generated equals/hashCode to find and increment weight
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StrategyMove that = (StrategyMove) o;
            return Objects.equals(hexId, that.hexId) &&
                   Objects.equals(tileId, that.tileId) &&
                   Objects.equals(rotation, that.rotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hexId, tileId, rotation);
        }
    }

    // --- Main Tool Logic ---

    public static void main(String[] args) {
        if (args.length != 3) {
            log.error("Usage: java StrategyCatalogueBuilder <old_games_folder> <game_logs_folder> <output_json_file>");
            log.error("Example: ... BatchLogGenerator 'old_games' 'game_logs' 'expertstrategies1835.json'");
            return;
        }

        File oldGamesFolder = new File(args[0]);
        File gameLogsFolder = new File(args[1]);
        File outputFile = new File(args[2]);

        if (!oldGamesFolder.isDirectory() || !gameLogsFolder.isDirectory()) {
            log.error("Error: Both <old_games_folder> and <game_logs_folder> must be valid directories.");
            return;
        }

        log.info("Starting Strategy Catalogue build...");
        log.info("Input (Actions): {}", oldGamesFolder.getAbsolutePath());
        log.info("Input (States):  {}", gameLogsFolder.getAbsolutePath());
        log.info("Output (JSON):   {}", outputFile.getAbsolutePath());
        
        // This is our in-memory database
        StrategyCatalogue catalogue = new StrategyCatalogue();
        // A helper map to quickly find an existing strategy by its context
        Map<StrategyContext, StrategyEntry> catalogueIndex = new HashMap<>();

        // 1. Find all snapshot directories in the game_logs folder
        File[] historyDirs = gameLogsFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory() && name.endsWith("_history");
            }
        });

        if (historyDirs == null || historyDirs.length == 0) {
            log.warn("No '_history' directories found in {}. Nothing to process.", gameLogsFolder.getAbsolutePath());
            return;
        }

        log.info("Found {} game histories to process.", historyDirs.length);
        int totalMovesProcessed = 0;

        // 2. Process each game
        for (File snapshotDir : historyDirs) {
            String baseName = snapshotDir.getName().replace("_history", "");
            File originalRailsFile = new File(oldGamesFolder, baseName + ".rails");

            if (!originalRailsFile.isFile()) {
                log.warn("Skipping. Found state directory '{}' but no matching '.rails' file in '{}'", 
                    snapshotDir.getName(), oldGamesFolder.getName());
                continue;
            }

            log.info("--- Processing Game: {} ---", baseName);
            totalMovesProcessed += processGame(originalRailsFile, snapshotDir, catalogueIndex);
        }

        // 3. Save the final catalogue
        // Convert the map back to the list structure required by the JSON
        catalogue.tileLayingStrategies.addAll(catalogueIndex.values());
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            GSON.toJson(catalogue, writer);
            log.info("--- BUILD COMPLETE ---");
            log.info("Successfully saved Strategy Catalogue to {}", outputFile.getAbsolutePath());
            log.info("Total tile lay moves indexed: {}", totalMovesProcessed);
        } catch (Exception e) {
            log.error("FATAL: Could not write final JSON catalogue.", e);
        }
    }

    /**
     * Processes a single game, loading its actions and states, and adding them
     * to the catalogue index.
     */
    private static int processGame(File originalFile, File stateFolder, Map<StrategyContext, StrategyEntry> catalogueIndex) {
        int movesProcessed = 0;
        try {
            List<PossibleAction> actions = GameLoader.loadActionsFromFile(originalFile);

            for (int i = 0; i < actions.size(); i++) {
                PossibleAction action = actions.get(i);

                // We only care about tile lays
                if (!(action instanceof LayTile) || action.isCorrection()) {
                    continue;
                }
                
                LayTile tileAction = (LayTile) action;
                
                // Load the corresponding state file
                String stateFileName = String.format(Locale.ROOT, "state_%05d.json", i);
                File stateFile = new File(stateFolder, stateFileName);
                if (!stateFile.isFile()) {
                    continue; // Skip if state is missing (e.g., end of log)
                }

                // Parse the state and action
                GameStateData preState = GSON.fromJson(new FileReader(stateFile), GameStateData.class);
                StrategyContext context = getContextFromState(preState, tileAction);
                StrategyMove move = getMoveFromAction(tileAction);

                // --- This is the core logic ---
                // 1. Find the strategy for this exact context
                StrategyEntry entry = catalogueIndex.get(context);
                if (entry == null) {
                    // This is the first time we've seen this context. Create a new entry.
                    entry = new StrategyEntry(context);
                    catalogueIndex.put(context, entry);
                }

                // 2. Find the specific move in this strategy's list
                int moveIndex = entry.moves.indexOf(move);
                if (moveIndex != -1) {
                    // We've seen this exact move before. Increment its weight.
                    entry.moves.get(moveIndex).weight++;
                } else {
                    // This is a new move for this context. Add it.
                    entry.moves.add(move);
                }
                movesProcessed++;
            }

        } catch (Exception e) {
            log.error("Failed to process game {}: {}", originalFile.getName(), e.getMessage());
        }
        log.info("Indexed {} tile lay moves for this game.", movesProcessed);
        return movesProcessed;
    }

    /**
     * Extracts the "Intelligent Context" from the game state.
     */
    private static StrategyContext getContextFromState(GameStateData state, LayTile tileAction) {
        String companyId = ((PossibleORAction) tileAction).getCompany().getId();

        String phase;
        if (state.phase == null) {
            log.warn("GameStateData 'phase' object is null for company {}. Defaulting phase to '1'", companyId);
            phase = "1"; // Fallback
        } else {
            // This logic MUST mirror ExpertStrategyService.java
            // We assume 'offBoardRevenueStep' exists in the GameStateData.PhaseData POJO.
            int step = state.phase.offBoardRevenueStep; 
            phase = String.valueOf(step + 1);
        }
        // Find the president of the operating company
        String presidentId = null;
        for (GameStateData.CompanyData co : state.publicCompanies) {
            if (co.id.equals(companyId)) {
                presidentId = co.presidentId;
                break;
            }
        }
       List<String> friendlyCompanyIds = new ArrayList<>();
        Set<String> importantFriends = IMPORTANT_PAIRS.getOrDefault(companyId, Collections.emptySet());

        if (presidentId != null && !importantFriends.isEmpty()) {
            for (GameStateData.CompanyData co : state.publicCompanies) {
                // Filter: Must be owned by same president AND be a relevant geographic neighbor
                if (!co.id.equals(companyId) && 
                    presidentId.equals(co.presidentId) && 
                    importantFriends.contains(co.id)) {
                    
                    friendlyCompanyIds.add(co.id);
                }
            }
        }
        
        return new StrategyContext(companyId, phase, friendlyCompanyIds);
    }

    /**
     * Extracts the specific move data from the action.
     */
    private static StrategyMove getMoveFromAction(LayTile tileAction) {
        String hexId = tileAction.getChosenHex().getId();
        String tileId = tileAction.getLaidTile().toText();
        String rotation = tileAction.getChosenHex().getOrientationName(tileAction.getOrientation());
        
        return new StrategyMove(hexId, tileId, rotation);
    }
}