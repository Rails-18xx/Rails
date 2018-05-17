using GameLib.Net.Algorithms;
using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using System;
using System.Collections.Generic;


namespace GameLib.Net.Game
{
    public class RailsRoot : Root, IRailsItem
    {
        private static Logger<RailsRoot> log = new Logger<RailsRoot>();

        // currently we assume that there is only one instance running
        // thus it is possible to retrieve that version
        // TODO: Replace this by a full support of concurrent usage
        private static RailsRoot instance;

        public static RailsRoot Instance
        {
            get
            {
                return instance;
            }
        }

        // Base XML file
        private const string GAME_XML_FILE = "Game.xml";

        // Instance fields

        // Game data fields
        private GameData gameData;

        // Component Managers
        private GameManager gameManager;
        private CompanyManager companyManager;
        private PlayerManager playerManager;
        private PhaseManager phaseManager;
        private TrainManager trainManager;
        private StockMarket stockMarket;
        private MapManager mapManager;
        private TileManager tileManager;
        private RevenueManager revenueManager;
        private Bank bank;

        // Other Managers
        private ReportManager reportManager;

        private RailsRoot(GameData gameData) : base()
        {
            foreach (string playerName in gameData.Players)
            {
                log.Debug("Player: " + playerName);
            }
            foreach (string optionName in gameData.GameOptions.GetOptions().Keys)
            {
                log.Debug("Option: " + optionName + "="
                        + gameData.GameOptions.Get(optionName));
            }

            this.gameData = gameData;
        }

        public static RailsRoot Create(GameData gameData)
        {
            Precondition.CheckState(instance == null,
                "Currently only a single instance of RailsRoot is allowed");
            instance = new RailsRoot(gameData);
            log.Debug("RailsRoot: instance created");
            instance.Init();
            log.Debug("RailsRoot: instance initialized");
            instance.InitGameFromXML();
            log.Debug("RailsRoot: game configuration initialized");
            instance.FinishConfiguration();
            log.Debug("RailsRoot: game configuration finished");

            return instance;
        }


        // feedback from ComponentManager
        public void SetComponent(IConfigurable component)
        {
            switch (component)
            {
                case PlayerManager mgr:
                    playerManager = mgr;
                    break;
                case Bank mgr:
                    bank = mgr;
                    break;
                case CompanyManager mgr:
                    companyManager = mgr;
                    break;
                case StockMarket mgr:
                    stockMarket = mgr;
                    break;
                case GameManager mgr:
                    gameManager = mgr;
                    break;
                case PhaseManager mgr:
                    phaseManager = mgr;
                    break;
                case TrainManager mgr:
                    trainManager = mgr;
                    break;
                case MapManager mgr:
                    mapManager = mgr;
                    break;
                case TileManager mgr:
                    tileManager = mgr;
                    break;
                case RevenueManager mgr:
                    revenueManager = mgr;
                    break;
                default:
                    throw new ArgumentException($"Unknown component {component.GetType().Name} passed to SetComponent");
            }
        }

        public void InitGameFromXML()
        {
            string directory = "data" + ResourceLoader.SEPARATOR + gameData.GameName;
            // #FIXME file access
            var file = GameInterface.Instance.XmlLoader.LoadXmlFile(GAME_XML_FILE, directory);
            Tag componentManagerTag = Tag.FindTopTagInFile(file, 
                    /*GAME_XML_FILE, directory,*/ XmlTags.COMPONENT_MANAGER_ELEMENT_ID, gameData.GameOptions);

            ComponentManager componentManager = new ComponentManager();
            componentManager.Start(this, componentManagerTag);
            // The componentManager automatically returns results

            // creation of Report facilities
            reportManager = ReportManager.Create(this, "reportManager");
        }

        public bool FinishConfiguration()
        {
            /*
             * Initializations that involve relations between components can
             * only be done after all XML has been processed.
             */
            log.Info("========== Start of rails.game " + gameData.GameName + " ==========");
            log.Info("Rails version " + Config.Version);
            ReportBuffer.Add(this, LocalText.GetText("GameIs", gameData.GameName));

            playerManager.SetPlayers(gameData.Players, bank);
            gameManager.Init();
            // TODO: Can this be merged above?
            playerManager.Init();

            try
            {
                playerManager.FinishConfiguration(this);
                companyManager.FinishConfiguration(this);
                trainManager.FinishConfiguration(this);
                phaseManager.FinishConfiguration(this);
                tileManager.FinishConfiguration(this);
                mapManager.FinishConfiguration(this);
                bank.FinishConfiguration(this);
                stockMarket.FinishConfiguration(this);

                if (revenueManager != null)
                    revenueManager.FinishConfiguration(this);
            }
            catch (ConfigurationException e)
            {
                log.Error(e.Message);
                //System.out.println(e.getMessage());
                //e.printStackTrace();
                DisplayBuffer.Add(this, e.Message);
                return false;
            }
            return true;
        }

        public string Start()
        {
            // FIXME (Rails2.0): Should this not be part of the initial Setup configuration?
            int nbPlayers = gameData.Players.Count;
            if (nbPlayers < playerManager.MinPlayers
                    || nbPlayers > playerManager.MaxPlayers)
            {
                return gameData.GameName + " is not configured to be played with " + nbPlayers + " players\n"
                        + "Please enter a valid number of players, or add a <Players> entry to data/" + gameData.GameName + "/Game.xml";
            }

            gameManager.StartGame();
            return null;
        }

        /*----- Getters -----*/

        public GameManager GameManager
        {
            get
            {
                return gameManager;
            }
        }

        public CompanyManager CompanyManager
        {
            get
            {
                return companyManager;
            }
        }

        public PlayerManager PlayerManager
        {
            get
            {
                return playerManager;
            }
        }

        public PhaseManager PhaseManager
        {
            get
            {
                return phaseManager;
            }
        }

        public TrainManager TrainManager
        {
            get
            {
                return trainManager;
            }
        }

        public StockMarket StockMarket
        {
            get
            {
                return stockMarket;
            }
        }

        public MapManager MapManager
        {
            get
            {
                return mapManager;
            }
        }

        public TileManager TileManager
        {
            get
            {
                return tileManager;
            }
        }

        public RevenueManager RevenueManager
        {
            get
            {
                return revenueManager;
            }
        }

        public Bank Bank
        {
            get
            {
                return bank;
            }
        }

        public ReportManager ReportManager
        {
            get
            {
                return reportManager;
            }
        }


        /**
         * @return the gameName
         */
        public string GameName
        {
            get
            {
                return gameData.GameName;
            }
        }

        /**
         * @return the gameOptions
         */
        public GameOptionsSet GameOptions
        {
            get
            {
                return gameData.GameOptions;
            }
        }

        /**
         * @return the gameData
         */
        public GameData GameData
        {
            get
            {
                return gameData;
            }
        }


        new public RailsRoot Parent
        {
            get
            {
                throw new NotImplementedException();
            }
        }

        new public RailsRoot GetRoot
        {
            get
            {
                return this;
            }
        }

        IRailsItem IItem<IRailsItem, RailsRoot>.Parent => ((IRailsItem)instance).Parent;

        public static void ClearInstance()
        {
            instance = null;
        }
    }
}
