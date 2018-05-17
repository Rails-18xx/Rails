using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;

namespace ExampleApp
{
    class Program
    {
        private static Logger<Program> log = new Logger<Program>();

        static void SaveGameReport(IEnumerable<string> report, string reportFilename, bool failed)
        {
            StreamWriter reportFile = null;
            try
            {
                reportFile = new StreamWriter(reportFilename);
            }
            catch (IOException)
            {
                Console.WriteLine("Error: cannot open file " + reportFilename + " for report writing");
            }
            if (reportFile != null)
            {
                foreach (string msg in report)
                {
                    reportFile.WriteLine(msg);
                }
                reportFile.Close();
                if (failed)
                {
                    Console.WriteLine("Created failed report at " + reportFilename);
                }
                else
                {
                    Console.WriteLine("Created base line report file at " + reportFilename);
                }
            }
            reportFile.Dispose();
        }

        private static void PrepareGameReport(string gameFile, string reportFilename)
        {
            RailsRoot root = null;
            if (File.Exists(gameFile))
            {
                Console.WriteLine("Found game at " + Path.GetFullPath(gameFile));
                GameLoader gameLoader = new GameLoader();
                if (gameLoader.CreateFromFile(gameFile))
                {
                    root = gameLoader.GetRoot;
                }
            }
            if (root != null)
            {
                var report = root.ReportManager.ReportBuffer.GetAsList();
                SaveGameReport(report, reportFilename, false);
            }
        }

        // returns gameName if preparation was successful
        private static string PrepareTestGame(string gameFile, bool overrideReport)
        {
            // check preconditions
            if (!File.Exists(gameFile)) return null;

            // check if it is a Rails savefile
            string gameName = null;
            if (Path.GetExtension(gameFile).Equals(Config.Get("save.filename.extension")))
            {
                gameName = Path.GetFileNameWithoutExtension(gameFile);
                string gamePath = Path.GetDirectoryName(gameFile);

                // check if there is a reportfile
                //string reportFilename = gamePath + File.separator + gameName
                //        + "." + Config.get("report.filename.extension");
                //File reportFile = new File(reportFilename);

                string reportFilename = Path.Combine(gamePath, gameName);
                reportFilename += "." + Config.Get("report.filename.extension");

                if (!File.Exists(reportFilename) || overrideReport)
                {
                    PrepareGameReport(gameFile, reportFilename);
                    RailsRoot.ClearInstance();
                }
            }

            return gameName;
        }

        private static void RecursiveTestSuite(string rootPath, string dirPath, int level, bool overrideReport)
        {
            string combinedDirPath;
            // completeDirPath
            if (rootPath != null)
                combinedDirPath = Path.Combine(rootPath, dirPath);
            else
                combinedDirPath = dirPath;

            // assign directory
            //File directory = new File(combinedDirPath);

            // check if directory exists otherwise return null
            if (!Directory.Exists(combinedDirPath)) return;

            // add deeper directories
            List<string> dirList = new List<string>(Directory.EnumerateDirectories(combinedDirPath));
            foreach (string dn in dirList)
            {
                RecursiveTestSuite(null, dn, level + 1, overrideReport);
            }

            // use filelist to sort 
            List<string> filenameList = new List<string>(Directory.EnumerateFiles(combinedDirPath));
            filenameList.Sort();


            //foreach (string fn in filenameList)
            //{
            //    string f = Path.Combine(combinedDirPath + fn);
            //    string nextDirPath;
            //    if (dirPath.Equals(""))
            //        nextDirPath = f.getName();
            //    else
            //        nextDirPath = dirPath + File.separator + f.getName();
            //    if (f.isDirectory() && level <= maxRecursionLevel)
            //    {
            //        TestSuite newSuite = recursiveTestSuite(rootPath, nextDirPath, level + 1, overrideReport);
            //        if (newSuite != null) suite.addTest(newSuite);
            //    }
            //}

            // add files of directory
            foreach (string fn in filenameList)
            {
                //File f = new File(combinedDirPath + File.separator + fn);
                string gameName = PrepareTestGame(fn, overrideReport);
            }
        }

        private static List<GameInfo> gameList = new List<GameInfo>();

        public static void Initialize()
        {
            string credits;

            GameInfoParser gip = new GameInfoParser();
            SortedSet<GameInfo> gameInfoList = new SortedSet<GameInfo>();
            try
            {
                gameInfoList = gip.ProcessGameList();
                credits = gip.GetCredits();
            }
            catch (ConfigurationException e)
            {
                //Log.Error(e.getMessage());
                Console.WriteLine(e.Message);
            }
            // convert list to map
            foreach (GameInfo game in gameInfoList)
            {
                gameList.Add(game);
            }
        }

        private static Dictionary<GameInfo, GameOptionsSet.Builder> gameOptions = new Dictionary<GameInfo, GameOptionsSet.Builder>();

        static GameOptionsSet.Builder GetAvailableOptions(GameInfo game)
        {
            if (!gameOptions.ContainsKey(game))
            {
                return LoadOptions(game);
            }
            return gameOptions[game];
        }

        private static GameOptionsSet.Builder LoadOptions(GameInfo game)
        {
            Console.WriteLine("Load Game Options of " + game.Name);
            GameOptionsSet.Builder loadGameOptions = null;
            try
            {
                loadGameOptions = GameOptionsParser.Load(game.Name);
            }
            catch (ConfigurationException e)
            {
                Console.WriteLine(e.Message);
                loadGameOptions = GameOptionsSet.GetBuilder();
            }
            gameOptions[game] = loadGameOptions;
            return loadGameOptions;
        }

        private static void StartNewGame(GameInfo g, List<string> p)
        {
            GameInfo selectedGame = g;
            List<String> players = p;
            
            LoadOptions(g);
            GameOptionsSet.Builder selectedOptions = GetAvailableOptions(selectedGame);

            // check against number of available players
            if (players.Count < selectedGame.MinPlayers)
            {
                Console.WriteLine("Not enough players");
                return;
            }

            RailsRoot railsRoot = null;
            try
            {
                GameData gameData = GameData.Create(selectedGame, selectedOptions, players);
                railsRoot = RailsRoot.Create(gameData);
            }
            catch (ConfigurationException e)
            {
                // TODO: Fix this behavior, give more information?
                // Simply exit
                Console.WriteLine("Failed to start game: " + e.Message);
                return;
            }

            String startError = railsRoot.Start();
            if (startError != null)
            {
                Console.WriteLine("Error starting game: " + startError);
                //System.exit(-1);
                return;
            }

            Console.WriteLine("Game started.");
        }

        static GameInfo GetDefaultGame()
        {
            GameInfo defaultGame = GameInfo.FindGame(gameList, Config.Get("default_game"));
            if (defaultGame == null)
            {
                defaultGame = gameList[0];
            }
            return defaultGame;
        }

        protected static bool ProcessOnServer(PossibleAction action)
        {
            bool result;

            Player player = RailsRoot.Instance.PlayerManager.CurrentPlayer;

            action.SetActed();
            action.PlayerName = player.Id;

            log.Debug("==Passing to server: " + action);

           // Player player = getCurrentPlayer();
            if (player != null)
            {
                action.PlayerName = player.Id;
            }

            // Process the action on the server
            result = RailsRoot.Instance.GameManager.Process(action);

            // Follow-up the result
            log.Debug("==Result from server: " + result);

            return result;
        }

        static void Main(string[] args)
        {
            Trace.Listeners.Add(new TextWriterTraceListener(Console.Out));
            Trace.AutoFlush = true;
            var l = LoggerBase.Instance;
            LoggerBase.Level = TraceLevel.Verbose;

            ConfigManager.InitConfiguration(true);

            // Main test directory 
            string rootPath = Config.Get("save.directory");

            if (args != null && args.Length > 0)
            {
                // command line argument: only directories are possible
                Console.WriteLine("Number of args: " + args.Length);
                foreach (string arg in args)
                    // discard test suite, only override the report files
                    RecursiveTestSuite(rootPath, arg, 0, true);
            }
            else
            {
                Initialize();
                GameInfo game = GetDefaultGame();
                List<string> players = new List<string>() { "Billy", "Joe", "Jim", "Bob" };
                StartNewGame(game, players);

                //var saveAction = new GameAction(GameAction.Modes.SAVE);
                //saveAction.Filepath = "saved";
                //ProcessOnServer(saveAction);

                //string saveGameFile = Path.GetFullPath("saved");
                //string reportFile = Path.GetFullPath("report");
                //PrepareGameReport(saveGameFile, reportFile);

                //GameLoader gameLoader = new GameLoader();
                //gameLoader.CreateFromFile(saveGameFile);
                StartRoundTest();
            }
        }

        private static void StartRoundTest()
        {
            RailsRoot root = RailsRoot.Instance;

            var gameManager = root.GameManager;

            var possibleActions = gameManager.GetPossibleActions();

            var action = (BidStartItem)possibleActions.GetList()[2];
            action.ActualBid = 200;
            ProcessOnServer(action);
            possibleActions = gameManager.GetPossibleActions();

            action = (BidStartItem)possibleActions.GetList()[1];
            action.ActualBid = 100;
            ProcessOnServer(action);
            possibleActions = gameManager.GetPossibleActions();

            var buyAction = (BuyStartItem)possibleActions.GetList()[0];
            ProcessOnServer(buyAction);
            possibleActions = gameManager.GetPossibleActions();

            buyAction = (BuyStartItem)possibleActions.GetList()[0];
            ProcessOnServer(buyAction);
            possibleActions = gameManager.GetPossibleActions();

            buyAction = (BuyStartItem)possibleActions.GetList()[0];
            ProcessOnServer(buyAction);
            possibleActions = gameManager.GetPossibleActions();

            buyAction = (BuyStartItem)possibleActions.GetList()[0];
            buyAction.AssociatedSharePrice = 100;
            ProcessOnServer(buyAction);
            possibleActions = gameManager.GetPossibleActions();

            var saveAction = new GameAction(GameAction.Modes.SAVE);
            saveAction.Filepath = "saved";
            ProcessOnServer(saveAction);

        }
    }
}
