// [REPLACE THE ENTIRE FILE WITH THIS]
package net.sf.rails.util;

import com.google.common.collect.Lists;
import net.sf.rails.common.*;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameOptionsParser;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.TrainCard;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.SplashWindow;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.PossibleAction;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import net.sf.rails.game.ai.JsonStatePojos;
import net.sf.rails.game.ai.snapshot.GameStateData;
import net.sf.rails.game.ai.snapshot.GameStateRestorer;

/**
 * GameLoader is responsible to load a saved Rails game
 */
public class GameLoader {

    private static final Logger log = LoggerFactory.getLogger(GameLoader.class);

    // game data
    private final GameIOData gameIOData = new GameIOData();

    // object data
    private ObjectInputStream ois = null;
    private RailsRoot railsRoot = null;
    private Exception exception = null;

    private int actionCounter = 1;

    public GameLoader() {
        // do nothing
    }

    private int moveLimit = 0;

    public void setMoveLimit(int moveLimit) {
        this.moveLimit = moveLimit;
    }

    public static void loadAndStartGame(File gameFile) {
        loadAndStartGame(gameFile, 0);
    }

    public static void loadAndStartGame(File gameFile, int moveLimit) {
        if (gameFile == null || !gameFile.exists()) {
            System.out.println("file not found");
            System.exit(1);
        }
        
        SplashWindow splashWindow = new SplashWindow(true, gameFile.getName());

        try {
            // Check if this is a JSON state file
            if (gameFile.getName().endsWith(".json")) {
                // log.warn("JSON state file detected. Starting restore from: {}",
                // gameFile.getAbsolutePath());

                // 1. Parse the JSON file
                Gson gson = new GsonBuilder().create();
                JsonReader reader = new JsonReader(new FileReader(gameFile));

                GameStateData loadedState = gson.fromJson(reader, GameStateData.class);

                // 2. Restore the state
                GameStateRestorer restorer = new GameStateRestorer();
                RailsRoot railsRoot = restorer.restoreState(loadedState);

// 3. Wake up the engine to generate legal moves for the restored state
                GameManager gm = railsRoot.getGameManager();
                if (gm.getCurrentRound() != null) {
                    gm.getCurrentRound().setPossibleActions();
                }

                // 4. Start the UI in "Loaded" mode
                GameUIManager gameUIManager = startGameUIManager(railsRoot, true, splashWindow);
                gameUIManager.setGameFile(gameFile);
                gameUIManager.startLoadedGame();
                
                
                splashWindow.finalizeGameInit();
                gameUIManager.notifyOfSplashFinalization();

            } else {
                // --- THIS IS THE CORRECTED .RAILS LOGIC ---
                // This logic is now restored from GameLoader-org.java

                GameLoader loader = new GameLoader();
                loader.setMoveLimit(moveLimit);
                if (!loader.createFromFile(gameFile)) {
                    // If loading fails, throw the stored exception
                    Exception e = loader.getException();
                    // log.error("Game load failed", e);
                    if (e instanceof RailsReplayException) {
                        String title = LocalText.getText("LOAD_INTERRUPTED_TITLE");
                        // The exception message is now just the number.
                        // Parse it to an int and pass it to the formatter.
                        try {
                            log.info("GAMELOADER: Displaying 'Load Interrupted' dialog.");
                            int actionNum = Integer.parseInt(e.getMessage());
                            String message = LocalText.getText("LOAD_INTERRUPTED_MESSAGE", actionNum);
                            JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title,
                                    JOptionPane.ERROR_MESSAGE);
                        } catch (NumberFormatException nfe) {
                            // Fallback for any other replay exception
                            log.error("GAMELOADER: RailsReplayException message was not a number: {}", e.getMessage());
                            String message = LocalText.getText("LOAD_INTERRUPTED_MESSAGE", e.getMessage());
                            JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title,
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        String title = LocalText.getText("LOAD_FAILED_TITLE");
                        String message = LocalText.getText("LOAD_FAILED_MESSAGE", e.getMessage());
                        JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title,
                                JOptionPane.ERROR_MESSAGE);
                    }
                    return; // Exit on failure
                }

                // Get the populated RailsRoot
                RailsRoot railsRoot = loader.getRoot();

                // Use the ORIGINAL 'wasLoaded=true' flag and call...
                GameUIManager gameUIManager = startGameUIManager(railsRoot, true, splashWindow);

                gameUIManager.setGameFile(gameFile);
                // ...the ORIGINAL UI init method!
                gameUIManager.startLoadedGame();

                splashWindow.finalizeGameInit();
                gameUIManager.notifyOfSplashFinalization();
            }
        } catch (Exception e) {
            // log.error("Failed to load game from file!", e); // Generic error
            // TODO: Better error handling
            System.exit(-1);
        }
    }

    public static GameUIManager startGameUIManager(RailsRoot game, boolean wasLoaded, SplashWindow splashWindow) {
        // TODO: Replace that with a Configure method
        GameManager gameManager = game.getGameManager();
        String gameUIManagerClassName = gameManager.getClassName(GuiDef.ClassName.GAME_UI_MANAGER);
        GameUIManager gameUIManager = null;
        try {
            Class<? extends GameUIManager> gameUIManagerClass = Class.forName(gameUIManagerClassName)
                    .asSubclass(GameUIManager.class);
            gameUIManager = gameUIManagerClass.newInstance();
            gameUIManager.init(game, wasLoaded, splashWindow);
        } catch (Exception e) {
            // log.error("Cannot instantiate class {}", gameUIManagerClassName, e);
            System.exit(1);
        }
        return gameUIManager;
    }

    // FIXME: Rails 2.0 add undefined attribute to allow
    // deviations from undefined to default values
    private GameOptionsSet.Builder loadDefaultGameOptions(String gameName) {
        // log.debug("Load default Game Options of {}", gameName);
        GameOptionsSet.Builder loadGameOptions = null;
        try {
            loadGameOptions = GameOptionsParser.load(gameName);
        } catch (ConfigurationException e) {
            // log.error(e.getMessage());
            loadGameOptions = GameOptionsSet.builder();
        }
        return loadGameOptions;
    }

    /**
     * Load the gameData from file
     *
     * @param gameFile
     */
    @SuppressWarnings("unchecked")
    public void loadGameData(File gameFile) throws Exception {
        // log.info("Loading game from file {}", gameFile.getCanonicalPath());
        // FIXME: Removed the filename replacement expression
        // check if this still works
        // String filename = filePath.replaceAll(".*[/\\\\]", "");
        ois = new RailsObjectInputStream(this, new FileInputStream(gameFile));

        Object object = ois.readObject();
        String version;
        if (object instanceof String) {
            // New in 1.0.7: Rails version & save date/time.
            version = (String) object;
            object = ois.readObject();
        } else {
            // Allow for older saved file versions.
            version = "pre-1.0.7";
        }
        gameIOData.setVersion(version);
        // log.debug("Reading Rails {} saved file {}", version, gameFile.getName());

        if (object instanceof String) {
            String date = (String) object;
            gameIOData.setDate(date);
            // log.debug("File was saved at {}", date);
            object = ois.readObject();
        }

        // read versionID for serialization compatibility
        long fileVersionID = (Long) object;
        // log.debug("Saved versionID={} (object={})", fileVersionID, object);
        gameIOData.setFileVersionID(fileVersionID);
        long saveFileVersionID = GameSaver.saveFileVersionID;

        if (fileVersionID != saveFileVersionID) {
            throw new Exception("Save version " + fileVersionID
                    + " is incompatible with current version "
                    + saveFileVersionID);
        }

        // read name of saved game
        String gameName = (String) ois.readObject();
        // log.debug("Saved game: {}", gameName);

        // read default and saved game options
        GameOptionsSet.Builder gameOptions = loadDefaultGameOptions(gameName);
        Map<String, String> savedOptions = (Map<String, String>) ois.readObject();
        // log.debug("Saved game options = {}", savedOptions);

        for (GameOption option : gameOptions.getOptions()) {
            String name = option.getName();
            if (savedOptions.containsKey(name)) {
                option.setSelectedValue(savedOptions.get(name));
                // log.info("Assigned option from game file {}={}", name,
                // option.getSelectedValue());
            } else {
                // FIXME: Rails 2.0 add unassigned value as other default possibility
                // log.debug("Missing option in save file {} using default value instead",
                // name);
            }
        }

        object = ois.readObject();
        if (object instanceof Map) {
            // used to store game file specific configuration options that aren't related to
            // the game itself
            Map<String, String> configOptions = (Map<String, String>) object;
            // log.debug("Saved file configuration = {}", configOptions);

            // iterate over configOptions injecting into ConfigManager as needed
            for (Entry<String, String> config : configOptions.entrySet()) {
                Config.set(config.getKey(), config.getValue());
            }

            // read the next object which would be the list of player names
            object = ois.readObject();
        }

        // read playerNames
        List<String> playerNames = (List<String>) object;
        // log.debug("Player names = {}", playerNames);
        GameInfo game = GameInfo.builder().withName(gameName).build();

        gameIOData.setGameData(GameData.create(game, gameOptions, playerNames));
    }

    /**
     * Convert the gameData
     * Requires successful load of gameData
     */
    @SuppressWarnings("unchecked")
    public void convertGameData() throws Exception {
        // Read game actions into gameData.listOfActions
        // read next object in stream
        Object actionObject = null;
        while (true) { // Single-pass loop.
            try {
                actionObject = ois.readObject();
            } catch (EOFException e) {
                // Allow saved file at start of game (with no actions).
                break;

            }
            if (actionObject instanceof List) {
                // Until Rails 1.3: one List of PossibleAction
                gameIOData.setActions((List<PossibleAction>) actionObject);
            } else if (actionObject instanceof PossibleAction) {
                List<PossibleAction> actions = Lists.newArrayList();
                // Since Rails 1.3.1: separate PossibleActionsObjects
                int n = 0;
                while (actionObject instanceof PossibleAction) {
                    actions.add((PossibleAction) actionObject);
                    // log.debug("Reading action {}: {}", ++n,
                    // actionObject.getClass().getSimpleName());
                    try {
                        actionObject = ois.readObject();
                    } catch (EOFException e) {
                        break;
                    }
                }
                gameIOData.setActions(actions);
            }
            break;
        }
        /**
         * todo: the code below is far from perfect, but robust
         */

        // at the end of file user comments are added as SortedMap
        if (actionObject instanceof SortedMap) {
            // FIXME (Rails2.0): Do something with userComments
            // gameData.userComments = (SortedMap<Integer, String>) actionObject;
            // log.debug("file load: found user comments");
        } else {
            try {
                Object object = ois.readObject();
                if (object instanceof SortedMap) {
                    // FIXME (Rails2.0): Do something with userComments
                    // gameData.userComments = (SortedMap<Integer, String>) actionObject;
                    // log.debug("file load: found user comments");
                }
            } catch (IOException e) {
                // continue without comments, if any IOException occurs
                // sometimes not only the EOF Exception is raised
                // but also the java.io.StreamCorruptedException: invalid type code
            }
        }
        ois.close();
        ois = null;
    }

    // // [REPLACE the entire 'replayGame' method with this]
    // public boolean replayGame() {

    // int actionCount;
    // GameManager gameManager = railsRoot.getGameManager();
    // log.info("--- GAMELOADER: REPLAYGAME START ---");
    // if (moveLimit > 0) {
    // log.info("GAMELOADER: Replay limited to {} actions.", moveLimit);
    // }

    // gameManager.setReloading(true);

    // // CRITICAL: We must reset transient state for ALL round types that
    // // have a reset method, otherwise they will be null on reload.
    // if (gameManager.getCurrentRound() instanceof
    // net.sf.rails.game.OperatingRound) {
    // log.info("GAMELOADER: Detected OperatingRound. Calling
    // resetTransientStateOnLoad().");
    // ((net.sf.rails.game.OperatingRound)
    // gameManager.getCurrentRound()).resetTransientStateOnLoad();
    // } else if (gameManager.getCurrentRound() instanceof
    // net.sf.rails.game.financial.StockRound) {
    // log.info("GAMELOADER: Detected StockRound. Calling
    // resetTransientStateOnLoad().");
    // ((net.sf.rails.game.financial.StockRound)
    // gameManager.getCurrentRound()).resetTransientStateOnLoad();
    // } else if (gameManager.getCurrentRound() != null) {
    // log.warn("GAMELOADER: Loaded round is of type '{}', no reset method
    // specified.", gameManager.getCurrentRound().getClass().getName());
    // } else {
    // log.error("GAMELOADER: FATAL: getCurrentRound() is null on reload.");
    // }

    // if (gameIOData.getActions() != null) {
    // // set possible actions for first action
    // gameManager.getCurrentRound().setPossibleActions();

    // int processedCount = 0; // Initialize counter
    // for (PossibleAction action : gameIOData.getActions()) {
    // if (moveLimit > 0 && processedCount >= moveLimit) {
    // log.info("GAMELOADER: Replay limit reached ({}). Stopping replay.",
    // moveLimit);
    // break;
    // }
    // processedCount++; // Increment counter

    // actionCount = increaseActionCounter();

    // // RE-APPLYING the brute-force try-catch block to the GameLoader.
    // // We must ignore errors in BOTH the loader and the manager.
    // try {
    // // GameManager.processOnReload() is also patched to always return 'true'
    // if (!gameManager.processOnReload(action)) {
    // // This block should theoretically not be reached if GameManager is patched,
    // // but we keep it for safety.
    // log.warn("GAMELOADER [BruteForce]: processOnReload returned false for action
    // {}. IGNORING.", actionCount);
    // }
    // } catch (Exception e) {
    // // This handles any unexpected crash during the processOnReload call
    // log.error("GAMELOADER [BruteForce]: CRASH during replay of action {}. Action:
    // {}. IGNORING. Error: {} -> {}",
    // actionCount,
    // action.getClass().getSimpleName(),
    // e.getClass().getSimpleName(),
    // e.getMessage());
    // }
    // }
    // }

    // gameManager.setReloading(false);

    // // FIXME (Rails2.0): CommentItems have to be replaced
    // // ReportBuffer.setCommentItems(gameData.userComments);

    // // callback to GameManager
    // gameManager.finishLoading();
    // // return true if no exception occurred
    // return (exception == null);
    // }

    public boolean replayGame() {

        int actionCount;
        GameManager gameManager = railsRoot.getGameManager();
        log.info("--- GAMELOADER: REPLAYGAME START (STRICT) ---");

        // Note: Reloading=true disables some UI updates, but we need strict logic
        // checking.
        gameManager.setReloading(true);

        if (gameManager.getCurrentRound() instanceof net.sf.rails.game.OperatingRound) {
            ((net.sf.rails.game.OperatingRound) gameManager.getCurrentRound()).resetTransientStateOnLoad();
        } else if (gameManager.getCurrentRound() instanceof net.sf.rails.game.financial.StockRound) {
            ((net.sf.rails.game.financial.StockRound) gameManager.getCurrentRound()).resetTransientStateOnLoad();
        }

        if (gameIOData.getActions() != null) {
            gameManager.getCurrentRound().setPossibleActions();

            int processedCount = 0;
            boolean hasLimit = (moveLimit != 0);
            int targetLimit = moveLimit;
            if (moveLimit < 0) {
                targetLimit = gameIOData.getActions().size() + moveLimit;
            }

            for (PossibleAction action : gameIOData.getActions()) {

                if (hasLimit && processedCount >= targetLimit) {
                    log.info("GAMELOADER: Replay stopped at limit: " + targetLimit);
                    break;
                }
                processedCount++;
                actionCount = increaseActionCounter();

                // DIRECT CALL: No try-catch.
                // If processOnReload crashes, the exception escapes immediately.
                // This stops the loop and fails the RegressionTest instantly.
                gameManager.processOnReload(action);
            }
        }

        gameManager.setReloading(false);
        gameManager.finishLoading();
        return true;
    }

    public RailsRoot getRoot() {
        return railsRoot;
    }

    public Exception getException() {
        return exception;
    }

    public List<PossibleAction> getActions() {
        return gameIOData.getActions();
    }

    public String getGameDataAsText() {
        return gameIOData.metaDataAsText() + gameIOData.gameOptionsAsText() + gameIOData.playerNamesAsText();
    }

    /**
     * @param gameFile
     * @return false if exception occurred
     */
    public boolean createFromFile(File gameFile) {
        try {
            // 1st: loadGameData
            loadGameData(gameFile);

            // 2nd: create game
            railsRoot = RailsRoot.create(gameIOData.getGameData());

            // 3rd: convert game data (retrieve actions)
            convertGameData();

            // 4th: start game
            railsRoot.start();

        } catch (Exception e) {
            // log.debug("Exception during createFromFile in gameLoader ", e);
            exception = e;
            return false;
        }
        // 5th: replay game
        return replayGame();
    }

    /**
     * A subclass of ObjectInputStream for Rails
     * <p>
     * 1. Allows to add context information (here the railsRoot)
     * Took the idea from
     * http://www.cordinc.com/blog/2011/05/injecting-context-in-java-seri.html
     * <p>
     * 2. Should allow to use new package names and still load old game files
     * See: http://stackoverflow.com/questions/5305473
     * However this approach did not work. I did not investigate it further so far.
     * See code below
     */
    public static class RailsObjectInputStream extends ObjectInputStream {

        private final GameLoader loader;

        public RailsObjectInputStream(GameLoader loader, InputStream in) throws IOException {
            super(in);
            this.loader = loader;
        }

        public RailsRoot getRoot() {
            return loader.getRoot();
        }

        // @Override
        // protected java.io.ObjectStreamClass readClassDescriptor()
        // throws IOException, ClassNotFoundException {
        // ObjectStreamClass desc = super.readClassDescriptor();
        // String className = desc.getName();
        // log.debug("Found class = " + className);
        // if (className.startsWith("rails.")) {
        // String newClassName = className.replace("rails.", "net.sf.rails.");
        // log.debug("Replaced class " + className + " by new class " + newClassName);
        // return ObjectStreamClass.lookup(Class.forName(newClassName));
        // } else {
        // return desc;
        // }
        // }
    }

    public boolean reloadGameFromFile(RailsRoot root, File file) {
        try {
            railsRoot = root;
            // 1st: loadGameData
            loadGameData(file);

            // 2nd: convert game data (retrieve actions)
            convertGameData();

        } catch (Exception e) {
            // log.debug("Exception during createFromFile in gameLoader ", e);
            exception = e;
            return false;
        }
        return true;
    }

    public int getActionCounter() {
        return actionCounter;
    }

    /**
     * A standalone method for the batch-logging tool.
     * This loads a .rails file, replays it with the brute-force patch,
     * and relies on the GameManager hook to save all state snapshots.
     *
     * @param gameFile        The input .rails file.
     * @param outputDirectory The destination directory for state_XXXX.json files.
     */
    public static void createAndLogFromFile(File gameFile, File outputDirectory) {
        log.info("--- Starting Log Generation for: {} ---", gameFile.getName());
        log.info("--- Outputting states to: {} ---", outputDirectory.getAbsolutePath());

        GameLoader loader = new GameLoader();
        RailsRoot railsRoot = null;

        try {
            // 1. Create the output directory
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // 2. Load Game Data
            loader.loadGameData(gameFile);

            // 3. Create Game Instance
            railsRoot = RailsRoot.create(loader.gameIOData.getGameData());
            loader.railsRoot = railsRoot; // Set instance variable for replayGame()

            // 4. Convert Actions
            loader.convertGameData();

            // 5. Start Game (initializes state)
            railsRoot.start();

            // 6. Inject Logger Hook
            GameManager gameManager = railsRoot.getGameManager();
            gameManager.setLogOutputDirectory(outputDirectory); // Tell the GM where to save

            // 7. Replay Game (uses the brute-force replayGame method)
            boolean replayFinished = loader.replayGame(); // This will log warnings but not stop
            log.info("Replay finished for {}. Success (did not hard-crash): {}", gameFile.getName(), replayFinished);
            log.info("--- Log generation complete for: {} ---", gameFile.getName());

        } catch (Exception e) {
            log.error("FATAL ERROR during log generation for {}. Skipping file.", gameFile.getName(), e);
            if (loader.ois != null) {
                try {
                    loader.ois.close();
                } catch (IOException ioException) {
                    // ignore
                }
            }
        }
    }

    // ... (lines of unchanged context code) ...
    public int increaseActionCounter() {
        return ++actionCounter;
    }

    /**
     * Loads a .rails file and returns the list of executed actions
     * without starting the game engine or performing replay.
     * This is used by the GameHistoryMerger tool.
     *
     * @param gameFile The input .rails file.
     * @return The list of PossibleAction objects recorded in the save file.
     * @throws Exception if load fails (e.g., file not found or incompatible
     *                   version).
     */
    public static List<PossibleAction> loadActionsFromFile(File gameFile) throws Exception {
        GameLoader loader = new GameLoader();

        // 1. Load the meta data (version, options, player names)
        loader.loadGameData(gameFile);

        // CRITICAL: We must create the RailsRoot object *before* converting actions
        // so that the PossibleAction objects can find the necessary context (like
        // PlayerManager)
        // during deserialization (the 'readObject' method).
        // Note: We do *not* call railsRoot.start() as that would begin game flow.
        RailsRoot railsRoot = net.sf.rails.game.RailsRoot.create(loader.gameIOData.getGameData());
        loader.railsRoot = railsRoot;

        // 2. Load the action objects from the end of the file
        loader.convertGameData();

        return loader.getActions();
    }
}
