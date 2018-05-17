using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.GameRound;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using GameLib.Rails.Game.Correct;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */

namespace GameLib.Net.Game
{
    public class GameManager : RailsManager, IConfigurable, IOwner
    {
        protected Type stockRoundClass = typeof(StockRound);
        protected Type operatingRoundClass = typeof(OperatingRound);
        protected Type shareSellingRoundClass = typeof(ShareSellingRound);

        // Variable UI Class names
        protected string gameUIManagerClassName = GuiDef.GetDefaultClassName(GuiDef.ClassName.GAME_UI_MANAGER);
        protected string orUIManagerClassName = GuiDef.GetDefaultClassName(GuiDef.ClassName.OR_UI_MANAGER);
        protected string gameStatusClassName = GuiDef.GetDefaultClassName(GuiDef.ClassName.GAME_STATUS);
        protected string statusWindowClassName = GuiDef.GetDefaultClassName(GuiDef.ClassName.STATUS_WINDOW);
        protected string orWindowClassName = GuiDef.GetDefaultClassName(GuiDef.ClassName.OR_WINDOW);
        protected string startRoundWindowClassName = GuiDef.GetDefaultClassName(GuiDef.ClassName.START_ROUND_WINDOW);

        // map of correctionManagers
        protected Dictionary<CorrectionType, CorrectionManager> correctionManagers = new Dictionary<CorrectionType, CorrectionManager>();

        /** Map relating portfolio names and objects, to enable deserialization.
         * OBSOLETE since Rails 1.3.1, but still required to enable reading old saved files */
        protected Dictionary<string, PortfolioModel> portfolioMap = new Dictionary<string, PortfolioModel>();
        /** Map relating portfolio unique names and objects, to enable deserialization */
        protected Dictionary<string, PortfolioModel> portfolioUniqueNameMap = new Dictionary<string, PortfolioModel>();

        protected int currentNumberOfOperatingRounds = 1;
        protected bool skipFirstStockRound = false;
        protected bool showCompositeORNumber = true;

        protected bool forcedSellingCompanyDump = true;
        protected bool gameEndsWithBankruptcy = false;
        protected int gameEndsWhenBankHasLessOrEqual = 0;
        protected bool gameEndsAfterSetOfORs = true;

        protected bool dynamicOperatingOrder = true;

        /** Will only be set during game reload */
        protected bool reloading = false;

        protected Dictionary<GameDef.Parm, Object> gameParameters = new Dictionary<GameDef.Parm, object>();

        /**
         * Current round should not be set here but from within the Round classes.
         * This is because in some cases the round has already changed to another
         * one when the constructor terminates. Example: if the privates have not
         * been sold, it finishes by starting an Operating Round, which handles the
         * privates payout and then immediately starts a new Start Round.
         */
        protected GenericState<IRoundFacade> currentRound;
        protected IRoundFacade interruptedRound = null;

        protected IntegerState startRoundNumber;
        protected IntegerState srNumber;


        protected IntegerState absoluteORNumber;
        protected IntegerState relativeORNumber;
        protected IntegerState numOfORs;

        protected BooleanState firstAllPlayersPassed;


        /** GameOver pending, a last OR or set of ORs must still be completed */
        protected BooleanState gameOverPending;
        /** GameOver is executed, no more moves */
        protected BooleanState gameOver;
        protected Boolean gameOverReportedUI = false;
        protected BooleanState endedByBankruptcy;




        /** UI display hints */
        protected GuiHints guiHints;

        /** Flags to be passed to the UI, aiding the layout definition */
        protected Dictionary<GuiDef.Parm, Boolean> guiParameters = new Dictionary<GuiDef.Parm, bool>();

        protected GenericState<StartPacket> startPacket;

        protected PossibleActions possibleActions = PossibleActions.Create();

        protected ListState<PossibleAction> executedActions;

        /** Special properties that can be used by other players or companies
         * than just the owner (such as buyable bonus tokens as in 1856).
         */
        protected Portfolio<SpecialProperty> commonSpecialProperties = null;

        /** indicates that the recoverySave already issued a warning, avoids displaying several warnings */
        protected bool recoverySaveWarning = true;

        /** Flag to skip a subsequent Done action (if present) during reloading.
         * <br>This is a fix to maintain backwards compatibility when redundant
         * actions are skipped in new code versions (such as the bypassing of
         * a treasury trading step if it cannot be executed).
         * <br>This flag must be reset after processing <i>any</i> action (not just Done).
         */
        protected bool skipNextDone = false;
        /** Step that must be in effect to do an actual Done skip during reloading.
         * <br> This is to ensure that Done actions in different OR steps are
         * considered separately.
         */
        protected GameDef.OrStep? skippedStep = null;

        // storage to replace static class variables
        // TODO: Move that to a better place
        protected Dictionary<string, object> objectStorage = new Dictionary<string, object>();
        protected Dictionary<string, int> storageIds = new Dictionary<string, int>();

        private static int revenueSpinnerIncrement = 10;
        //Used for Storing the PublicCompany to be Founded by a formationround
        private PublicCompany nationalToFound;

        //    private Player NationalFormStartingPlayer = null;

        private Dictionary<PublicCompany, Player> NationalFormStartingPlayer = new Dictionary<PublicCompany, Player>();

        protected PlayerManager.PlayerOrderModel playerNamesModel;

        /**
         * @return the revenueSpinnerIncrement
         */
        public static int RevenueSpinnerIncrement
        {
            get
            {
                return revenueSpinnerIncrement;
            }
        }

        protected static Logger<GameManager> log = new Logger<GameManager>();

        public GameManager(RailsRoot parent, string id) : base(parent, id)
        {
            currentRound = GenericState<IRoundFacade>.Create(this, "currentRound");
            startRoundNumber = IntegerState.Create(this, "startRoundNumber");
            srNumber = IntegerState.Create(this, "srNumber");
            absoluteORNumber = IntegerState.Create(this, "absoluteORNUmber");
            relativeORNumber = IntegerState.Create(this, "relativeORNumber");
            numOfORs = IntegerState.Create(this, "numOfORs");
            firstAllPlayersPassed = BooleanState.Create(this, "firstAllPlayersPassed");
            gameOverPending = BooleanState.Create(this, "gameOverPending");
            gameOver = BooleanState.Create(this, "gameOver");
            endedByBankruptcy = BooleanState.Create(this, "endedByBankruptcy");
            startPacket = GenericState<StartPacket>.Create(this, "startPacket");
            executedActions = ListState<PossibleAction>.Create(this, "executedActions");

            guiHints = GuiHints.Create(this, "guiHints");
        }

        public void ConfigureFromXML(Tag tag)
        {
            /* Get the rails.game name as configured */
            Tag gameTag = tag.GetChild("Game");
            if (gameTag == null)
            {
                throw new ConfigurationException(
                "No Game tag specified in GameManager tag");
            }

            string gameName = gameTag.GetAttributeAsString("name");
            // TODO (Rails 2.0): Check if this still works and is still needed
            if (gameName == null)
            {
                throw new ConfigurationException("No gameName specified in Game tag");
            }
            if (!gameName.Equals(GetRoot.GameName))
            {
                throw new ConfigurationException("Deviating gameName specified in Game tag");
            }

            InitGameParameters();

            Tag gameParmTag = tag.GetChild("GameParameters");
            if (gameParmTag != null)
            {
                // StockRound class and other properties
                Tag srTag = gameParmTag.GetChild("StockRound");
                if (srTag != null)
                {
                    // FIXME: Rails 2.0, move this to some default .xml!
                    string srClassName =
                        srTag.GetAttributeAsString("class", "GameLib.Net.Game.Financial.StockRound");
                    try {
                        stockRoundClass = Type.GetType(srClassName, true);
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException("Cannot find class "
                                + srClassName, e);
                    }
                    string stockRoundSequenceRuleString =
                        srTag.GetAttributeAsString("sequence");
                    if (!string.IsNullOrEmpty(stockRoundSequenceRuleString))
                    {
                        if (stockRoundSequenceRuleString.Equals("SellBuySell", StringComparison.OrdinalIgnoreCase))
                        {
                            SetGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE, StockRound.SELL_BUY_SELL);
                        }
                        else if (stockRoundSequenceRuleString.Equals("SellBuy", StringComparison.OrdinalIgnoreCase))
                        {
                            SetGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE, StockRound.SELL_BUY);
                        }
                        else if (stockRoundSequenceRuleString.Equals("SellBuyOrBuySell", StringComparison.OrdinalIgnoreCase))
                        {
                            SetGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE, StockRound.SELL_BUY_OR_BUY_SELL);
                        }
                    }

                    skipFirstStockRound = srTag.GetAttributeAsBoolean("skipFirst", skipFirstStockRound);

                    foreach (string ruleTagName in srTag.GetChildren().Keys)
                    {
                        if (ruleTagName.Equals("NoSaleInFirstSR"))
                        {
                            SetGameParameter(GameDef.Parm.NO_SALE_IN_FIRST_SR, true);
                        }
                        else if (ruleTagName.Equals("NoSaleIfNotOperated"))
                        {
                            SetGameParameter(GameDef.Parm.NO_SALE_IF_NOT_OPERATED, true);
                        }
                        else if (ruleTagName.Equals("NoSaleOfJustBoughtShare"))
                        {
                            SetGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT, true);
                        }
                    }
                }

                // OperatingRound class
                Tag orTag = gameParmTag.GetChild("OperatingRound");
                if (orTag != null)
                {
                    // FIXME: Rails 2.0, move this to some default .xml!
                    string orClassName =
                        orTag.GetAttributeAsString("class", "GameLib.Net.Game.OperatingRound");
                    try
                    {
                        operatingRoundClass = Type.GetType(orClassName, true);
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException("Cannot find class "
                                + orClassName, e);
                    }

                    Tag orderTag = orTag.GetChild("OperatingOrder");
                    if (orderTag != null)
                    {
                        dynamicOperatingOrder = orderTag.GetAttributeAsBoolean("dynamic",
                                dynamicOperatingOrder);
                    }

                    Tag emergencyTag = orTag.GetChild("EmergencyTrainBuying");
                    if (emergencyTag != null)
                    {
                        SetGameParameter(GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN,
                                emergencyTag.GetAttributeAsBoolean("mustBuyCheapestTrain",
                                        GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN.DefaultValueAsBoolean()));
                        SetGameParameter(GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN,
                                emergencyTag.GetAttributeAsBoolean("mayAlwaysBuyNewTrain",
                                        GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN.DefaultValueAsBoolean()));
                        SetGameParameter(GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY,
                                emergencyTag.GetAttributeAsBoolean("mayBuyFromCompany",
                                        GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY.DefaultValueAsBoolean()));
                    }
                    Tag revenueIncrementTag = orTag.GetChild("RevenueIncrement");
                    if (revenueIncrementTag != null)
                    {
                        revenueSpinnerIncrement = revenueIncrementTag.GetAttributeAsInteger("amount", 10);
                    }
                }

                // ShareSellingRound class
                Tag ssrTag = gameParmTag.GetChild("ShareSellingRound");
                if (ssrTag != null)
                {
                    // FIXME: Rails 2.0, move this to some default .xml!
                    string ssrClassName =
                        ssrTag.GetAttributeAsString("class", "GameLib.Net.Game.ShareSellingRound");
                    try {
                        shareSellingRoundClass = Type.GetType(ssrClassName, true);
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException("Cannot find class "
                                + ssrClassName, e);
                    }
                }

                /* Max. % of shares of one company that a player may hold */
                Tag shareLimitTag = gameParmTag.GetChild("PlayerShareLimit");
                if (shareLimitTag != null)
                {
                    SetGameParameter(GameDef.Parm.PLAYER_SHARE_LIMIT,
                            shareLimitTag.GetAttributeAsInteger("percentage",
                                    GameDef.Parm.PLAYER_SHARE_LIMIT.DefaultValueAsInt()));
                }

                /* Max. % of shares of one company that the bank pool may hold */
                Tag poolLimitTag = gameParmTag.GetChild("BankPoolShareLimit");
                if (poolLimitTag != null)
                {
                    SetGameParameter(GameDef.Parm.POOL_SHARE_LIMIT,
                            shareLimitTag.GetAttributeAsInteger("percentage",
                                    GameDef.Parm.POOL_SHARE_LIMIT.DefaultValueAsInt()));
                }

                /* Max. % of own shares that a company treasury may hold */
                Tag treasuryLimitTag = gameParmTag.GetChild("TreasuryShareLimit");
                if (treasuryLimitTag != null)
                {
                    SetGameParameter(GameDef.Parm.TREASURY_SHARE_LIMIT,
                            shareLimitTag.GetAttributeAsInteger("percentage",
                                    GameDef.Parm.TREASURY_SHARE_LIMIT.DefaultValueAsInt()));
                }
            }


            /* End of rails.game criteria */
            Tag endOfGameTag = tag.GetChild("EndOfGame");
            if (endOfGameTag != null)
            {
                Tag forcedSellingTag = endOfGameTag.GetChild("ForcedSelling");
                if (forcedSellingTag != null)
                {
                    forcedSellingCompanyDump =
                        forcedSellingTag.GetAttributeAsBoolean("CompanyDump", true);
                }
                if (endOfGameTag.GetChild("Bankruptcy") != null)
                {
                    gameEndsWithBankruptcy = true;
                }
                Tag bankBreaksTag = endOfGameTag.GetChild("BankBreaks");
                if (bankBreaksTag != null)
                {
                    gameEndsWhenBankHasLessOrEqual =
                        bankBreaksTag.GetAttributeAsInteger("limit",
                                gameEndsWhenBankHasLessOrEqual);
                    string attr = bankBreaksTag.GetAttributeAsString("finish");
                    if (attr.Equals("setOfORs", StringComparison.OrdinalIgnoreCase))
                    {
                        gameEndsAfterSetOfORs = true;
                    }
                    else if (attr.Equals("currentOR", StringComparison.OrdinalIgnoreCase))
                    {
                        gameEndsAfterSetOfORs = false;
                    }
                }
            }

            Tag guiClassesTag = tag.GetChild("GuiClasses");
            if (guiClassesTag != null)
            {
                // GameUIManager class
                Tag gameUIMgrTag = guiClassesTag.GetChild("GameUIManager");
                if (gameUIMgrTag != null)
                {
                    gameUIManagerClassName =
                        gameUIMgrTag.GetAttributeAsString("class", gameUIManagerClassName);
                    // Check instantiatability (not sure if this belongs here)
                    Configure.CanTypeBeInstantiated(gameUIManagerClassName);
                }

                // ORUIManager class
                Tag orMgrTag = guiClassesTag.GetChild("ORUIManager");
                if (orMgrTag != null)
                {
                    orUIManagerClassName =
                        orMgrTag.GetAttributeAsString("class", orUIManagerClassName);
                    // Check instantiatability (not sure if this belongs here)
                    Configure.CanTypeBeInstantiated(orUIManagerClassName);
                }

                // GameStatus class
                Tag gameStatusTag = guiClassesTag.GetChild("GameStatus");
                if (gameStatusTag != null)
                {
                    gameStatusClassName =
                        gameStatusTag.GetAttributeAsString("class", gameStatusClassName);
                    // Check instantiatability (not sure if this belongs here)
                    Configure.CanTypeBeInstantiated(gameStatusClassName);
                }

                // StatusWindow class
                Tag statusWindowTag = guiClassesTag.GetChild("StatusWindow");
                if (statusWindowTag != null)
                {
                    statusWindowClassName =
                        statusWindowTag.GetAttributeAsString("class",
                                statusWindowClassName);
                    // Check instantiatability (not sure if this belongs here)
                    Configure.CanTypeBeInstantiated(statusWindowClassName);
                }

                // ORWindow class
                Tag orWindowTag = guiClassesTag.GetChild("ORWindow");
                if (orWindowTag != null)
                {
                    orWindowClassName =
                        orWindowTag.GetAttributeAsString("class", orWindowClassName);
                    // Check instantiatability (not sure if this belongs here)
                    Configure.CanTypeBeInstantiated(orWindowClassName);
                }

                // StartRoundWindow class
                Tag startRoundWindowTag = guiClassesTag.GetChild("StartRoundWindow");
                if (startRoundWindowTag != null)
                {
                    startRoundWindowClassName =
                        startRoundWindowTag.GetAttributeAsString("class",
                                startRoundWindowClassName);
                    // Check instantiatability (not sure if this belongs here)
                    Configure.CanTypeBeInstantiated(startRoundWindowClassName);
                }
            }
        }

        public void FinishConfiguration(RailsRoot root)
        {
        }

        public void Init()
        {
            showCompositeORNumber = !"simple".Equals(Config.Get("or.number_format"), StringComparison.OrdinalIgnoreCase);
        }

        public void StartGame()
        {
            SetGuiParameters();
            GetRoot.CompanyManager.InitStartPackets(this);
            BeginStartRound();
        }

        protected void SetGuiParameters()
        {
            CompanyManager cm = GetRoot.CompanyManager;

            foreach (PublicCompany company in cm.GetAllPublicCompanies())
            {
                if (company.HasParPrice) guiParameters[GuiDef.Parm.HAS_ANY_PAR_PRICE] = true;
                if (company.CanBuyPrivates) guiParameters[GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES] = true;
                if (company.CanHoldOwnShares) guiParameters[GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES] = true;
                if (company.MaxNumberOfLoans != 0) guiParameters[GuiDef.Parm.HAS_ANY_COMPANY_LOANS] = true;
            }

            foreach (PrivateCompany company in cm.GetAllPrivateCompanies())
            {
                foreach (SpecialProperty sp in company.GetSpecialProperties())
                {
                    if (sp is SpecialBonusTokenLay)
                    {
                        guiParameters[GuiDef.Parm.DO_BONUS_TOKENS_EXIST] = true;
                        goto end_of_loop;
                    }
                }
                end_of_loop: { }
            }

            // define guiParameters from gameOptions
            if (GameOption.GetAsBoolean(this, "NoMapMode"))
            {
                guiParameters[GuiDef.Parm.NO_MAP_MODE] = true;
                guiParameters[GuiDef.Parm.ROUTE_HIGHLIGHT] = false;
                guiParameters[GuiDef.Parm.REVENUE_SUGGEST] = false;
            } else
            {
                if ("Highlight".Equals(GameOption.GetValue(this, "RouteAwareness"), StringComparison.OrdinalIgnoreCase))
                {
                    guiParameters[GuiDef.Parm.ROUTE_HIGHLIGHT] = true;
                }
                if ("Suggest".Equals(GameOption.GetValue(this, "RevenueCalculation"), StringComparison.OrdinalIgnoreCase))
                {
                    guiParameters[GuiDef.Parm.REVENUE_SUGGEST] = true;
                }
            }

        }

        private void InitGameParameters()
        {

            foreach (GameDef.Parm parm in GameDef.Parm.Values)
            {
                gameParameters[parm] = parm.DefaultValue();
            }
        }

        public PossibleActions GetPossibleActions()
        {
            return possibleActions;
        }

        protected void SetRound(IRoundFacade round)
        {
            currentRound.Set(round);
        }

        public void NextRound(Round round)
        {
            if (round is StartRound)
            {
                if (((StartRound)round).StartPacket.AreAllSold())
                { //This start Round was completed
                    StartPacket nextStartPacket = GetRoot.CompanyManager.GetNextUnfinishedStartPacket();
                    if (nextStartPacket == null)
                    {
                        if (skipFirstStockRound)
                        {
                            Phase currentPhase = GetRoot.PhaseManager.GetCurrentPhase();
                            if (currentPhase.NumberOfOperatingRounds != numOfORs.Value)
                            {
                                numOfORs.Set(currentPhase.NumberOfOperatingRounds);
                            }
                            log.Info("Phase=" + currentPhase.ToText() + " ORs=" + numOfORs);

                            // Create a new OperatingRound (never more than one Stock Round)
                            // OperatingRound.resetRelativeORNumber();

                            relativeORNumber.Set(1);
                            StartOperatingRound(true);
                        }
                        else
                        {
                            StartStockRound();
                        }
                    }
                    else
                    {
                        BeginStartRound();
                    }
                }
                else
                {
                    StartOperatingRound(RunIfStartPacketIsNotCompletelySold());
                }
            }
            else if (round is StockRound)
            {
                Phase currentPhase = GetRoot.PhaseManager.GetCurrentPhase();
                if (currentPhase == null) log.Error("Current Phase is null??");
                numOfORs.Set(currentPhase.NumberOfOperatingRounds);
                log.Info("Phase=" + currentPhase.ToText() + " ORs=" + numOfORs);

                // Create a new OperatingRound (never more than one Stock Round)
                // OperatingRound.resetRelativeORNumber();
                relativeORNumber.Set(1);
                StartOperatingRound(true);

            }
            else if (round is OperatingRound)
            {
                if (gameOverPending.Value && !gameEndsAfterSetOfORs)
                {
                    FinishGame();
                }
                else if (relativeORNumber.Add(1) <= numOfORs.Value)
                {
                    // There will be another OR
                    StartOperatingRound(true);
                }
                else if (GetRoot.CompanyManager.GetNextUnfinishedStartPacket() != null)
                {
                    BeginStartRound();
                }
                else
                {
                    if (gameOverPending.Value && gameEndsAfterSetOfORs)
                    {
                        FinishGame();
                    }
                    else
                    {
                        ((OperatingRound)round).CheckForeignSales();
                        StartStockRound();
                    }
                }
            }
        }

        protected void BeginStartRound()
        {
            StartPacket startPacket = GetRoot.CompanyManager.GetNextUnfinishedStartPacket();

            // check if there are still unfinished startPackets
            if (startPacket != null)
            {
                // set this to the current startPacket
                this.startPacket.Set(startPacket);
                // start a new StartRound
                CreateStartRound(startPacket);
            }
            else
            {
                // otherwise 
                StartStockRound();
            }
        }

        protected void CreateStartRound(StartPacket startPacket)
        {
            string startRoundClassName = startPacket.RoundClassName;
            StartRound startRound = CreateRound<StartRound>(startRoundClassName,
                        "startRound_" + startRoundNumber.Value);
            startRoundNumber.Add(1);
            startRound.Start();
        }

        /** Stub, to be overridden if companies can run before the Start Packet has been completely sold
         * (as in 1835).
         * @return true if companies can run regardless. Default false.
         */
        virtual protected bool RunIfStartPacketIsNotCompletelySold()
        {
            return false;
        }

        protected void StartStockRound()
        {
            StockRound sr = CreateRound<StockRound>(stockRoundClass, "SR_" + srNumber.Value);
            srNumber.Add(1);
            sr.Start();
        }

        protected void StartOperatingRound(bool operate)
        {
            log.Debug("Operating round started with operate-flag=" + operate);
            string orId;
            if (operate)
            {
                orId = "OR_" + absoluteORNumber.Value;
            }
            else
            {
                orId = "OR_Start_" + startRoundNumber.Value;
            }
            OperatingRound or = (OperatingRound)CreateRound(operatingRoundClass, orId);
            if (operate) absoluteORNumber.Add(1);
            or.Start();
        }

        // FIXME: We need an ID!
        protected T CreateRound<T>(string roundClassName, string id) where T : IRoundFacade, ICreatable
        {
            T round = default(T); // null;
            try
            {
                round = Configure.Create<T>(roundClassName, this, id);
            }
            catch (Exception e)
            {
                log.Error("Cannot instantiate class "
                        + roundClassName, e);
                //System.exit(1);
                throw e;
            }
            SetRound(round);
            return round;
        }

        // FIXME: We need an ID!
        protected T CreateRound<T>(Type roundClass, string id) where T : IRoundFacade, ICreatable
        {
            T round = default(T); // null;
            try
            {
                round = Configure.Create<T>(roundClass, this, id);
            }
            catch (Exception e)
            {
                log.Error("Cannot instantiate class "
                        + roundClass.Name, e);
                //System.exit(1);
                throw e;
            }
            SetRound(round);
            return round;
        }

        protected IRoundFacade CreateRound(Type roundClass, string id)
        {
            IRoundFacade round = null;
            try
            {
                round = (IRoundFacade)Configure.Create(roundClass, this, id);
            }
            catch (Exception e)
            {
                log.Error("Cannot instantiate class "
                        + roundClass.Name, e);
                //System.exit(1);
                throw e;
            }
            SetRound(round);
            return round;
        }

        public void NewPhaseChecks(IRoundFacade round)
        {

        }

        public void ReportAllPlayersPassed()
        {
            ReportBuffer.Add(this, LocalText.GetText("ALL_PASSED"));
            firstAllPlayersPassed.Set(true);
        }

        public bool GetFirstAllPlayersPassed()
        {
            return firstAllPlayersPassed.Value;
        }

        public string GetORId()
        {
            if (showCompositeORNumber)
            {
                return GetCompositeORNumber();
            }
            else
            {
                return absoluteORNumber.Value.ToString(); ;
            }
        }

        public int GetAbsoluteORNumber()
        {
            return absoluteORNumber.Value;
        }

        public string GetCompositeORNumber()
        {
            return srNumber.Value + "." + relativeORNumber.Value;
        }

        public int GetRelativeORNumber()
        {
            return relativeORNumber.Value;
        }

        public string GetNumOfORs()
        {
            return numOfORs.ToText();
        }

        public int GetStartRoundNumber()
        {
            return startRoundNumber.Value;
        }

        public int GetSRNumber()
        {
            return srNumber.Value;
        }

        public void StartShareSellingRound(Player player, int cashToRaise,
                PublicCompany cashNeedingCompany, bool problemDumpOtherCompanies)
        {

            interruptedRound = CurrentRound;

            // An id basd on interruptedRound and company id
            string id = "SSR_" + interruptedRound.Id + "_" + cashNeedingCompany.Id;
            // check if other companies can be dumped
            CreateRound<ShareSellingRound>(shareSellingRoundClass, id).Start(
                    interruptedRound, player, cashToRaise, cashNeedingCompany,
                    !problemDumpOtherCompanies || forcedSellingCompanyDump);
            // the last parameter indicates if the dump of other companies is allowed, either this is explicit or
            // the action does not require that check
        }

        public void StartTreasuryShareTradingRound(PublicCompany company)
        {
            interruptedRound = CurrentRound;
            string id = "TreasuryShareRound_" + interruptedRound.Id + "_" + company.Id;
            CreateRound<TreasuryShareRound>(typeof(TreasuryShareRound), id).Start(interruptedRound);
        }

        public bool Process(PossibleAction action)
        {
            bool result = true;

            GetRoot.ReportManager.DisplayBuffer.Clear();
            guiHints.ClearVisibilityHints();
            ChangeStack changeStack = GetRoot.StateManager.ChangeStack;
            bool startGameAction = false;

            if (action is NullAction && ((NullAction)action).Mode == NullAction.Modes.START_GAME)
            {
                // Skip processing at game start after Load.
                // We're only here to create PossibleActions.
                result = true;
                startGameAction = true;
            }
            else if (action != null)
            {

                // Should never be null.

                action.SetActed();
                result = false;

                // Check player
                string actionPlayerName = action.PlayerName;
                string currentPlayerName = CurrentPlayer.Id;
                if (!actionPlayerName.Equals(currentPlayerName))
                {
                    DisplayBuffer.Add(this, LocalText.GetText("WrongPlayer",
                            actionPlayerName, currentPlayerName));
                    return false;
                }

                // Check if the action is allowed
                if (!possibleActions.Validate(action))
                {
                    DisplayBuffer.Add(this, LocalText.GetText("ActionNotAllowed", action.ToString()));
                    return false;
                }


                if (action is GameAction)
                {
                    // Process undo/redo centrally
                    GameAction gameAction = (GameAction)action;
                    result = ProcessGameActions(gameAction);
                }
                else
                {
                    // All other actions: process per round

                    result = ProcessCorrectionActions(action) || CurrentRound.Process(action);
                    if (result && action.HasActed)
                    {
                        executedActions.Add(action);
                    }
                }
            }

            possibleActions.Clear();

            // Note: round may have changed!
            CurrentRound.SetPossibleActions();

            // TODO: SetPossibleAction can contain state changes (like initTurn)
            // Remove that and move closing the ChangeStack after the processing of the action
            if (result && !(action is GameAction) && !(startGameAction))
            {
                changeStack.Close(action);
            }

            // only pass available => execute automatically
            if (!IsGameOver() && possibleActions.ContainsOnlyPass())
            {
                result = Process(possibleActions.GetList()[0]);
            }

            // TODO: Check if this still works as it moved above the close of the ChangeStack 
            // to have a ChangeSet open to initialize the CorrectionManagers
            if (!IsGameOver()) SetCorrectionActions();

            // Add the Undo/Redo possibleActions here.
            if (changeStack.IsUndoPossible(CurrentPlayer))
            {
                possibleActions.Add(new GameAction(GameAction.Modes.UNDO));
            }
            if (changeStack.IsUndoPossible())
            {
                possibleActions.Add(new GameAction(GameAction.Modes.FORCED_UNDO));
            }
            if (changeStack.IsRedoPossible())
            {
                possibleActions.Add(new GameAction(GameAction.Modes.REDO));
            }

            // logging of game actions activated
            foreach (PossibleAction pa in possibleActions.GetList())
            {
                log.Debug(CurrentPlayer.Id + " may: " + pa.ToString());
            }

            return result;
        }

        /**
         * Adds all Game actions
         * Examples are: undo/redo/corrections
         */
        private void SetCorrectionActions()
        {
            // If any Correction is active
            foreach (CorrectionType ct in CorrectionType.AllOf)
            {
                CorrectionManager cm = GetCorrectionManager(ct);
                if (cm.IsActive())
                {
                    possibleActions.Clear();
                }
            }

            // Correction Actions
            foreach (CorrectionType ct in CorrectionType.AllOf)
            {
                CorrectionManager cm = GetCorrectionManager(ct);
                possibleActions.AddAll(cm.CreateCorrections());
            }
        }

        private bool ProcessCorrectionActions(PossibleAction a)
        {
            bool result = false;

            if (a is CorrectionAction)
            {
                CorrectionAction ca = (CorrectionAction)a;
                CorrectionType ct = ca.CorrectionType;
                CorrectionManager cm = GetCorrectionManager(ct);
                result = cm.ExecuteCorrection(ca);
            }

            return result;
        }

        private bool ProcessGameActions(GameAction gameAction)
        {
            // Process undo/redo centrally
            bool result = false;

            ChangeStack changeStack = GetRoot.StateManager.ChangeStack;
            int index = gameAction.MoveStackIndex;
            switch (gameAction.Mode)
            {
                case GameAction.Modes.SAVE:
                    result = Save(gameAction);
                    break;
                case GameAction.Modes.RELOAD:
                    result = Reload(gameAction);
                    break;
                case GameAction.Modes.UNDO:
                    changeStack.Undo();
                    result = true;
                    break;
                case GameAction.Modes.FORCED_UNDO:
                    if (index == -1)
                    {
                        changeStack.Undo();
                    }
                    else
                    {
                        changeStack.Undo(index);
                    }
                    result = true;
                    break;
                case GameAction.Modes.REDO:
                    if (index == -1)
                    {
                        changeStack.Redo();
                    }
                    else
                    {
                        changeStack.Redo(index);
                    }
                    result = true;
                    break;
                case GameAction.Modes.EXPORT:
                    result = Export(gameAction);
                    break;
            }

            return result;
        }

        public bool ProcessOnReload(PossibleAction action)
        {

            GetRoot.ReportManager.DisplayBuffer.Clear();

            // TEMPORARY FIX TO ALLOW OLD 1856 SAVED FILES TO BE PROCESSED
            if (GetRoot.GameName.Equals("1856")
                    //&& currentRound.get().getClass() != CGRFormationRound.class
                    && possibleActions.Contains(typeof(RepayLoans))
                        && (!possibleActions.Contains(action.GetType())
                                || (action.GetType() == typeof(NullAction)
                                        && ((NullAction)action).Mode != NullAction.Modes.DONE)))
            {
                // Insert "Done"
                log.Debug("Action DONE inserted");
                CurrentRound.Process(new NullAction(NullAction.Modes.DONE));
                possibleActions.Clear();
                CurrentRound.SetPossibleActions();
                if (!IsGameOver()) SetCorrectionActions();
            }

            log.Debug("Action (" + action.PlayerName + "): " + action);

            // Log possible actions (normally this is outcommented)
            string playerName = CurrentPlayer.Id;
            foreach (PossibleAction a in possibleActions.GetList())
            {
                log.Debug(playerName + " may: " + a.ToString());
            }

            // New in Rails2.0: Check if the action is allowed
            if (!possibleActions.Validate(action))
            {
                DisplayBuffer.Add(this, LocalText.GetText("ActionNotAllowed",
                        action.ToString()));
                return false;
            }

            // FOR BACKWARDS COMPATIBILITY
            bool doProcess = true;
            if (skipNextDone)
            {
                if (action is NullAction
                        && ((NullAction)action).Mode == NullAction.Modes.DONE)
                {
                    if (currentRound.Value is OperatingRound
                            && ((OperatingRound)currentRound.Value).GetStep() == skippedStep)
                    {
                        doProcess = false;
                    }
                }
            }
            skipNextDone = false;
            skippedStep = null;

            ChangeStack changeStack = GetRoot.StateManager.ChangeStack;

            if (doProcess && !ProcessCorrectionActions(action) && !CurrentRound.Process(action))
            {
                string msg = "Player " + action.PlayerName + "\'s action \""
                + action.ToString() + "\"\n  in " + CurrentRound.RoundName
                + " is considered invalid by the game engine";
                log.Error(msg);
                DisplayBuffer.Add(this, msg);
                return false;
            }
            executedActions.Add(action);

            possibleActions.Clear();
            CurrentRound.SetPossibleActions();
            changeStack.Close(action);

            if (!IsGameOver()) SetCorrectionActions();

            log.Debug("Turn: " + CurrentPlayer.Id);
            return true;
        }

        public void FinishLoading()
        {
            guiHints.ClearVisibilityHints();
        }

        /** recoverySave method
         * Uses filePath defined in save.recovery.filepath
         *  */
        protected void RecoverySave()
        {
            if (Config.Get("save.recovery.active", "yes").Equals("no", StringComparison.OrdinalIgnoreCase)) return;

            GameSaver gameSaver = new GameSaver(GetRoot.GameData, new List<PossibleAction>(executedActions.View()));
            try
            {
                gameSaver.AutoSave();
                recoverySaveWarning = false;
            }
            catch (IOException e)
            {
                // suppress warning after first occurrence
                if (!recoverySaveWarning)
                {
                    DisplayBuffer.Add(this, LocalText.GetText("RecoverySaveFailed", e.Message));
                    recoverySaveWarning = true;
                }
                log.Error("autosave failed", e);
            }
        }

        protected bool Save(GameAction saveAction)
        {
            GameSaver gameSaver = new GameSaver(GetRoot.GameData, new List<PossibleAction>(executedActions.View()));
            string file = saveAction.Filepath;
            try
            {
                gameSaver.SaveGame(file);
                return true;
            }
            catch (IOException e)
            {
                DisplayBuffer.Add(this, LocalText.GetText("SaveFailed", e.Message));
                log.Error("save failed", e);
                return false;
            }
        }
        /**
         * tries to reload the current game
         * executes the additional action(s)
         */
        protected bool Reload(GameAction reloadAction)
        {
            log.Info("Reloading started");

            /* Use gameLoader to load the game data */
            GameLoader gameLoader = new GameLoader();
            string filepath = reloadAction.Filepath;


            if (!gameLoader.ReloadGameFromFile(filepath/*new File(filepath)*/))
            {
                return false;
            }

            /*  followed by actions and comments 
             try{
                 gameLoader.loadGameData(new File(filepath));
                 gameLoader.convertGameData();
             } catch (Exception e)  {
                 log.error("Load failed", e);
                 DisplayBuffer.add(this, LocalText.getText("LOAD_FAILED_MESSAGE", e.getMessage()));
             }
        */
            log.Debug("Starting to compare loaded actions");

            /* gameLoader actions get compared to the executed actions of the current game */
            List<PossibleAction> savedActions = gameLoader.GetActions();

            IsReloading = true;

            // Check size
            if (savedActions.Count < executedActions.Count)
            {
                DisplayBuffer.Add(this, LocalText.GetText("LOAD_FAILED_MESSAGE",
                "loaded file has less actions than current game"));
                return true;
            }

            // Check action identity
            int index = 0;
            PossibleAction executedAction;
            try
            {
                foreach (PossibleAction savedAction in savedActions)
                {
                    if (index < executedActions.Count)
                    {
                        executedAction = executedActions.Get(index);
                        if (!savedAction.EqualsAsAction(executedAction))
                        {
                            DisplayBuffer.Add(this, LocalText.GetText("LoadFailed",
                                    "loaded action \"" + savedAction.ToString()
                                    + "\"<br>   is not same as game action \"" + executedAction.ToString()
                                    + "\""));
                            return true;
                        }
                    }
                    else
                    {
                        if (index == executedActions.Count)
                        {
                            log.Info("Finished comparing old actions, starting to process new actions");
                        }
                        // Found a new action: execute it
                        if (!ProcessOnReload(savedAction))
                        {
                            log.Error("Reload interrupted");
                            DisplayBuffer.Add(this, LocalText.GetText("LoadFailed",
                                    " loaded action \"" + savedAction.ToString() + "\" is invalid"));
                            break;
                        }
                    }
                    index++;
                }
            }
            catch (Exception e)
            {
                log.Error("Reload failed", e);
                DisplayBuffer.Add(this, LocalText.GetText("LoadFailed", e.Message));
                return true;
            }


            IsReloading = false;
            FinishLoading();

            // use new comments (without checks)
            // FIXME (Rails2.0): CommentItems have to be replaced
            // ReportBuffer.setCommentItems(gameLoader.getComments());

            log.Info("Reloading finished");
            return true;
        }


        protected bool Export(GameAction exportAction)
        {

            string filename = exportAction.Filepath;
            bool result = false;

            try
            {
                // #FIXME_file_action
                //PrintWriter pw = new PrintWriter(filename);

                // write map information
                foreach (MapHex hex in GetRoot.MapManager.GetHexes())
                {
                    //pw.println(hex.Id + "," + hex.CurrentTile.ToText() + ","
                    //        + hex.CurrentTileRotation + ","
                    //        + hex.GetOrientationName(hex.CurrentTileRotation)
                    //);
                }
                //pw.close();
                result = true;
            }
            catch (/*IOException*/Exception e)
            {
                log.Error("Save failed", e);
                DisplayBuffer.Add(this, LocalText.GetText("SaveFailed", e.Message));
            }

            return result;
        }

        public void FinishShareSellingRound()
        {
            SetRound(interruptedRound);
            guiHints.CurrentRoundType = interruptedRound.GetType();
            guiHints.SetVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
            guiHints.ActivePanel = GuiDef.Panel.MAP;
            CurrentRound.Resume();
        }

        public void FinishTreasuryShareRound()
        {
            SetRound(interruptedRound);
            guiHints.CurrentRoundType = interruptedRound.GetType();
            guiHints.SetVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
            guiHints.ActivePanel = GuiDef.Panel.MAP;
            ((OperatingRound)CurrentRound).NextStep();
        }

        public void RegisterBankruptcy()
        {
            endedByBankruptcy.Set(true);
            string message =
                LocalText.GetText("PlayerIsBankrupt",
                        CurrentPlayer.Id);
            ReportBuffer.Add(this, message);
            DisplayBuffer.Add(this, message);
            if (gameEndsWithBankruptcy)
            {
                FinishGame();
            }
            else
            {
                ProcessBankruptcy();
            }
        }

        protected void ProcessBankruptcy()
        {
            // Currently a stub, don't know if there is any generic handling (EV)
        }

        public void RegisterBrokenBank()
        {
            gameOverPending.Set(true);
            ReportBuffer.Add(this, LocalText.GetText("BankIsBrokenReportText"));
            string msgContinue;
            if (gameEndsAfterSetOfORs)
                msgContinue = LocalText.GetText("gameOverPlaySetOfORs");
            else
                msgContinue = LocalText.GetText("gameOverPlayOnlyOR");
            string msg = LocalText.GetText("BankIsBrokenDisplayText", msgContinue);
            DisplayBuffer.Add(this, msg);
            AddToNextPlayerMessages(msg, true);
        }

        public void RegisterMaxedSharePrice(PublicCompany company, StockSpace space)
        {
            gameOverPending.Set(true);
            ReportBuffer.Add(this, LocalText.GetText("MaxedSharePriceReportText",
                    company.Id,
                    Bank.Format(this, space.Price)));
            string msgContinue;
            if (gameEndsAfterSetOfORs)
                msgContinue = LocalText.GetText("gameOverPlaySetOfORs");
            else
                msgContinue = LocalText.GetText("gameOverPlayOnlyOR");
            string msg = LocalText.GetText("MaxedSharePriceDisplayText",
                    company.Id,
                    Bank.Format(this, space.Price),
                    msgContinue);
            DisplayBuffer.Add(this, msg);
            AddToNextPlayerMessages(msg, true);
        }

        protected void FinishGame()
        {
            gameOver.Set(true);

            string message = LocalText.GetText("GameOver");
            ReportBuffer.Add(this, message);

            // FIXME: Rails 2.0 this is not allowed as this causes troubles with Undo
            // DisplayBuffer.add(this, message);

            ReportBuffer.Add(this, "");

            List<string> gameReport = GetGameReport();
            foreach (string s in gameReport)
                ReportBuffer.Add(this, s);

            // activate gameReport for UI
            GameOverReportedUI = false;

            // FIXME: This will not work, as it will create duplicated IDs
            CreateRound(typeof(EndOfGameRound), "EndOfGameRound");
        }


        public bool IsDynamicOperatingOrder
        {
            get
            {
                return dynamicOperatingOrder;
            }
        }

        public bool IsGameOver()
        {
            return gameOver.Value;
        }

        public BooleanState GameOverPendingModel
        {
            get
            {
                return gameOverPending;
            }
        }

        public bool GameOverReportedUI
        {
            get
            {
                return (gameOverReportedUI);
            }
            set
            {
                gameOverReportedUI = value;
            }
        }

        public List<string> GetGameReport()
        {

            List<string> b = new List<string>();

            /* Sort players by total worth */
            List<Player> rankedPlayers = new List<Player>();
            foreach (Player player in GetRoot.PlayerManager.Players)
            {
                rankedPlayers.Add(player);
            }
            //Collections.sort(rankedPlayers);
            rankedPlayers.Sort();

            /* Report winner */
            Player winner = rankedPlayers[0];
            b.Add(LocalText.GetText("EoGWinner") + winner.Id + "!");
            b.Add(LocalText.GetText("EoGFinalRanking") + " :");

            /* Report final ranking */
            int i = 0;
            foreach (Player p in rankedPlayers)
            {
                b.Add((++i) + ". " + Bank.Format(this, p.GetWorth()) + " "
                        + p.Id);
            }

            return b;
        }

        public IRoundFacade CurrentRound
        {
            get
            {
                return currentRound.Value;
            }
        }

        public GenericState<IRoundFacade> CurrentRoundModel
        {
            get
            {
                return currentRound;
            }
        }
        public List<PublicCompany> GetAllPublicCompanies()
        {
            return GetRoot.CompanyManager.GetAllPublicCompanies();
        }

        public List<PrivateCompany> GetAllPrivateCompanies()
        {
            return GetRoot.CompanyManager.GetAllPrivateCompanies();
        }

        public void AddPortfolio(PortfolioModel portfolio)
        {
            portfolioMap[portfolio.Name] = portfolio;
            portfolioUniqueNameMap[portfolio.UniqueName] = portfolio;
        }

        public PortfolioModel GetPortfolioByName(string name)
        {
            return portfolioMap[name];
        }

        public PortfolioModel GetPortfolioByUniqueName(string name)
        {
            return portfolioUniqueNameMap[name];
        }

        public StartPacket GetStartPacket()
        {
            return startPacket.Value;
        }

        public Phase CurrentPhase
        {
            get
            {
                return GetRoot.PhaseManager.GetCurrentPhase();
            }
        }

        public bool CanAnyCompanyHoldShares
        {
            get
            {
                return (bool)GetGuiParameter(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES);
            }
        }

        public string GetClassName(GuiDef.ClassName key)
        {
            if (key == GuiDef.ClassName.GAME_UI_MANAGER)
                return gameUIManagerClassName;
            else if (key == GuiDef.ClassName.OR_UI_MANAGER)
                return orUIManagerClassName;
            else if (key == GuiDef.ClassName.STATUS_WINDOW)
                return statusWindowClassName;
            else if (key == GuiDef.ClassName.START_ROUND_WINDOW)
                return startRoundWindowClassName;
            else if (key == GuiDef.ClassName.GAME_STATUS)
                return gameStatusClassName;
            else return "";
        }

        public /*object*/bool GetGuiParameter(GuiDef.Parm key)
        {
            if (guiParameters.ContainsKey(key))
            {
                return guiParameters[key];
            }
            else
            {
                return false;
            }
        }

        public void SetGuiParameter(GuiDef.Parm key, bool value)
        {
            guiParameters[key] = value;
        }

        public void SetGameParameter(GameDef.Parm key, object value)
        {
            gameParameters[key] = value;
        }

        public object GetGameParameter(GameDef.Parm key)
        {
            if (gameParameters.ContainsKey(key))
            {
                return gameParameters[key];
            }
            else
            {
                return false;
            }
        }

        public IRoundFacade InterruptedRound
        {
            get
            {
                return interruptedRound;
            }
        }

        // TODO: Was the int position argument required?
        public bool AddSpecialProperty(SpecialProperty property)
        {

            if (commonSpecialProperties == null)
            {
                commonSpecialProperties = PortfolioSet<SpecialProperty>.Create(this,
                        "CommonSpecialProperties");
            }
            return commonSpecialProperties.Add(property);
        }

        // TODO: Write new SpecialPropertiesModel

        /* (non-Javadoc)
         * @see rails.game.GameManager#getCommonSpecialProperties()
         */
        /*    public bool removeSpecialProperty(SpecialProperty property) {

                if (commonSpecialProperties != null) {
                    return commonSpecialProperties.removeObject(property);
                }

                return false;
            } */

        public List<SpecialProperty> GetCommonSpecialProperties()
        {
            return GetSpecialProperties<SpecialProperty>(false);
        }

        public Portfolio<SpecialProperty> CommonSpecialPropertiesPortfolio
        {
            get
            {
                return commonSpecialProperties;
            }
        }

        public List<T> GetSpecialProperties<T>(bool includeExercised) where T : SpecialProperty
        {
            List<T> result = new List<T>();

            if (commonSpecialProperties != null)
            {
                foreach (SpecialProperty sp in commonSpecialProperties)
                {
                    if (typeof(T).IsAssignableFrom(sp.GetType())
                            && sp.IsExecutionable
                            && (!sp.IsExercised() || includeExercised))
                    {
                        result.Add((T)sp);
                    }
                }
            }

            return result;
        }

        public GuiHints UIHints
        {
            get
            {
                return guiHints;
            }
        }

        public CorrectionManager GetCorrectionManager(CorrectionType ct)
        {
            CorrectionManager cm = null;
            if (correctionManagers.ContainsKey(ct))
            {
                cm = correctionManagers[ct];
            }
            if (cm == null)
            {
                cm = ct.NewCorrectionManager(this);
                correctionManagers[ct] = cm;
                log.Debug("Added CorrectionManager for " + ct);
            }
            return cm;
        }

        public List<PublicCompany> GetCompaniesInRunningOrder()
        {

            SortedDictionary<int, PublicCompany> operatingCompanies = new SortedDictionary<int, PublicCompany>();
            StockSpace space;
            int key;
            int minorNo = 0;
            foreach (PublicCompany company in GetRoot.CompanyManager.GetAllPublicCompanies())
            {

                // Key must put companies in reverse operating order, because sort
                // is ascending.
                if (company.HasStockPrice && company.HasStarted())
                {
                    space = company.GetCurrentSpace();
                    key =
                        1000000 * (999 - space.Price) + 10000
                        * (99 - space.Column) + 100
                        * space.Row
                        + space.GetStackPosition(company);
                }
                else
                {
                    key = ++minorNo;
                }
                operatingCompanies[key] = company;
            }

            return new List<PublicCompany>(operatingCompanies.Values);
        }

        public bool IsReloading
        {
            get
            {
                return reloading;
            }
            set
            {
                reloading = value;
            }
        }

        public void SetSkipDone(GameDef.OrStep step)
        {
            skipNextDone = true;
            skippedStep = step;
        }

        public void ResetStorage()
        {
            objectStorage = new Dictionary<string, object>();
            storageIds = new Dictionary<string, int>();
        }

        public int GetStorageId(string typeName)
        {
            if (!storageIds.ContainsKey(typeName))
                return 0;
            else
                return storageIds[typeName];
        }

        public int StoreObject(string typeName, object o)
        {
            int id;
            if (!storageIds.ContainsKey(typeName))
            {
                id = 0;
            }
            else
            {
                id = storageIds[typeName];
            }
            //if (id == null) id = 0;
            objectStorage[typeName + id.ToString()] = o;
            storageIds[typeName] = id + 1; // store next id
            return id;
        }

        public object RetrieveObject(string typeName, int id)
        {
            if (!objectStorage.ContainsKey(typeName + id.ToString()))
                return null;

            return objectStorage[typeName + id.ToString()];
        }

        // TODO (Rails2.0): rewrite this, use PhaseAction interface stored at PhaseManager
        public void ProcessPhaseAction(string name, string value)
        {
            CurrentRound.ProcessPhaseAction(name, value);
        }

        // FIXME (Rails2.0): does nothing now, replace this with a rewrite
        public void AddToNextPlayerMessages(string s, bool undoable)
        {

        }

        // shortcut to PlayerManager
        public int GetPlayerCertificateLimit(Player player)
        {
            int limit = GetRoot.PlayerManager.GetPlayerCertificateLimit(player);
            return limit;
        }

        // shortcut to PlayerManager
        protected Player CurrentPlayer
        {
            get
            {
                return GetRoot.PlayerManager.CurrentPlayer;
            }
        }

        public void SetNationalToFound(string national)
        {

            foreach (PublicCompany company in GetAllPublicCompanies())
            {
                if (company.Id.Equals("national"))
                {
                    nationalToFound = company;
                }
            }
        }

        public PublicCompany GetNationalToFound()
        {
            // TODO Auto-generated method stub
            return nationalToFound;
        }

        public void SetNationalFormationStartingPlayer(PublicCompany nationalToFound2, Player currentPlayer)
        {
            NationalFormStartingPlayer[nationalToFound2] = currentPlayer;

        }

        public Player GetNationalFormationStartingPlayer(PublicCompany comp)
        {
            return NationalFormStartingPlayer[comp];
        }
    }
}
