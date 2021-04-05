package net.sf.rails.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameOptionsParser;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.SplashWindow;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.PossibleAction;

import javax.swing.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SortedMap;

/**
 * GameLoader is responsible to load a saved Rails game
 */
public class XmlGameLoader {

    private static final Logger log = LoggerFactory.getLogger(XmlGameLoader.class);

    private GameIOData gameIOData;

    // object data
    private RailsRoot railsRoot = null;
    private Exception exception = null;

    public XmlGameLoader() {
        // do nothing
    }

    public static void loadAndStartGame(File gameFile) {
        SplashWindow splashWindow = new SplashWindow(true, gameFile.getAbsolutePath());
        splashWindow.notifyOfStep(SplashWindow.STEP_LOAD_GAME);

        // check to see if we were passed in a last_rails file
        if (GameUIManager.DEFAULT_SAVE_POLLING_EXTENSION.equals(StringUtils.substringAfterLast(gameFile.getName(), "."))) {
            // read the filename from the last rails file
            log.debug("loading current game file from last_rails {}", gameFile);
            try {
                String gameFileStr = FileUtils.readFileToString(gameFile, StandardCharsets.ISO_8859_1).trim();
                gameFile = new File(gameFile.getParentFile(), gameFileStr);
            } catch (IOException e) {
                log.warn("unable to load {}", gameFile);
                return;
            }
        }

        // use gameLoader instance to start game
        XmlGameLoader gameLoader = new XmlGameLoader();
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

        splashWindow.finalizeGameInit();
        gameUIManager.notifyOfSplashFinalization();
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
     *
     * @param gameFile
     */
    @SuppressWarnings("unchecked")
    public void loadGameData(File gameFile) throws Exception {
        log.info("Loading game from file {}", gameFile.getCanonicalPath());

        final XStream xStream = new XStream();

        this.gameIOData = (GameIOData) xStream.fromXML(gameFile);
    }

    /**
     * Convert the gameData
     * Requires successful load of gameData
     */
    public void convertGameData() {
        for (PossibleAction action : gameIOData.getActions()) {
            action.applyRailsRoot(railsRoot);
        }
    }

    /**
     * @return false if exception occurred
     */
    public boolean replayGame() {
        GameManager gameManager = railsRoot.getGameManager();
        log.debug("Starting to execute loaded actions");
        gameManager.setReloading(true);

        int count = 0;
        if (gameIOData.getActions() != null) {
            // set possible actions for first action
            gameManager.getCurrentRound().setPossibleActions();
            for (PossibleAction action : gameIOData.getActions()) {
                count++;
                if (!gameManager.processOnReload(action)) {
                    log.warn("Replay of game interrupted at action " + count);
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
    public boolean createFromFile(File gameFile) {
        try {
            // 1st: loadGameData
            loadGameData(gameFile);

            // 2nd: create game
            railsRoot = RailsRoot.create(gameIOData.getGameData());

            // 3rd: prepare game
            convertGameData();

            // 4th: start game
            railsRoot.start();

            // 5th: replay game
            return replayGame();
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("Exception during createFromFile in gameLoader ", e);
            exception = e;
            return false;
        }
    }

    public boolean reloadGameFromFile(RailsRoot root, File file) {
        try {
            railsRoot = root;

            // 1st: loadGameData
            loadGameData(file);

            // 2nd: prepare game
            convertGameData();

            return true;
        } catch (Exception e) {
            log.debug("Exception during createFromFile in gameLoader ", e);
            exception = e;
            return false;
        }
    }
}
