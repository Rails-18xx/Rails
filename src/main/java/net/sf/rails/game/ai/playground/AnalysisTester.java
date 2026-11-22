package net.sf.rails.game.ai.playground;

import com.google.gson.Gson;
import net.sf.rails.game.*;
import net.sf.rails.game.ai.snapshot.GameStateData;
import net.sf.rails.game.ai.snapshot.GameStateRestorer;
import net.sf.rails.algorithms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import javax.swing.JFrame;

/**
 * A utility class to validate the data pipeline and calculate R_pre (Pre-Action
 * Revenue).
 */
public class AnalysisTester {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTester.class);
    // Gson is required to load the JSON snapshot
    private static final Gson GSON = new Gson();

    /**
     * Calculates the maximum possible revenue for a given company in a given state.
     * This forms the R_pre half of the Delta R calculation.
     */
    public static double calculateMaxRevenue(GameStateData state, String companyId) throws Exception {
        // 1. Re-hydrate the state
        GameStateRestorer restorer = new GameStateRestorer();
        // The core step: rebuilding the live game object from the snapshot
        RailsRoot root = restorer.restoreState(state);
        GameManager gm = root.getGameManager();

        // 2. Find the target company
        PublicCompany operatingCompany = root.getCompanyManager().getPublicCompany(companyId);

        if (operatingCompany == null) {
            log.warn("Company {} not found in state.", companyId);
            return 0.0;
        }

        // 3. Simulate Revenue (R_pre)
        // This relies on the core engine's route calculation logic.
        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(root, operatingCompany, gm.getCurrentPhase());
        ra.initRevenueCalculator(true);
        return (double) ra.calculateRevenue();
    }

    /**
     * Temporary main method to run a quick test on a single state file.
     * Usage: java AnalysisTester <path_to_state_file> <company_id>
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            log.error("Usage: java AnalysisTester <path_to_state_file> <company_id>");
            return;
        }

        try {
            File stateFile = new File(args[0]);
            String companyId = args[1];

            // 1. Load the state POJO from JSON
            GameStateData stateData = GSON.fromJson(new FileReader(stateFile), GameStateData.class);

            // 2. Calculate R_pre
            double maxRevenue = calculateMaxRevenue(stateData, companyId);

            log.info("--- ANALYSIS TEST COMPLETE ---");
            log.info("State: {}", stateFile.getName());
            log.info("Company: {}", companyId);
            log.info("R_pre (Max Revenue) = {}M", (int) maxRevenue);
            log.info("------------------------------");
            // log.info("Opening visualization window for state {}...", stateFile.getName());

            // We must re-hydrate the state again, as the first one
            // was inside a static method.
            GameStateRestorer restorer = new GameStateRestorer();
            RailsRoot root = restorer.restoreState(stateData);

            // Get the map graph
            NetworkAdapter network = NetworkAdapter.create(root);
            NetworkGraph mapGraph = network.getMapGraph();
            mapGraph.optimizeGraph();

            // // Open the visualization window
            // JFrame mapWindow = mapGraph.visualize("State: " + stateFile.getName() + " | Company: " + companyId);
            // if (mapWindow != null) {
            //     mapWindow.setVisible(true);
            // }
            // log.info("Visualization complete. Check the new window.");
        } catch (Exception e) {
            log.error("Analysis Test FAILED.", e);
        }
    }
}