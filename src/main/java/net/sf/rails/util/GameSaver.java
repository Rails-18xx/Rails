package net.sf.rails.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.sf.rails.common.Config;
import net.sf.rails.common.GameData;
import net.sf.rails.common.LocalText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.PossibleAction;


/**
 * GameLoader is responsible to load a saved Rails game
 */
public class GameSaver {
    
    private static final Logger log =
            LoggerFactory.getLogger(GameSaver.class);

    /** Version ID of the Save file header, as written in save() */
    private static final long saveFileHeaderVersionID = 3L;
    /**
     * Overall save file version ID, taking into account the version ID of the
     * action package.
     */
    public static final long saveFileVersionID =
        saveFileHeaderVersionID * PossibleAction.serialVersionUID;

    // static data for autosave
    public static final String autosaveFolder = "autosave";
    public static final String autosaveFile = "18xx_autosave.rails";

    // game data
    private final GameIOData gameIOData = new GameIOData();
    
    /**
     * Creates a new game saver
     * @param gameData of the game to save
     * @param actions to save
     */
    public GameSaver(GameData gameData, List<PossibleAction> actions) {
        gameIOData.setGameData(gameData);
        gameIOData.setVersion(Config.getVersion());
        gameIOData.setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        gameIOData.setActions(actions);
        gameIOData.setFileVersionID(saveFileVersionID);
    }
    
    /**
     * Creates a new game saver based on a gameLoader 
     * @param gameLoader to use
     */
    public GameSaver(GameLoader gameLoader) {
        this(gameLoader.getRoot().getGameData(), gameLoader.getActions());
    }

    /**
     * Stores the game to a file
     * @param file to save game to
     */
    public void saveGame(File file) throws IOException {
        log.info("Trying to save file to " + file.getAbsoluteFile());

        ObjectOutputStream oos =
            new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(gameIOData.getVersion());
        oos.writeObject(gameIOData.getDate());
        oos.writeObject(gameIOData.getFileVersionID());
        oos.writeObject(gameIOData.getGameData().getGameName());
        oos.writeObject(gameIOData.getGameData().getGameOptions().getOptions());
        oos.writeObject(gameIOData.getGameData().getPlayers());
        for (PossibleAction action : gameIOData.getActions()) {
            oos.writeObject(action);
        }
        oos.close();
        log.info("File save successfull");
    }
    
    /**
     * stores game to autosave file
     * @throws IOException
     */
    public void autoSave() throws IOException  {
        File directory = SystemOS.get().getConfigurationFolder(autosaveFolder, true);
        String fileName = autosaveFile;
        
        // create temporary new save file
        File tempFile = new File(directory, fileName + ".tmp");
        saveGame(tempFile);
        log.debug("Created temporary recovery file, path = "  + tempFile.getPath());

        // rename the temp file to the recover file
        File recoveryFile = new File(directory, fileName);
        log.debug("Potential recovery at "  + recoveryFile.getPath());
        // check if previous save file exists
        boolean renameResult;
        if (recoveryFile.exists()) {
            log.debug("Recovery file exists");
            File backupFile = new File(directory, fileName + ".bak");
            //delete backup file if existing
            if (backupFile.exists()) backupFile.delete();
            //old recovery file becomes new backup file
            recoveryFile.renameTo(backupFile);
            log.debug("Recovery file renamed to " + backupFile.getPath());
            //temp file becomes new recoveryFile
            renameResult = tempFile.renameTo(recoveryFile);
        } else {
            log.debug("Recovery file does not exist");
            renameResult = tempFile.renameTo(recoveryFile);
        }

        if (!renameResult) {
            String message = LocalText.getText("RecoveryRenameFailed");
            throw new IOException(message);
        }
        log.debug("Renamed to recovery file, path = "  + recoveryFile.getPath());
    }
}
