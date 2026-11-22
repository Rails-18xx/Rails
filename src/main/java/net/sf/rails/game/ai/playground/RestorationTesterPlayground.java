package net.sf.rails.game.ai.playground;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import net.sf.rails.game.RailsRoot;
// FIX: Import the correct, new POJO data class
import net.sf.rails.game.ai.snapshot.GameStateData; 
import net.sf.rails.game.ai.snapshot.GameStateRestorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A simple main() class to test the GameStateRestorer.
 * * Takes one argument: the full path to a state_NNN.json file.
 */
public class RestorationTesterPlayground {

    private static final Logger log = LoggerFactory.getLogger(RestorationTesterPlayground.class);

    private RailsRoot liveGameRoot;
    private GameStateData loadedState; // FIX: Use new POJO type
    private GameStateRestorer restorer;

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            log.error("Usage: RestorationTesterPlayground <path-to-state.json>");
            return;
        }

        String filePath = args[0];
        log.info("--- Restoration Tester Playground ---");
        log.info("Attempting to load state from: {}", filePath);

        RestorationTesterPlayground app = new RestorationTesterPlayground();
        app.run(filePath);
    }

    public RestorationTesterPlayground() {
        this.restorer = new GameStateRestorer();
    }

// In RestorationTesterPlayground.java

    public void run(String filePath) {
        try {
            // 1. Load and Parse the JSON file
            log.info("Parsing JSON file...");
            Gson gson = new GsonBuilder().create();
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("File not found: {}", file.getAbsolutePath());
                return;
            }
            JsonReader reader = new JsonReader(new FileReader(file));

            // Parse into the new GameStateData.class
            loadedState = gson.fromJson(reader, GameStateData.class);

            if (loadedState == null) {
                log.error("Failed to parse JSON, loadedState is null.");
                return;
            }
            log.info("JSON Parsed successfully. Root object: {}", loadedState.getClass().getName());
            log.info("Action Count: {}", loadedState.gameManager.absoluteActionCounter);
            log.info("Current Phase: {}", loadedState.phase.currentPhaseName);

            // 2. Attempt to re-hydrate the state
            log.info("Calling GameStateRestorer.restoreState()...");
            
            // This line is now correct, as loadedState is GameStateData
            liveGameRoot = restorer.restoreState(loadedState);

            log.info("--- SUCCESS! ---");
            log.info("State re-hydration complete.");
            log.info("Restored RailsRoot: {}", liveGameRoot);
            log.info("Restored Current Player: {}", liveGameRoot.getGameManager().getCurrentPlayer().getId());
            log.info("Restored Bank Cash: {}", liveGameRoot.getBank().getCash());

            // FIX: Use the correct method to get train count
            log.info("Restored M1 Trains: {}", liveGameRoot.getCompanyManager().getPublicCompany("M1").getPortfolioModel().getTrainsModel().getPortfolio().items().size());
            // FIX: Use the correct method to get train count
            log.info("Restored BY Trains: {}", liveGameRoot.getCompanyManager().getPublicCompany("BY").getPortfolioModel().getTrainsModel().getPortfolio().items().size());

        } catch (Exception e) {
            log.error("Failed during restoration test:", e);
        }
    }


}