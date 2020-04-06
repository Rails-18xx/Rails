package net.sf.rails.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.sf.rails.common.Config;
import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameOptionsParser;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.SplashWindow;
import rails.game.action.PossibleAction;


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

    public GameLoader() {};

    public static void loadAndStartGame(File gameFile) {
        SplashWindow splashWindow = new SplashWindow(true, gameFile.getAbsolutePath());
        splashWindow.notifyOfStep(SplashWindow.STEP_LOAD_GAME);

        // use gameLoader instance to start game
        GameLoader gameLoader = new GameLoader();
        if (!gameLoader.createFromFile(gameFile)) {
            Exception e = gameLoader.getException();
            log.error("Game load failed", e);
            if (e instanceof RailsReplayException) {
                String title = LocalText.getText("LOAD_INTERRUPTED_TITLE");
                String message = LocalText.getText("LOAD_INTERRUPTED_MESSAGE", e.getMessage());
                JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
            } else {
                String title = LocalText.getText("LOAD_FAILED_TITLE");
                String message = LocalText.getText("LOAD_FAILED_MESSAGE", e.getMessage());
                JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
                // in this case start of game cannot continued
                return;
            }
        }

        GameUIManager gameUIManager = startGameUIManager(gameLoader.getRoot(), true, splashWindow);

        gameUIManager.setGameFile(gameFile);

        gameUIManager.startLoadedGame();
        gameUIManager.notifyOfSplashFinalization();
        splashWindow.finalizeGameInit();
    }

    public static GameUIManager startGameUIManager(RailsRoot game, boolean wasLoaded, SplashWindow splashWindow) {
        // TODO: Replace that with a Configure method
        GameManager gameManager = game.getGameManager();
        String gameUIManagerClassName = gameManager.getClassName(GuiDef.ClassName.GAME_UI_MANAGER);
        GameUIManager gameUIManager = null;
        try {
            Class<? extends GameUIManager> gameUIManagerClass = Class.forName(gameUIManagerClassName).asSubclass(GameUIManager.class);
            gameUIManager = gameUIManagerClass.newInstance();
            gameUIManager.init(game, wasLoaded, splashWindow);
        } catch (Exception e) {
            log.error("Cannot instantiate class {}", gameUIManagerClassName, e);
            System.exit(1);
        }
        return gameUIManager;
    }

    // FIXME: Rails 2.0 add undefined attribute to allow
    // deviations from undefined to default values
    private GameOptionsSet.Builder loadDefaultGameOptions(String gameName) {
        log.debug("Load default Game Options of {}", gameName);
        GameOptionsSet.Builder loadGameOptions = null;
        try {
            loadGameOptions = GameOptionsParser.load(gameName);
        } catch (ConfigurationException e) {
            log.error(e.getMessage());
            loadGameOptions = GameOptionsSet.builder();
        }
        return loadGameOptions;
    }

    /**
     * Load the gameData from file
     * @param filePath
     */

    @SuppressWarnings("unchecked")
    public void loadGameData(File gameFile) throws Exception {
        log.info("Loading game from file {}", gameFile.getCanonicalPath());
        // FIXME: Removed the filename replacement expression
        // check if this still works
        // String filename = filePath.replaceAll(".*[/\\\\]", "");
        ois = new RailsObjectInputStream(this, new FileInputStream(gameFile));

        Object object = ois.readObject();
        String version;
        if (object instanceof String) {
            // New in 1.0.7: Rails version & save date/time.
            version = (String)object;
            object = ois.readObject();
        } else {
            // Allow for older saved file versions.
            version = "pre-1.0.7";
        }
        gameIOData.setVersion(version);
        log.info("Reading Rails {} saved file {}", version, gameFile.getName());

        if (object instanceof String) {
            String date = (String)object;
            gameIOData.setDate(date);
            log.info("File was saved at {}", date);
            object = ois.readObject();
        }

        // read versionID for serialization compatibility
        long fileVersionID = (Long) object;
        log.debug("Saved versionID={} (object={})", fileVersionID, object);
        gameIOData.setFileVersionID(fileVersionID);
        long saveFileVersionID = GameSaver.saveFileVersionID;

        if (fileVersionID != saveFileVersionID) {
            throw new Exception("Save version " + fileVersionID
                    + " is incompatible with current version "
                    + saveFileVersionID);
        }

        // read name of saved game
        String gameName = (String) ois.readObject();
        log.debug("Saved game={}", gameName);

        // read default and saved game options
        GameOptionsSet.Builder gameOptions = loadDefaultGameOptions(gameName);
        Map<String, String> savedOptions = (Map<String, String>) ois.readObject();
        log.debug("Saved game options = {}", savedOptions);
        for (GameOption option:gameOptions.getOptions()) {
            String name = option.getName();
            if (savedOptions.containsKey(name)) {
                option.setSelectedValue(savedOptions.get(name));
                log.debug("Assigned option from game file {}", name);
            } else {
                // FIXME: Rails 2.0 add unassigned value as other default possibility
                log.debug("Missing option in save file {} using default value instead", name);
            }
        }

        object = ois.readObject();
        if ( object instanceof Map ) {
            // used to store game file specific configuration options that aren't related to the game itself
            Map<String, String> configOptions = (Map<String, String>) object;
            log.debug("Saved file configuration = {}", configOptions);

            // iterate over configOptions injecting into ConfigManager as needed
            for ( Entry<String, String> config : configOptions.entrySet() ) {
                Config.set(config.getKey(), config.getValue());
            }

            // read the next object which would be the list of player names
            object = ois.readObject();
        }

        // read playerNames
        List<String> playerNames = (List<String>) object;
        log.debug("Player names = {}", playerNames);
        GameInfo game = GameInfo.createLegacy(gameName);

        gameIOData.setGameData(GameData.create(game, gameOptions, playerNames));
    }

    /**
     * Convert the gameData
     * Requires successful load of gameData
     */
    @SuppressWarnings("unchecked")
    public void convertGameData() throws Exception  {
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
                while (actionObject instanceof PossibleAction) {
                    actions.add((PossibleAction)actionObject);
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
      todo: the code below is far from perfect, but robust
         */

        // at the end of file user comments are added as SortedMap
        if (actionObject instanceof SortedMap) {
            // FIXME (Rails2.0): Do something with userComments
            //gameData.userComments = (SortedMap<Integer, String>) actionObject;
            log.debug("file load: found user comments");
        } else {
            try {
                Object object = ois.readObject();
                if (object instanceof SortedMap) {
                    // FIXME (Rails2.0): Do something with userComments
                    // gameData.userComments = (SortedMap<Integer, String>) actionObject;
                    log.debug("file load: found user comments");
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

    /**
     * @return false if exception occurred
     */
    public boolean replayGame() {

        GameManager gameManager = railsRoot.getGameManager();
        log.debug("Starting to execute loaded actions");
        gameManager.setReloading(true);

        int count = -1;
        if ( gameIOData.getActions() != null ) {
            // set possible actions for first action
            gameManager.getCurrentRound().setPossibleActions();
            for (PossibleAction action : gameIOData.getActions()) {
                count++;
                if (!gameManager.processOnReload(action)) {
                    log.warn("Replay of game interrupted");
                    String message = LocalText.getText("LoadInterrupted", count);
                    exception = new RailsReplayException(message);
                    break;
                }
            }
        }

        gameManager.setReloading(false);

        // FIXME (Rails2.0): CommentItems have to be replaced
        // ReportBuffer.setCommentItems(gameData.userComments);

        // callback to GameManager
        gameManager.finishLoading();
        // return true if no exception occurred
        return (exception == null);
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
    public boolean createFromFile(File gameFile)  {
        try {
            // 1st: loadGameData
            loadGameData(gameFile);

            // 2nd: create game
            railsRoot = RailsRoot.create(gameIOData.getGameData());

            // 3rd: convert game data (retrieve actions)
            convertGameData();

            // 4tgh: start game
            railsRoot.start();

        } catch (Exception e) {
            log.debug("Exception during createFromFile in gameLoader ", e);
            exception = e;
            return false;
        }
        // 5th: replay game
        return replayGame();
    }

    /**
     * A subclass of ObjectInputStream for Rails
     *
     * 1. Allows to add context information (here the railsRoot)
     * Took the idea from http://www.cordinc.com/blog/2011/05/injecting-context-in-java-seri.html
     *
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

//        @Override
//        protected java.io.ObjectStreamClass readClassDescriptor()
//                throws IOException, ClassNotFoundException {
//            ObjectStreamClass desc = super.readClassDescriptor();
//            String className = desc.getName();
//            log.debug("Found class = " + className);
//            if (className.startsWith("rails.")) {
//                String newClassName = className.replace("rails.", "net.sf.rails.");
//                log.debug("Replaced class " + className + " by new class " + newClassName);
//                return ObjectStreamClass.lookup(Class.forName(newClassName));
//            } else {
//                return desc;
//            }
//        }
    }

    public boolean reloadGameFromFile(File file) {

        try {
            // 1st: loadGameData
            loadGameData(file);

            railsRoot = RailsRoot.getInstance();
           // 2nd: convert game data (retrieve actions)
            convertGameData();


        } catch (Exception e) {
            log.debug("Exception during createFromFile in gameLoader ", e);
            exception = e;
            return false;
        }
        return true;

    }
}
