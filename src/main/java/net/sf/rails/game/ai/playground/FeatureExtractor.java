package net.sf.rails.game.ai.playground;

import com.google.gson.Gson;
import net.sf.rails.game.*;
import net.sf.rails.game.ai.snapshot.GameStateData;
import net.sf.rails.game.ai.snapshot.GameStateRestorer;
import net.sf.rails.algorithms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.LayTile;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;
import net.sf.rails.util.GameLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main analysis tool.
 * Calculates both Immediate Revenue (Delta R) and Potential Revenue (Sigma C)
 * for all tile lays in a game history and saves them to a .tsv file.
 *
 * Usage: java FeatureExtractor <rails_file> <state_history_dir> <output_tsv_file>
 */
public class FeatureExtractor {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtractor.class);
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        if (args.length != 3) {
            log.error("Usage: java FeatureExtractor <path_to_original_rails_file> <path_to_state_history_directory> <path_to_output.tsv>");
            return;
        }

        File originalFile = new File(args[0]);
        File stateFolder = new File(args[1]);
        File outputFile = new File(args[2]);

        if (!originalFile.isFile() || !stateFolder.isDirectory()) {
            log.error("Invalid paths. Please provide a valid .rails file and a valid _history directory.");
            return;
        }

        log.info("Loading action sequence from: {}", originalFile.getName());
        log.info("Loading states from: {}", stateFolder.getName());

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Write the TSV Header
            writer.println("ActionID\tCompany\tHex\tTile\tPhase\tDelta_R\tDelta_SigmaC");

            List<PossibleAction> actions = GameLoader.loadActionsFromFile(originalFile);
            log.info("Successfully loaded {} actions. Filtering for LayTile...", actions.size());

            int tileLaysFound = 0;

            for (int i = 0; i < actions.size(); i++) {
                PossibleAction action = actions.get(i);

                if (!(action instanceof LayTile) || action.isCorrection()) {
                    continue;
                }

                LayTile tileAction = (LayTile) action;
                String stateFileName = String.format(Locale.ROOT, "state_%05d.json", i);
                File stateFile = new File(stateFolder, stateFileName);

                if (!stateFile.isFile()) {
                    log.warn("Missing state file for action {}: {}. Halting analysis.", i, stateFileName);
                    break;
                }

                GameStateData preState = GSON.fromJson(new FileReader(stateFile), GameStateData.class);
                String companyId = ((PossibleORAction) tileAction).getCompany().getId();
                String phaseId = preState.phase.currentPhaseName;

                // 1. Calculate metrics for the pre-state
                double r_pre = calculateImmediateRevenue(preState, companyId);
                double sigmaC_pre = calculatePotentialRevenue(preState, companyId, phaseId);

                // 2. Create the post-action state (S')
                GameStateData postState = createPostTileState(preState, tileAction);

                // 3. Calculate metrics for the post-state
                double r_post = calculateImmediateRevenue(postState, companyId);
                double sigmaC_post = calculatePotentialRevenue(postState, companyId, phaseId);

                // 4. Calculate the deltas
                int delta_R = (int)(r_post - r_pre);
                int delta_SigmaC = (int)(sigmaC_post - sigmaC_pre);

                // 5. Log to console and write to file
                String logMsg = String.format(Locale.ROOT,
                    "Action #%d (Co: %s, Hex: %s) -> dR: %dM, dSigmaC: %dM",
                    preState.gameManager.absoluteActionCounter, companyId, tileAction.getChosenHex().getId(),
                    delta_R, delta_SigmaC
                );
                log.info(logMsg);
                
                writer.printf(Locale.ROOT, "%d\t%s\t%s\t%s\t%s\t%d\t%d\n",
                    preState.gameManager.absoluteActionCounter,
                    companyId,
                    tileAction.getChosenHex().getId(),
                    tileAction.getLaidTile().toText(),
                    phaseId,
                    delta_R,
                    delta_SigmaC
                );

                tileLaysFound++;
            }
            
            log.info("---------------------------------");
            log.info("Calculation Complete. Found {} LayTile actions.", tileLaysFound);
            log.info("Database saved to: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Feature extraction FAILED.", e);
            e.printStackTrace(); // Show the full error
        }
    }

    /**
     * Calculates Immediate Revenue (Delta R) using the company's *actual* trains.
     */
    private static double calculateImmediateRevenue(GameStateData state, String companyId) throws Exception {
        GameStateRestorer restorer = new GameStateRestorer();
        RailsRoot root = restorer.restoreState(state);
        GameManager gm = root.getGameManager();
        PublicCompany company = root.getCompanyManager().getPublicCompany(companyId);

        if (company == null) return 0.0;

        // Create adapter, this time with 'true' to load REAL trains
        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(root, company, gm.getCurrentPhase(), true);
        
        ra.initRevenueCalculator(true);
        return (double) ra.calculateRevenue();
    }


    /**
     * Calculates Potential Revenue (Sigma C) using phase-appropriate VIRTUAL trains.
     */
    private static double calculatePotentialRevenue(GameStateData state, String companyId, String phaseId) throws Exception {
        GameStateRestorer restorer = new GameStateRestorer();
        RailsRoot root = restorer.restoreState(state);
        GameManager gm = root.getGameManager();
        PublicCompany company = root.getCompanyManager().getPublicCompany(companyId);
        Phase phase = gm.getCurrentPhase();

        if (company == null) return 0.0;

        // Determine which virtual trains to use
        String virtualTrainName;
        int trainCount;
        
        if (phaseId.equals("1") || phaseId.equals("2")) {
            virtualTrainName = "3";
            trainCount = 2; // Two '3' trains
        } else {
            virtualTrainName = "4";
            trainCount = 2; // Two '4' trains
        }

        // Create adapter *without* populating real trains
        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(root, company, phase, false); // false = don't populate

        // Manually find home tokens
        NetworkAdapter networkAdapter = ra.getNetworkAdapterInternal();
        NetworkGraph graph = networkAdapter.getRouteGraphCached(company, false);
        NetworkVertex.initAllRailsVertices(graph, company, phase, true);
        ra.addStartVertices(graph.getCompanyBaseTokenVertexes(company));

        // Add the correct number of virtual trains
        for (int i = 0; i < trainCount; i++) {
            NetworkTrain virtualTrain = NetworkTrain.createFromString(virtualTrainName);
            if (virtualTrain != null) {
                ra.addTrain(virtualTrain);
            } else {
                 log.error("Could not create virtual NetworkTrain from string '{}'", virtualTrainName);
                 return 0.0;
            }
        }
        
        ra.initRevenueCalculator(true);
        return (double) ra.calculateRevenue();
    }


    /**
     * Creates a new, deep-copied GameStateData object with the tile lay applied.
     */
    private static GameStateData createPostTileState(GameStateData preState, LayTile tileAction) {
        String json = GSON.toJson(preState);
        GameStateData postState = GSON.fromJson(json, GameStateData.class);

        String targetHexId = tileAction.getChosenHex().getId();
        String newTileId = tileAction.getLaidTile().toText();
        String newRotationName = tileAction.getChosenHex().getOrientationName(tileAction.getOrientation());

        for (GameStateData.MapHexData hexData : postState.map.hexes) {
            if (hexData.id.equals(targetHexId)) {
                hexData.tileId = newTileId;
                hexData.rotationName = newRotationName;
                break;
            }
        }
        return postState;
    }
}