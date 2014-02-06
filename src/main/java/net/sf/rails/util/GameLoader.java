package net.sf.rails.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameOptionsParser;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.PossibleAction;

import com.google.common.collect.Lists;


/**
 * GameLoader is responsible to load a saved Rails game
 */
public class GameLoader {
    
    private static final Logger log =
            LoggerFactory.getLogger(GameLoader.class);

    // game data
    private final GameIOData gameIOData = new GameIOData();

    // object data
    private ObjectInputStream ois = null;
    private RailsRoot railsRoot = null;
    private Exception exception = null;

    public GameLoader() {};

    // FIXME: Rails 2.0 add undefined attribute to allow
    // deviations from undefined to default values
    private GameOptionsSet.Builder loadDefaultGameOptions(String gameName) {
        log.debug("Load default Game Options of " + gameName);
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
     * @param filepath
     */
    @SuppressWarnings("unchecked")
    public void loadGameData(String filepath) throws Exception {
        log.info("Loading game from file " + filepath);
        String filename = filepath.replaceAll(".*[/\\\\]", "");
        ois = new RailsObjectInputStream(new FileInputStream(
                new File(filepath)));

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
        log.info("Reading Rails " + version  +" saved file "+filename);

        if (object instanceof String) {
            String date = (String)object;
            gameIOData.setDate(date);
            log.info("File was saved at " + date);
            object = ois.readObject();
        }

        // read versionID for serialization compatibility
        long fileVersionID = (Long) object;
        log.debug("Saved versionID="+ fileVersionID+" (object="+object+")");
        gameIOData.setFileVersionID(fileVersionID);
        long saveFileVersionID = GameSaver.saveFileVersionID;

        if (fileVersionID != saveFileVersionID) {
            throw new Exception("Save version " + fileVersionID
                    + " is incompatible with current version "
                    + saveFileVersionID);
        }

        // read name of saved game
        String gameName = (String) ois.readObject();
        log.debug("Saved game="+ gameName);

        
        // read default and saved game options
        GameOptionsSet.Builder gameOptions = loadDefaultGameOptions(gameName);
        Map<String, String> savedOptions = (Map<String, String>) ois.readObject();
        log.debug("Saved game options = " + savedOptions);
        for (GameOption option:gameOptions.getOptions()) {
            String name = option.getName();
            if (savedOptions.containsKey(name)) {
                option.setSelectedValue(savedOptions.get(name));
                log.debug("Assigned option from file " + name);
            } else {
                // FIXME: Rails 2.0 add unassigned value as other default possibility
                log.debug("Missing option in save file " + name + " using default value instead");
            }
        }

        // read playerNames
        List<String> playerNames = (List<String>) ois.readObject();
        log.debug("Player names = " + playerNames);
        GameInfo game = GameInfo.createLegacy(gameName);
        
        gameIOData.setGameData(GameData.create(game, gameOptions, playerNames));
    }
    
    /**
     * Convert the gameData
     * Requires successfull load of gameData
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
        if (gameIOData != null && gameIOData.getActions() != null) {
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
     * @param the filePath used to create the game
     * @return false if exception occurred
     */
    public boolean createFromFile(String filepath)  {

        try {
            // 1st: loadGameData
            loadGameData(filepath);
            
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
     * A subclass of ObjectInputStream that allows to use new package names and still load
     * old game files
     * 
     * See: http://stackoverflow.com/questions/5305473
     */
    
    public static class RailsObjectInputStream extends ObjectInputStream {

        public RailsObjectInputStream(InputStream in) throws IOException {
            super(in);
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
}
