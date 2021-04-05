package net.sf.rails.util;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import net.sf.rails.common.Config;
import net.sf.rails.common.GameData;
import net.sf.rails.common.LocalText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.PossibleAction;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * GameLoader is responsible to load a saved Rails game
 */
public class XmlGameSaver implements IGameSaver {

    private static final Logger log = LoggerFactory.getLogger(XmlGameSaver.class);

    /**
     * Version ID of the Save file header, as written in save()
     */
    private static final long SAVE_FILE_HEADER_VERSION_ID = 3L;
    /**
     * Overall save file version ID, taking into account the version ID of the
     * action package.
     */
    public static final long saveFileVersionID = SAVE_FILE_HEADER_VERSION_ID * PossibleAction.serialVersionUID;

    // static data for autosave
    public static final String AUTOSAVE_FOLDER = "autosave";
    public static final String AUTOSAVE_FILE = "18xx_autosave.rails";

    // game data
    private final GameIOData gameIOData = new GameIOData();

    /**
     * Creates a new game saver
     *
     * @param gameData of the game to save
     * @param actions  to save
     */
    public XmlGameSaver(GameData gameData, List<PossibleAction> actions) {
        gameIOData.setGameData(gameData);
        gameIOData.setVersion(Config.getVersion());
        gameIOData.setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        gameIOData.setActions(actions);
        gameIOData.setFileVersionID(saveFileVersionID);
    }

    /**
     * Creates a new game saver based on a gameLoader
     *
     * @param gameLoader to use
     */
    public XmlGameSaver(GameLoader gameLoader) {
        this(gameLoader.getRoot().getGameData(), gameLoader.getActions());
    }

    /**
     * Stores the game to a file
     *
     * @param file to save game to
     */
    public void saveGame(File file) throws IOException {
        log.info("Saving to {}", file.getAbsoluteFile());

        final XStream xStream = new XStream();

        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            xStream.toXML(gameIOData, outputStream);
        }

        log.debug("File save successful");
    }

    /**
     * stores game to autosave file
     *
     * @throws IOException
     */
    public void autoSave() throws IOException {
        File directory = SystemOS.get().getConfigurationFolder(AUTOSAVE_FOLDER, true);
        String fileName = AUTOSAVE_FILE;

        // create temporary new save file
        File tempFile = new File(directory, fileName + ".tmp");
        saveGame(tempFile);
        log.debug("Created temporary recovery file, path = {}", tempFile.getPath());

        // rename the temp file to the recover file
        File recoveryFile = new File(directory, fileName);
        log.debug("Potential recovery at {}", recoveryFile.getPath());
        // check if previous save file exists
        boolean renameResult;
        if (recoveryFile.exists()) {
            log.debug("Recovery file exists");
            File backupFile = new File(directory, fileName + ".bak");
            //delete backup file if existing
            if (backupFile.exists()) {
                if (!backupFile.delete()) {
                    log.warn("Unable to delete file {}", backupFile);
                }
            }
            //old recovery file becomes new backup file
            if (!recoveryFile.renameTo(backupFile)) {
                log.warn("Unable to rename recovery file {}", recoveryFile);
            } else {
                log.debug("Recovery file renamed to {}", backupFile.getPath());
            }
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
        log.debug("Renamed to recovery file, path = {}", recoveryFile.getPath());
    }
}
