using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

// #FIXME_GameLoader
namespace GameLib.Net.Util
{
    public class GameLoader
    {
        private static Logger<GameLoader> log = new Logger<GameLoader>();

        // game data
        private GameIOData gameIOData = new GameIOData();

        // object data
        //private ObjectInputStream ois = null;
        private RailsRoot railsRoot = null;
        private Exception exception = null;

        public GameLoader() { }

        public static void LoadAndStartGame(string gameFile)
        {
            GameLoader gameLoader = new GameLoader();
            if (!gameLoader.CreateFromFile(gameFile))
            {
                throw new FileLoadException();
            }
#if false
            SplashWindow splashWindow = new SplashWindow(true, gameFile.getAbsolutePath());
            splashWindow.notifyOfStep(SplashWindow.STEP_LOAD_GAME);

            // use gameLoader instance to start game
            GameLoader gameLoader = new GameLoader();
            if (!gameLoader.createFromFile(gameFile))
            {
                Exception e = gameLoader.getException();
                log.error("Game load failed", e);
                if (e is RailsReplayException) {
                    string title = LocalText.getText("LOAD_INTERRUPTED_TITLE");
                    string message = LocalText.getText("LOAD_INTERRUPTED_MESSAGE", e.getMessage());
                    JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
                } else {
                    string title = LocalText.getText("LOAD_FAILED_TITLE");
                    string message = LocalText.getText("LOAD_FAILED_MESSAGE", e.getMessage());
                    JOptionPane.showMessageDialog(splashWindow.getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
                    // in this case start of game cannot continued
                    return;
                }
            }

            GameUIManager gameUIManager = startGameUIManager(gameLoader.getRoot(), true, splashWindow);

            // TODO: Check if this is correct
            gameUIManager.setSaveDirectory(gameFile.getParent());

            gameUIManager.startLoadedGame();
            gameUIManager.notifyOfSplashFinalization();
            splashWindow.finalizeGameInit();
            splashWindow = null;
#endif
        }

#if false
        public static GameUIManager StartGameUIManager(RailsRoot game, bool wasLoaded, SplashWindow splashWindow)
        {
            // TODO: Replace that with a Configure method
            GameManager gameManager = game.getGameManager();
            string gameUIManagerClassName = gameManager.getClassName(GuiDef.ClassName.GAME_UI_MANAGER);
            GameUIManager gameUIManager = null;
            try
            {
                Class <? extends GameUIManager > gameUIManagerClass =
                    Class.forName(gameUIManagerClassName).asSubclass(GameUIManager.class);
            gameUIManager = gameUIManagerClass.newInstance();
            gameUIManager.init(game, wasLoaded, splashWindow);
        } catch (Exception e) {
            log.error("Cannot instantiate class " + gameUIManagerClassName, e);
            System.exit(1);
        }
        return gameUIManager;
    }
#endif

        // FIXME: Rails 2.0 add undefined attribute to allow
        // deviations from undefined to default values
        private GameOptionsSet.Builder LoadDefaultGameOptions(string gameName)
        {

            log.Debug("Load default Game Options of " + gameName);
            GameOptionsSet.Builder loadGameOptions = null;
            try
            {
                loadGameOptions = GameOptionsParser.Load(gameName);
            }
            catch (ConfigurationException e)
            {
                log.Error(e.Message);
                loadGameOptions = GameOptionsSet.GetBuilder();
            }
            return loadGameOptions;
        }

        /**
         * Load the gameData from file
         * @param filePath
         */

        public void LoadGameData(JsonSerializer serializer, JsonReader reader)
        {
            //log.Info("Loading game from file " + Path.GetFullPath(gameFile));

            //JsonSerializer serializer = new JsonSerializer();
            //using (FileStream s = File.Open(gameFile, FileMode.Open))
            //using (StreamReader sr = new StreamReader(s))
            //using (JsonReader reader = new JsonTextReader(sr))
            //{

            gameIOData = serializer.Deserialize<GameIOData>(reader);
            //gameIOData.Version = serializer.Deserialize<string>(reader);
            log.Info("Reading Rails " + gameIOData.Version + " saved data");
            //gameIOData.Date = serializer.Deserialize<string>(reader);
            log.Info("File was saved at " + gameIOData.Date);
            //gameIOData.FileVersionID = serializer.Deserialize<long>(reader);
            log.Debug("Saved versionID=" + gameIOData.FileVersionID);
            long saveFileVersionID = GameSaver.saveFileVersionID;
            if (gameIOData.FileVersionID != saveFileVersionID)
            {
                throw new Exception("Save version " + gameIOData.FileVersionID
                        + " is incompatible with current version "
                        + saveFileVersionID);
            }

            //string gameName = serializer.Deserialize<string>(reader);
            string gameName = gameIOData.GameData.GameName;
            log.Debug("Saved game=" + gameName);

            // read default and saved game options
            GameOptionsSet.Builder gameOptions = LoadDefaultGameOptions(gameName);
            //Dictionary<string, string> savedOptions = serializer.Deserialize<Dictionary<string, string>>(reader);
            var savedOptions = gameIOData.GameData.GameOptions.GetOptions();
            log.Debug("Saved game options = " + savedOptions);
            foreach (GameOption option in gameOptions.GetOptions())
            {
                string name = option.Name;
                if (savedOptions.ContainsKey(name))
                {
                    option.SelectedValue = savedOptions[name];
                    log.Debug("Assigned option from file " + name);
                }
                else
                {
                    // FIXME: Rails 2.0 add unassigned value as other default possibility
                    log.Debug("Missing option in save file " + name + " using default value instead");
                }
            }

            // read playerNames
            //List<string> playerNames = serializer.Deserialize<List<string>>(reader);
            var playerNames = gameIOData.GameData.Players;
            log.Debug("Player names = " + playerNames);
            //GameInfo game = GameInfo.CreateLegacy(gameName);

            //gameIOData.GameData = GameData.Create(game, gameOptions, playerNames);
        }

        /**
         * Convert the gameData
         * Requires successful load of gameData
         */
        public void ConvertGameData(JsonSerializer serializer, JsonReader reader)
        {
            gameIOData.Actions = serializer.Deserialize<List<PossibleAction>>(reader);

            // Read game actions into gameData.listOfActions
            // read next object in stream
            //object actionObject = null;
            //while (true)
            //{ // Single-pass loop.
            //    try
            //    {
            //        actionObject = ois.readObject();
            //    }
            //    catch (EOFException e)
            //    {
            //        // Allow saved file at start of game (with no actions).
            //        break;

            //    }
            //    if (actionObject is List)
            //    {
            //        // Until Rails 1.3: one List of PossibleAction
            //        gameIOData.setActions((List<PossibleAction>)actionObject);
            //    }
            //    else if (actionObject is PossibleAction)
            //    {
            //        List<PossibleAction> actions = Lists.newArrayList();
            //        // Since Rails 1.3.1: separate PossibleActionsObjects
            //        while (actionObject is PossibleAction)
            //        {
            //            actions.add((PossibleAction)actionObject);
            //            try
            //            {
            //                actionObject = ois.readObject();
            //            }
            //            catch (EOFException e)
            //            {
            //                break;
            //            }
            //        }
            //        gameIOData.setActions(actions);
            //    }
            //    break;
            //}
            /**
todo: the code below is far from perfect, but robust
             */

#if false // not reading user comments now
            // at the end of file user comments are added as SortedMap
            if (actionObject is SortedMap) {
                // FIXME (Rails2.0): Do something with userComments
                //gameData.userComments = (SortedMap<Integer, string>) actionObject;
                log.debug("file load: found user comments");
            } else {
                try
                {
                    Object object = ois.readObject();
                    if (object is SortedMap) {
                        // FIXME (Rails2.0): Do something with userComments
                        // gameData.userComments = (SortedMap<Integer, string>) actionObject;
                        log.debug("file load: found user comments");
                    }
                }
                catch (IOException e)
                {
                    // continue without comments, if any IOException occurs
                    // sometimes not only the EOF Exception is raised
                    // but also the java.io.StreamCorruptedException: invalid type code
                }
            }
            ois.close();
            ois = null;
#endif
        }

        /**
         * @return false if exception occurred
         */
        public bool ReplayGame()
        {

            GameManager gameManager = railsRoot.GameManager;
            log.Debug("Starting to execute loaded actions");
            gameManager.IsReloading = true;

            int count = -1;
            if (gameIOData != null && gameIOData.Actions != null)
            {
                // set possible actions for first action
                gameManager.CurrentRound.SetPossibleActions();
                foreach (PossibleAction action in gameIOData.Actions)
                {
                    count++;
                    if (!gameManager.ProcessOnReload(action))
                    {
                        log.Warn("Replay of game interrupted");
                        string message = LocalText.GetText("LoadInterrupted", count);
                        exception = new Exception();//new RailsReplayException(message);
                        break;
                    }
                }
            }

            gameManager.IsReloading = false;

            // FIXME (Rails2.0): CommentItems have to be replaced
            // ReportBuffer.setCommentItems(gameData.userComments);

            // callback to GameManager
            gameManager.FinishLoading();
            // return true if no exception occurred
            return (exception == null);
        }

        public RailsRoot GetRoot
        {
            get
            {
                return railsRoot;
            }
        }

        public Exception GetException()
        {
            return exception;
        }

        public List<PossibleAction> GetActions()
        {
            return gameIOData.Actions;
        }

        public string GetGameDataAsText()
        {
            return gameIOData.MetaDataAsText() + gameIOData.GameOptionsAsText() + gameIOData.PlayerNamesAsText();
        }

        /**
         * @param gameFile
         * @return false if exception occurred
         */
        public bool CreateFromFile(string gameFile)
        {
            log.Info("Loading game from file " + Path.GetFullPath(gameFile));

            using (FileStream s = File.Open(gameFile, FileMode.Open))
            using (StreamReader sr = new StreamReader(s))
            {
                return DeserializeGame(sr);
            }
        }

        public bool CreateFromString(string data)
        {
            log.Info("Loading game from string");

            using (StringReader sr = new StringReader(data))
            {
                return DeserializeGame(sr);
            }
        }

        public bool DeserializeGame(TextReader sr)
        {
            JsonSerializer serializer = new JsonSerializer();
            serializer.Context = new System.Runtime.Serialization.StreamingContext(System.Runtime.Serialization.StreamingContextStates.Other, this);
            //serializer.TypeNameHandling = TypeNameHandling.All;
            //using (FileStream s = File.Open(gameFile, FileMode.Open))
            //using (StreamReader sr = new StreamReader(s))
            using (JsonReader reader = new JsonTextReader(sr))
            {
                reader.SupportMultipleContent = true;
                try
                {
                    // 1st: loadGameData
                    LoadGameData(serializer, reader);

                    // 2nd: create game
                    railsRoot = RailsRoot.Create(gameIOData.GameData);

                    if (reader.TokenType == JsonToken.EndObject) reader.Read();
                    // 3rd: convert game data (retrieve actions)
                    ConvertGameData(serializer, reader);

                    // 4th: start game
                    railsRoot.Start();
                }
                catch (Exception e)
                {
                    log.Debug("Exception during createFromFile in gameLoader " + e.Message);
                    exception = e;
                    return false;
                }
            }
            // 5th: replay game
            return ReplayGame();
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
#if false
        public static class RailsObjectInputStream extends ObjectInputStream
{

        private final GameLoader loader;
        
        public RailsObjectInputStream(GameLoader loader, InputStream in) throws IOException
{
    super(in);
            this.loader = loader;
}

public RailsRoot GetRoot
{
            get
            {
                return loader.getRoot();
            }
}
        
//        @Override
//        protected java.io.ObjectStreamClass readClassDescriptor() 
//                throws IOException, ClassNotFoundException {
//            ObjectStreamClass desc = super.readClassDescriptor();
//            string className = desc.getName();
//            log.debug("Found class = " + className);
//            if (className.startsWith("rails.")) {
//                string newClassName = className.replace("rails.", "net.sf.rails.");
//                log.debug("Replaced class " + className + " by new class " + newClassName);
//                return ObjectStreamClass.lookup(Class.forName(newClassName));
//            } else {
//                return desc;
//            }
//        }
    }
#endif
        public bool ReloadGameFromFile(string file)
        {
            using (FileStream s = File.Open(file, FileMode.Open))
            using (StreamReader sr = new StreamReader(s))
            {
                return ReloadGameFromStream(sr);
            }
        }

        public bool ReloadGameFromString(string data)
        {
            using (StringReader sr = new StringReader(data))
            {
                return ReloadGameFromStream(sr);
            }
        }

        public bool ReloadGameFromStream(TextReader tr)
        {
            JsonSerializer serializer = new JsonSerializer();
            serializer.Context = new System.Runtime.Serialization.StreamingContext(System.Runtime.Serialization.StreamingContextStates.Other, this);
            //using (FileStream s = File.Open(file, FileMode.Open))
            //using (StreamReader sr = new StreamReader(s))
            using (JsonTextReader reader = new JsonTextReader(tr))
            {
                reader.SupportMultipleContent = true;

                try
                {
                    // 1st: loadGameData
                    LoadGameData(serializer, reader);

                    railsRoot = RailsRoot.Instance;

                    // fix problems reading multiple types from file
                    if (reader.TokenType == JsonToken.EndObject) reader.Read();

                    // 2nd: convert game data (retrieve actions)
                    ConvertGameData(serializer, reader);
                }
                catch (Exception e)
                {
                    log.Debug("Exception during createFromFile in gameLoader " + e.Message);
                    exception = e;
                    return false;
                }
            }
            return true;
        }
    }
}
