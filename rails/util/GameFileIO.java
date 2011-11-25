package rails.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.game.*;
import rails.game.action.PossibleAction;

/**
 * Functions to load and save games from/to file
 * 
 * @author freystef
 *
 */
public class GameFileIO {

    protected static Logger log =
        Logger.getLogger(Game.class.getPackage().getName());

    private GameData gameData = new GameData();

    // fields for data load
    private ObjectInputStream ois = null;
    private Game loadedGame = null;
    private boolean dataLoadDone = false;
    private boolean initialized = false;

    // fields for data save
    private boolean initSave = false;

    public String getGameDataAsText() {
        return gameData.metaDataAsText() + gameData.gameOptionsAsText() + gameData.playerNamesAsText();
    }

    public Game getGame() {
        return loadedGame;
    }

    public List<PossibleAction> getActions() {
        return gameData.actions;
    }

    public void setActions(List<PossibleAction> actions) {
        gameData.actions = actions;
    }

    public SortedMap<Integer, String> getComments() {
        return gameData.userComments;
    }

    public void setComments(SortedMap<Integer, String> comments) {
        gameData.userComments = comments;
    }

    @SuppressWarnings("unchecked")
    public void loadGameData(String filepath) {

        dataLoadDone = true;

        log.info("Loading game from file " + filepath);
        String filename = filepath.replaceAll(".*[/\\\\]", "");

        try {
            ois = new ObjectInputStream(new FileInputStream(
                    new File(filepath)));

            Object object = ois.readObject();
            if (object instanceof String) {
                // New in 1.0.7: Rails version & save date/time.
                gameData.meta.version = (String)object;
                object = ois.readObject();
            } else {
                // Allow for older saved file versions.
                gameData.meta.version = "pre-1.0.7";
            }

            log.info("Reading Rails " + gameData.meta.version  +" saved file "+filename);

            if (object instanceof String) {
                gameData.meta.date = (String)object;
                log.info("File was saved at "+ gameData.meta.date);
                object = ois.readObject();
            }

            // read versionID for serialization compatibility
            gameData.meta.fileVersionID = (Long) object;
            log.debug("Saved versionID="+gameData.meta.fileVersionID+" (object="+object+")");
            long GMsaveFileVersionID = GameManager.saveFileVersionID;

            if (gameData.meta.fileVersionID != GMsaveFileVersionID) {
                throw new Exception("Save version " + gameData.meta.fileVersionID
                        + " is incompatible with current version "
                        + GMsaveFileVersionID);
            }

            // read name of saved game
            gameData.meta.gameName = (String) ois.readObject();
            log.debug("Saved game="+ gameData.meta.gameName);

            // read selected game options and player names
            gameData.gameOptions = (Map<String, String>) ois.readObject();
            log.debug("Selected game options = " + gameData.gameOptions);
            gameData.playerNames = (List<String>) ois.readObject();
            log.debug("Player names = " + gameData.playerNames);

        } catch (Exception e) {
            dataLoadDone = false;
            log.fatal("Load failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
        }
    }

    public Game initGame() throws ConfigurationException {

        // check if initial load was done
        if (!dataLoadDone) {
            throw new ConfigurationException("No game was loaded");
        }

        // initialize loadedGame
        loadedGame = new Game(gameData.meta.gameName, gameData.playerNames, gameData.gameOptions);

        if (!loadedGame.setup()) {
            loadedGame = null;
            throw new ConfigurationException("Error in setting up " + gameData.meta.gameName);
        }

        String startError = loadedGame.start();
        if (startError != null) {
            DisplayBuffer.add(startError);
        }

        return loadedGame;
    }


    @SuppressWarnings("unchecked")
    public boolean loadActionsAndComments() throws ConfigurationException  {
        if (!dataLoadDone) {
            throw new ConfigurationException("No game was loaded");
        }
        // Read game actions into gameData.listOfActions
        try {
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
                    gameData.actions = (List<PossibleAction>) actionObject;
                } else if (actionObject instanceof PossibleAction) {
                    gameData.actions = new ArrayList<PossibleAction>();
                    // Since Rails 1.3.1: separate PossibleActionsObjects
                    while (actionObject instanceof PossibleAction) {
                        gameData.actions.add((PossibleAction)actionObject);
                        try {
                            actionObject = ois.readObject();
                        } catch (EOFException e) {
                            break;
                        }
                    }
                }
                break;
            }
            /**
          todo: the code below is far from perfect, but robust
             */

            // init user comments to have a defined object in any case
            gameData.userComments = new TreeMap<Integer,String>();

            // at the end of file user comments are added as SortedMap
            if (actionObject instanceof SortedMap) {
                gameData.userComments = (SortedMap<Integer, String>) actionObject;
                log.debug("file load: found user comments");
            } else {
                try {
                    Object object = ois.readObject();
                    if (object instanceof SortedMap) {
                        gameData.userComments = (SortedMap<Integer, String>) actionObject;
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
            initialized = true;
        } catch (Exception e) {
            log.fatal("Load failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
            initialized = false;
        }
        return initialized;
    }

    public void replayGame() throws Exception {
        if (!initialized) {
            throw new ConfigurationException("No game was loaded/initialized");
        }

        GameManagerI gameManager = loadedGame.getGameManager();
        log.debug("Starting to execute loaded actions");
        gameManager.setReloading(true);

        int count = -1;
        for (PossibleAction action : gameData.actions) {
            count++;
            if (!gameManager.processOnReload(action)) {
                log.error ("Load interrupted");
                DisplayBuffer.add(LocalText.getText("LoadInterrupted", count));
                ReportBuffer.add(LocalText.getText("LoadInterrupted", count));
                break;
            }
        }

        gameManager.setReloading(false);
        ReportBuffer.setCommentItems(gameData.userComments);

        // callback to GameManager
        gameManager.finishLoading();
    }

    /**
     * sets the meta data required for a game save
     */
    public void initSave(Long saveFileVersionID, String gameName, Map<String, String> gameOptions, List<String> playerNames) {
        gameData.meta.version = Game.version+" "+BuildInfo.buildDate;
        gameData.meta.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        gameData.meta.fileVersionID = saveFileVersionID;
        gameData.meta.gameName = gameName;
        gameData.gameOptions = gameOptions;
        gameData.playerNames = playerNames;
        initSave = true;
    }

    /**
     * Stores the game to a file
     * requires initSave and setting actions and comments
     */
    public boolean saveGame(File file, boolean displayErrorMessage, String errorMessageKey) {
        if (!(initSave || dataLoadDone) || gameData.actions == null) {
            log.warn("File save not possible due to missing data");
            return false;
        }
        boolean result = false;
        log.info("Trying to save file to " + file.getAbsoluteFile());
        try {
            ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(gameData.meta.version);
            oos.writeObject(gameData.meta.date);
            oos.writeObject(gameData.meta.fileVersionID);
            oos.writeObject(gameData.meta.gameName);
            oos.writeObject(gameData.gameOptions);
            oos.writeObject(gameData.playerNames);
            for (PossibleAction action : gameData.actions) {
                oos.writeObject(action);
            }
            oos.writeObject(gameData.userComments);
            oos.close();

            result = true;
            log.info("File save successfull");
        } catch (IOException e) {
            log.error(errorMessageKey, e);
            if (displayErrorMessage) {
                DisplayBuffer.add(LocalText.getText("SaveFailed", e.getMessage()));
            }
        }
        return result;
    }

}
