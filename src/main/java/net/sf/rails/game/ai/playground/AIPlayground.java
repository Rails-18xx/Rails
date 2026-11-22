package net.sf.rails.game.ai.playground;

// --- Necessary Imports ---
import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo; 
import net.sf.rails.common.GameOptionsSet; 
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.ai.AIEvaluatorService;
import net.sf.rails.game.ai.AIPathfinderService;
import net.sf.rails.game.ai.BuyTrainOption; 
import net.sf.rails.game.ai.ExpertStrategyService;
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.TokenLayOption; 
import rails.game.action.BuyTrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader; 
import java.io.FileReader;    
import java.io.IOException;     
import java.nio.file.Files;     
import java.nio.file.Paths;     
import java.util.ArrayList;
import java.util.HashMap; // --- [FIX] Added import for Map ---
import java.util.List;
import java.util.Map; // --- [FIX] Added import for Map ---
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// --- End Imports ---

/**
 * The main executable for the test harness.
 * Loads services, calls parser, and prints scores.
 */
public class AIPlayground {

    private static final Logger aiLog = LoggerFactory.getLogger("AI_Decision_Log");
    private static final String DEFAULT_STATE_FILE = "OR1_M1_state.txt";

    public static void main(String[] args) {
        String stateFilePath = DEFAULT_STATE_FILE;
        if (args.length > 0) {
            stateFilePath = args[0];
            aiLog.info("Using state file provided via command line: {}", stateFilePath);
        } else {
            aiLog.info("Using default state file: {}", stateFilePath);
        }

        // --- 1. Create Mock RailsRoot ---
        RailsRoot mockRailsRoot = createMockRailsRoot();
        if (mockRailsRoot == null) {
            aiLog.error("Critical: Failed to create Mock RailsRoot. Exiting playground.");
            return;
        }

        // --- 2. Load and Parse State File ---
        StaticGameContext parsedState = loadAndParseStateFile(stateFilePath, mockRailsRoot);
        if (parsedState == null) {
            aiLog.error("Critical: Failed to load or parse state file: {}. Exiting playground.", stateFilePath);
            return;
        }
        aiLog.info("Successfully parsed state for company: {}", parsedState.getOperatingCompanyId());

        // --- 3. Instantiate AI Services ---
        AIEvaluatorService evaluator = new AIEvaluatorService(); 
        AIPathfinderService pathfinder = new AIPathfinderService(); 
        ExpertStrategyService expertStrategyService = new ExpertStrategyService(evaluator, pathfinder); 

// --- [FIX] Create dummy 'memory' for test harness ---
        Map<String, Integer> dummyTileLayStep = new HashMap<>();
        
        // [RECOMMENDED ADDITION] Manually set script state for testing
        // To test M2's Step 2 (which expects step 1):
        dummyTileLayStep.put("M2", 1); 
        aiLog.info("--- Overriding script step for M2 to 1 ---");

        // --- 4. Score Tile Lays ---
        // --- 4. Score Tile Lays ---
        aiLog.info("--- Scoring Tile Lays ---");
        try {
            List<TileLayOption> tileOptions = parsedState.getTileLayOptions(); 
            if (tileOptions.isEmpty()) {
                aiLog.info("  No TileLay actions found in state file.");
            } else {
                aiLog.info("Found {} TileLay options.", tileOptions.size());
                for (TileLayOption option : tileOptions) {
                    // --- [FIX] Pass the dummy memory map ---
double score = expertStrategyService.scoreTileLay(option, parsedState);
                    aiLog.info("  - Score: {:<10.2f} | Option: {}", score, option);
                }
            }
        } catch (Exception e) {
            aiLog.error("Error during tile scoring: {}", e.getMessage());
            e.printStackTrace();
        }

        // --- 5. Score Token Lays ---
        aiLog.info("--- Scoring Token Lays ---");
        aiLog.info("  (Token scoring not yet implemented in playground)");


        // --- 6. Score Train Buys ---
        aiLog.info("--- Scoring Train Buys for: {} ---", parsedState.getOperatingCompanyId());
        List<BuyTrainOption> legalTrainBuyOptions = parsedState.getLegalTrainBuyOptions(); 

        if (legalTrainBuyOptions.isEmpty()) {
             aiLog.info("  No BuyTrain actions found in state file.");
        } else {
            aiLog.info("Found {} BuyTrain options.", legalTrainBuyOptions.size());
            for (BuyTrainOption option : legalTrainBuyOptions) {
                try {
                    RailsRoot root = parsedState.getRailsRoot(); 
                    if (root == null) {
                         aiLog.error("Cannot score BuyTrainOption {}: RailsRoot is null in StaticGameContext.", option.getTrainId());
                         continue; 
                    }
                    PublicCompany operatingCompany = parsedState.getOperatingCompany();
                     if (operatingCompany == null) {
                         String compId = parsedState.getOperatingCompanyId();
                         operatingCompany = (compId != null) ? root.getCompanyManager().getPublicCompany(compId) : null;
                         if (operatingCompany == null) {
                             aiLog.error("Cannot score BuyTrainOption {}: OperatingCompany ID {} not found or not set.", option.getTrainId(), compId);
                             continue; 
                         }
                     }

                    Train trainToBuy = root.getTrainManager().getTrainByUniqueId(option.getTrainId());
                    if (trainToBuy == null) {
                         aiLog.error("Could not find Train object for ID: {}", option.getTrainId());
                         continue; 
                    }

                    BuyTrain buyTrainAction = new BuyTrain(trainToBuy, operatingCompany, option.getCost());
                    double score = expertStrategyService.scoreBuyTrain(buyTrainAction, parsedState);
                    aiLog.info("  - Score: {:<10.2f} | Option: Buy Train {}, Cost {}", score, option.getTrainId(), option.getCost());

                } catch (Exception e) {
                     aiLog.error("Error scoring BuyTrainOption {}: {}", option, e.getMessage());
                     e.printStackTrace(); 
                }
            }
        }

        aiLog.info("--- Playground Run Complete ---");
    }

    /**
     * Parses the .txt state file into a StaticGameContext object.
     */
    private static StaticGameContext loadAndParseStateFile(String filePath, RailsRoot root) {
        StaticGameContext context = new StaticGameContext(root);
        aiLog.info("Loading and parsing state file: {}", filePath);

        String operatingCompanyId = null; 

        Pattern companyIdPattern = Pattern.compile("^SAVED_FOR_COMPANY:\\s*(\\w+)");
        Pattern companyDataPattern = Pattern.compile("^COMPANY\\[(\\w+)\\]:\\s*CASH=(\\d+),\\s*TRAINS=([\\w_,]*),.*"); 
        Pattern buyTrainPattern = Pattern.compile("^POSSIBLE_ACTION\\[\\d+\\]:\\s*(TYPE=BuyTrain.*)");
        Pattern tileLayPattern = Pattern.compile("^LEGAL_TILE_LAY\\[\\d+\\]:\\s*(HEX=\\w+,.*)"); 

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; 
                }

                // --- Parse Company ID ---
                Matcher idMatcher = companyIdPattern.matcher(line);
                if (idMatcher.find()) {
                    operatingCompanyId = idMatcher.group(1);
                    context.setOperatingCompanyId(operatingCompanyId); 
                    aiLog.debug("Parsed Operating Company ID: {}", operatingCompanyId);
                    continue; 
                }

                // --- Parse Company Data ---
                Matcher dataMatcher = companyDataPattern.matcher(line);
                if (dataMatcher.find()) {
                    String companyId = dataMatcher.group(1);
                    if (companyId.equals(operatingCompanyId)) {
                        try {
                            int cash = Integer.parseInt(dataMatcher.group(2));
                            String trainsStr = dataMatcher.group(3);
                            int trainCount = (trainsStr == null || trainsStr.isEmpty()) ? 0 : trainsStr.split(",").length;
                            int trainLimit = 3; 
                            context.setOperatingCompanyData(cash, trainCount, trainLimit); 
                            aiLog.debug("Parsed data for {}: Cash={}, Trains={}, Limit={}", operatingCompanyId, cash, trainCount, trainLimit);
                        } catch (NumberFormatException e) {
                             aiLog.warn("Could not parse cash for company {} in line: {}", companyId, line);
                        }
                    }
                    continue; 
                }

                // --- Parse BuyTrain Actions ---
                Matcher buyTrainMatcher = buyTrainPattern.matcher(line);
                if (buyTrainMatcher.find()) {
                    try {
                        String data = buyTrainMatcher.group(1); 
                        BuyTrainOption option = BuyTrainOption.parse(data); 
                        context.addBuyTrainOption(option); 
                        aiLog.debug("Parsed BuyTrainOption: {}", option);
                    } catch (Exception e) {
                        aiLog.warn("Failed to parse BuyTrainOption from line: {}", line, e);
                    }
                    continue; 
                }

                // --- Parse TileLay Actions ---
                Matcher tileLayMatcher = tileLayPattern.matcher(line);
                if (tileLayMatcher.find()) {
                    try {
                        String data = tileLayMatcher.group(1); 
                        TileLayOption option = TileLayOption.parse(data);
                        context.addTileLayOption(option); 
                         aiLog.debug("Parsed TileLayOption: {}", option);
                    } catch (Exception e) {
                        aiLog.warn("Failed to parse TileLayOption from line: {}", line, e);
                    }
                    continue; 
                }
            }
        } catch (IOException e) {
            aiLog.error("Failed to read state file {}: {}", filePath, e.getMessage());
            e.printStackTrace();
            return null; 
        } catch (Exception e) {
            aiLog.error("Unexpected error during parsing of state file {}: {}", filePath, e.getMessage());
            e.printStackTrace();
            return null; 
        }

        if (context.getOperatingCompanyId() == null) {
            aiLog.error("Could not find 'SAVED_FOR_COMPANY:' line in state file: {}", filePath);
        }

        aiLog.info("Finished parsing state file.");
        return context;
    }


    /**
     * Creates a minimal mock RailsRoot needed for the playground.
     */
    private static RailsRoot createMockRailsRoot() {
        try {
            aiLog.debug("Creating mock RailsRoot...");

            GameInfo mockGameInfo = GameInfo.builder()
                                        .withName("1835")
                                        .withMinPlayers(2) 
                                        .withMaxPlayers(6) 
                                        .build();

            GameOptionsSet.Builder mockOptionsBuilder = GameOptionsSet.builder();

            GameData mockGameData = GameData.create(mockGameInfo, mockOptionsBuilder, new ArrayList<String>());

            RailsRoot mockRoot = RailsRoot.create(mockGameData);

            if (mockRoot.getTrainManager() == null || mockRoot.getCompanyManager() == null || mockRoot.getTileManager() == null ) {
                 aiLog.error("One or more essential managers (Train, Company, Tile) are NULL after RailsRoot.create!");
                 return null; 
            } else {
                 aiLog.debug("Mock TrainManager, CompanyManager, TileManager seem initialized.");
            }

            aiLog.debug("Mock RailsRoot created successfully.");
            return mockRoot;

        } catch (ConfigurationException e) { 
            aiLog.error("ConfigurationException creating mock RailsRoot: {}", e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) { 
             aiLog.error("Unexpected error creating mock RailsRoot: {}", e.getMessage());
             e.printStackTrace();
             return null;
        }
    }


    /**
     * Extracts a value for a given key from a string like "KEY1=VAL1, KEY2=VAL2".
     */
    private static String extractValue(String lineData, String key) {
        try {
            Pattern p = Pattern.compile(Pattern.quote(key) + "=([\\w_]+)");
            Matcher m = p.matcher(lineData);
            if (m.find()) {
                return m.group(1); 
            }
        } catch (Exception e) {
            aiLog.warn("Regex error extracting key '{}' from '{}': {}", key, lineData, e.getMessage());
        }
        return null; 
    }

}