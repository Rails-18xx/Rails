package net.sf.rails.game.ai.playground; // Or your chosen package

import net.sf.rails.common.*;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameInfoParser; // Needed to get GameInfo
import net.sf.rails.common.parser.GameOptionsParser; // Needed to load options
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.SplashWindow;
import net.sf.rails.util.GameLoader; // To potentially use startGameUIManager

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet; // For GameInfo list
import javax.swing.JOptionPane; // For error messages
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoGUIRunner {

    private static final Logger log = LoggerFactory.getLogger(AutoGUIRunner.class);
    private static final int MAX_ACTIONS = 2000;
    private static final long AI_MOVE_DELAY_MS = 200;

    public static void main(String[] args) {
        log.info("Starting AutoGUIRunner...");
        RailsRoot railsRoot = null;
        GameUIManager gameUIManager = null;
        String gameName = "1835"; // *** HARDCODE the game ***
        List<String> playerNames = new ArrayList<>(Arrays.asList("AI_1", "AI_2", "AI_3","AI_4,","AI_5")); // *** HARDCODE players ***


        try {
            // --- 1. Initialize Configuration (like RunGame.main) ---
            ConfigManager.initConfiguration(false);
            log.info("Configuration initialized.");

            // --- 2. Load GameInfo for the chosen game ---
            log.info("Loading GameInfo for: {}", gameName);
            GameInfoParser gip = new GameInfoParser();
            SortedSet<GameInfo> gameList = gip.processGameList(); // Load all available games
            GameInfo selectedGame = GameInfo.findGame(gameList, gameName);
            if (selectedGame == null) {
                log.error("Game '{}' not found in available games list.", gameName);
                System.exit(1);
            }
            log.info("GameInfo loaded.");

            // --- 3. Load Default Game Options AND SET RouteAlgorithm ---
            log.info("Loading default GameOptionsSet builder for: {}", gameName);
            GameOptionsSet.Builder optionsBuilder;
            GameOptionsSet gameOptions; // Final options set

            try {
                optionsBuilder = GameOptionsParser.load(gameName); // Load defaults into builder
                log.info("GameOptionsSet builder loaded from parser.");

                

            } catch (ConfigurationException e) {
                log.error("Failed to load/process default GameOptionsSet for {}. Cannot proceed reliably.", gameName,
                        e);
                System.exit(1);
                return; // Added return
            }
            //log.debug("Using options map: {}", gameOptions.getOptions());

            // --- 4. Create GameData ---
            log.info("Creating GameData...");
            GameData gameData = GameData.create(selectedGame, optionsBuilder, playerNames);
            log.info("GameData created.");

            // --- 5. Create RailsRoot ---
            log.info("Creating RailsRoot...");
            railsRoot = RailsRoot.create(gameData);
            if (railsRoot == null) {
                /* ... error handling ... */ System.exit(1);
            }
            log.info("RailsRoot created.");

            // --- 6. Start Game Logic (sets initial round) ---
            log.info("Starting game logic (railsRoot.start())...");
            String startError = railsRoot.start(); // This calls gameManager.startGame()
            if (startError != null) {
                log.error("Error starting game logic: {}", startError);
                // Optionally show error in a simple dialog even in auto mode
                JOptionPane.showMessageDialog(null, startError, "Game Start Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            log.info("Game logic started successfully.");

            // --- 7. Create SplashWindow (needed for UI init calls) ---
            SplashWindow splash = new SplashWindow(true, "Auto Running..."); // Show splash briefly

            // --- 8. Create and Initialize UI Manager ---
            // Replicating GameLoader.startGameUIManager more closely
            log.info("Creating and initializing GameUIManager...");
            String uiManagerClassName = railsRoot.getGameManager().getClassName(GuiDef.ClassName.GAME_UI_MANAGER);
            gameUIManager = (GameUIManager) Class.forName(uiManagerClassName).getDeclaredConstructor().newInstance();

            splash.notifyOfStep(SplashWindow.STEP_INIT_UI);
            gameUIManager.init(railsRoot, false, splash); // false = new game
            gameUIManager.gameUIInit(true); // true = new game (calls updateUI internally)
            log.info("GameUIManager initialized.");

            // --- 9. Finalize Splash/Visibility ---
            splash.finalizeGameInit(); // This should make windows visible
            gameUIManager.notifyOfSplashFinalization();
            log.info("Splash finalized, UI should be visible.");

            // --- 10. Wait 5 Seconds ---
            log.info("Waiting 2 seconds before starting AI...");
            Thread.sleep(2000);
            log.info("Wait finished. Handing off to AI runner thread.");

        } catch (Exception e) {
            log.error("Critical error during game initialization: {}", e.getMessage(), e);
            System.exit(1);
        }

        // --- 11. Start AI Runner Thread ---
        final GameUIManager finalUIManager = gameUIManager;
        final GameManager finalGameManager = railsRoot.getGameManager();

        Thread aiThread = new Thread(() -> runAILoop(finalUIManager, finalGameManager));
        aiThread.setName("AI-AutoRunner");
        aiThread.start();

        log.info("Main thread finished setup. AI thread running.");
    }

    // runAILoop method remains the same
    private static void runAILoop(GameUIManager uiManager, GameManager gameManager) {
        log.info("AI Loop Started. Target actions: {}", MAX_ACTIONS);
        try {
            // Give the UI a moment to appear and settle
            Thread.sleep(2000); // 2 seconds initial delay

            while (true) {
                int currentActionCount = gameManager.getCurrentActionCount();
                boolean gameOver = gameManager.isGameOver();

                if (gameOver) {
                    log.info("AI Loop: Game is Over (Action {}). Stopping AI.", currentActionCount);
                    break;
                }
                if (currentActionCount >= MAX_ACTIONS) {
                    log.info("AI Loop: Reached target action count ({}). Stopping AI.", currentActionCount);
                    break;
                }

                SwingUtilities.invokeLater(() -> {
                    try {
                        log.debug("AI Loop (EDT): Triggering performAIMove(). Current Action Count: {}",
                                gameManager.getCurrentActionCount());
                        uiManager.performAIMove();
                    } catch (Exception e) {
                        log.error("AI Loop (EDT): Exception during performAIMove: {}", e.getMessage(), e);
                    }
                });

                Thread.sleep(AI_MOVE_DELAY_MS);

            } // End while loop

        } catch (InterruptedException e) {
            log.error("autorun error {}",e.toString());
            /* ... */ } catch (Exception e) {
            /* ... */ }
        log.info("AI Loop Finished.");
    }
}