using GameLib.Net.Common;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

// #FIXME_GameSaver
namespace GameLib.Net.Util
{
    public class GameSaver
    {
        private static Logger<GameSaver> log = new Logger<GameSaver>();

        /** Version ID of the Save file header, as written in save() */
        private const long saveFileHeaderVersionID = 3L;
        /**
         * Overall save file version ID, taking into account the version ID of the
         * action package.
         */
        public const long saveFileVersionID = saveFileHeaderVersionID * PossibleAction.serialVersionUID;

        // static data for autosave
        public const string autosaveFolder = "autosave";
        public const string autosaveFile = "18xx_autosave.rails";

        // game data
        private GameIOData gameIOData = new GameIOData();

        /**
         * Creates a new game saver
         * @param gameData of the game to save
         * @param actions to save
         */
        public GameSaver(GameData gameData, List<PossibleAction> actions)
        {
            gameIOData.GameData = gameData;
            gameIOData.Version = Config.Version;
            gameIOData.Date = $"{DateTime.Now:G}";
            //gameIOData.SetDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").Format(new Date()));
            gameIOData.Actions = actions;
            gameIOData.FileVersionID = saveFileVersionID;
        }

        /**
         * Creates a new game saver based on a gameLoader 
         * @param gameLoader to use
         */
        public GameSaver(GameLoader gameLoader) :
            this(gameLoader.GetRoot.GameData, gameLoader.GetActions())
        {

        }

        public void SaveGame(string file)
        {
            log.Info("Trying to save file to " + Path.GetFullPath(file));

            using (FileStream s = File.Open(file, FileMode.Create))
            using (StreamWriter sw = new StreamWriter(s))
            {
                SerializeGame(sw, true);
            }
        }

        public string SaveGameToString()
        {
            StringBuilder sb = new StringBuilder();
            using (StringWriter sw = new StringWriter(sb))
            {
                SerializeGame(sw, false);
            }

            return sb.ToString();
        }

        /**
         * Stores the game to a file
         * @param file to save game to
         */
        public void SerializeGame(TextWriter tw, bool indentJson)
        {
            //log.Info("Trying to save file to " + Path.GetFullPath(file));
            JsonSerializer serializer = new JsonSerializer();
            serializer.TypeNameHandling = TypeNameHandling.All;
            //using (FileStream s = File.Open(file, FileMode.Create))
            //using (StreamWriter sw = new StreamWriter(s))
            using (JsonWriter writer = new JsonTextWriter(tw))
            {
                writer.Formatting = indentJson ? Formatting.Indented : Formatting.None;
                //serializer.Serialize(writer, gameIOData.Version);
                //serializer.Serialize(writer, gameIOData.Date);
                //serializer.Serialize(writer, gameIOData.FileVersionID);
                //serializer.Serialize(writer, gameIOData.GameData.GameName);
                //serializer.Serialize(writer, gameIOData.GameData.GameOptions.GetOptions());
                //serializer.Serialize(writer, gameIOData.GameData.Players);
                //serializer.Serialize(writer, gameIOData.Actions);
                serializer.Serialize(writer, gameIOData);
                serializer.Serialize(writer, gameIOData.Actions);
#if false
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
        log.info("File save successful");
#endif
                log.Info("File save successful");
            }
        }

            /**
             * stores game to autosave file
             * @throws IOException
             */
            public void AutoSave()
        {
            throw new NotImplementedException();
#if false
            File directory = SystemOS.get().getConfigurationFolder(autosaveFolder, true);
            string fileName = autosaveFile;

            // create temporary new save file
            File tempFile = new File(directory, fileName + ".tmp");
            saveGame(tempFile);
            log.debug("Created temporary recovery file, path = " + tempFile.getPath());

            // rename the temp file to the recover file
            File recoveryFile = new File(directory, fileName);
            log.debug("Potential recovery at " + recoveryFile.getPath());
            // check if previous save file exists
            boolean renameResult;
            if (recoveryFile.exists())
            {
                log.debug("Recovery file exists");
                File backupFile = new File(directory, fileName + ".bak");
                //delete backup file if existing
                if (backupFile.exists()) backupFile.delete();
                //old recovery file becomes new backup file
                recoveryFile.renameTo(backupFile);
                log.debug("Recovery file renamed to " + backupFile.getPath());
                //temp file becomes new recoveryFile
                renameResult = tempFile.renameTo(recoveryFile);
            }
            else
            {
                log.debug("Recovery file does not exist");
                renameResult = tempFile.renameTo(recoveryFile);
            }

            if (!renameResult)
            {
                string message = LocalText.getText("RecoveryRenameFailed");
                throw new IOException(message);
            }
            log.debug("Renamed to recovery file, path = " + recoveryFile.getPath());
#endif
        }

    }
}
