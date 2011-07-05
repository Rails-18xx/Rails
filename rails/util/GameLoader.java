package rails.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.game.Game;
import rails.game.GameManager;
import rails.game.GameManagerI;
import rails.game.ReportBuffer;
import rails.game.action.PossibleAction;

/**
 * @author freystef
 *
 */
public class GameLoader {

    protected static Logger log =
        Logger.getLogger(Game.class.getPackage().getName());
    
    private boolean dataLoadDone;
    private boolean initialized;
    
    private ObjectInputStream ois;

    private String saveVersion;
    private String saveDate;
    private Long saveFileVersionID;
    private String saveGameName;
    private Map<String, String> selectedGameOptions;
    private List<String> playerNames;
    private List<PossibleAction> listOfActions;
    private SortedMap<Integer, String> userComments;
    
    private Game loadedGame;

    public String getGameData() {
        StringBuilder s = new StringBuilder();
        s.append("Rails saveVersion = " + saveVersion + "\n");
        s.append("File was saved at " + saveDate + "\n");
        s.append("Saved versionID=" + saveFileVersionID + "\n");
        s.append("Save game=" + saveGameName + "\n");
        for (String key : selectedGameOptions.keySet()) {
            s.append("Option "+key+"="+selectedGameOptions.get(key)+ "\n");
        }
        int i=1;
        for (String player : playerNames) {
            s.append("Player "+(i++)+": "+player + "\n");
        }
        return s.toString();
    }
    
    public Game getGame() {
        return loadedGame;
    }
    
    public List<PossibleAction> getActions() {
        return listOfActions;
    }
    
    public SortedMap<Integer, String> getComments() {
        return userComments;
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
                saveVersion = (String)object;
                object = ois.readObject();
            } else {
                // Allow for older saved file versions.
                saveVersion = "pre-1.0.7";
            }
            
            log.info("Reading Rails " + saveVersion +" saved file "+filename);

            if (object instanceof String) {
                saveDate = (String)object;
                log.info("File was saved at "+ saveDate);
                object = ois.readObject();
            }

            // read versionID for serialization compatibility
            saveFileVersionID = (Long) object;
            log.debug("Saved versionID="+saveFileVersionID+" (object="+object+")");
            long GMsaveFileVersionID = GameManager.saveFileVersionID;
            
            if (saveFileVersionID != GMsaveFileVersionID) {
                throw new Exception("Save version " + saveFileVersionID
                                    + " is incompatible with current version "
                                    + GMsaveFileVersionID);
            }

            // read name of saved game
            saveGameName = (String) ois.readObject();
            log.debug("Saved game="+ saveGameName);
           
            // read selected game options and player names
            selectedGameOptions = (Map<String, String>) ois.readObject();
            log.debug("Selected game options = " + selectedGameOptions);
            playerNames = (List<String>) ois.readObject();
            log.debug("Player names = " + playerNames);
            
        } catch (Exception e) {
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
        loadedGame = new Game(saveGameName, playerNames, selectedGameOptions);

        if (!loadedGame.setup()) {
            loadedGame = null;
            throw new ConfigurationException("Error in setting up " + saveGameName);
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
      // Read game actions into listOfActions
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
                  listOfActions = (List<PossibleAction>) actionObject;
              } else if (actionObject instanceof PossibleAction) {
                  listOfActions = new ArrayList<PossibleAction>();
                  // Since Rails 1.3.1: separate PossibleActionsObjects
                  while (actionObject instanceof PossibleAction) {
                      listOfActions.add((PossibleAction)actionObject);
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
          userComments = new TreeMap<Integer,String>();
          
          // at the end of file user comments are added as SortedMap
          if (actionObject instanceof SortedMap) {
              userComments = (SortedMap<Integer, String>) actionObject;
              log.debug("file load: found user comments");
          } else {
              try {
                  Object object = ois.readObject();
                  if (object instanceof SortedMap) {
                      userComments = (SortedMap<Integer, String>) actionObject;
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
        
      for (PossibleAction action : listOfActions) {
              if (!gameManager.processOnReload(action)) {
                  log.error ("Load interrupted");
                  DisplayBuffer.add(LocalText.getText("LoadInterrupted"));
                  break;
              }
      }
      
      gameManager.setReloading(false);
      ReportBuffer.setCommentItems(userComments);

      // callback to GameManager
      gameManager.finishLoading();
    }
}
