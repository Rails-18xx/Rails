using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.GameRound;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using GameLib.Rails.Game.Correct;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

/**
 * Implements a basic Operating Round. <p> A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded.
 */

namespace GameLib.Net.Game
{
    public class OperatingRound : Round, IObserver
    {
        /* Transient memory (per round only) */
        protected GenericState<GameDef.OrStep> stepObject;

        protected bool actionPossible = true;

        /* flag for using rails without map support */
        protected bool noMapMode;

        // TODO: Check if this should not be turned into a State?
        protected List<PublicCompany> companiesOperatedThisRound = new List<PublicCompany>();

        protected ListState<PublicCompany> operatingCompanies = null; // will
                                                                      // be
                                                                      // created
                                                                      // below

        protected GenericState<PublicCompany> operatingCompany;
        // do not use a operatingCompany.getObject() as reference
        // TODO: Question is this remark above still relevant?

        // Non-persistent lists (are recreated after each user action)

        protected DictionaryState<string, int> tileLaysPerColor;

        protected List<LayBaseToken> currentNormalTokenLays = new List<LayBaseToken>();

        protected List<LayBaseToken> currentSpecialTokenLays = new List<LayBaseToken>();

        /** A List per player with owned companies that have excess trains */
        protected Dictionary<Player, List<PublicCompany>> excessTrainCompanies = null;

        protected ListState<TrainCertificateType> trainsBoughtThisTurn;

        protected DictionaryState<PublicCompany, int> loansThisRound = null;

        protected string thisOrNumber;

        protected PossibleAction selectedAction = null;

        protected PossibleAction savedAction = null;

        public const int SPLIT_ROUND_DOWN = 2; // More to the treasury

        // protected static GameDef.OrStep[] steps =
        protected GameDef.OrStep[] steps = new GameDef.OrStep[] {
            GameDef.OrStep.INITIAL, GameDef.OrStep.LAY_TRACK,
            GameDef.OrStep.LAY_TOKEN, GameDef.OrStep.CALC_REVENUE,
            GameDef.OrStep.PAYOUT, GameDef.OrStep.BUY_TRAIN,
            GameDef.OrStep.TRADE_SHARES, GameDef.OrStep.FINAL };

        protected bool doneAllowed = false;

        protected TrainManager trainManager;

        /*
         * ======================================= 1. OR START and END
         * =======================================
         */

        /**
         * Constructed via Configure
         */
        public OperatingRound(GameManager parent, string id) : base(parent, id)
        {
            stepObject = GenericState<GameDef.OrStep>.Create(this, "ORStep", GameDef.OrStep.INITIAL);
            operatingCompany = GenericState<PublicCompany>.Create(this, "operatingCompany");
            tileLaysPerColor = DictionaryState<string, int>.Create(this, "tileLaysPerColour");
            trainsBoughtThisTurn = ListState<TrainCertificateType>.Create(this, "trainsBoughtThisTurn");

            trainManager = GetRoot.TrainManager;

            operatingCompanies = ListState<PublicCompany>.Create(this, "operatingCompanies", SetOperatingCompanies());
            stepObject.AddObserver(this);

            noMapMode = GameOption.GetAsBoolean(this, "NoMapMode");

            guiHints.SetVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
            guiHints.SetVisibilityHint(GuiDef.Panel.STATUS, true);
            guiHints.ActivePanel = GuiDef.Panel.MAP;
        }

        public void Start()
        {

            thisOrNumber = gameManager.GetORId();

            ReportBuffer.Add(this, LocalText.GetText("START_OR", thisOrNumber));

            foreach (Player player in GetRoot.PlayerManager.Players)
            {
                player.SetWorthAtORStart();
            }

            PrivatesPayOut();

            if (operatingCompanies.Count > 0)
            {

                StringBuilder msg = new StringBuilder();
                foreach (PublicCompany company in operatingCompanies.View())
                {
                    msg.Append(",").Append(company.Id);
                }
                string smsg;
                if (msg.Length > 0)
                {
                    smsg = msg.ToString().Substring(1); // DeleteCharAt(0);
                }
                else
                {
                    smsg = msg.ToString();
                }
                log.Info("Initial operating sequence is " + smsg);

                if (SetNextOperatingCompany(true))
                {
                    SetStep(GameDef.OrStep.INITIAL);
                }
                return;
            }

            // No operating companies yet: close the round.
            string text = LocalText.GetText("ShortORExecuted");
            ReportBuffer.Add(this, text);
            DisplayBuffer.Add(this, text);
            FinishRound();
        }

        protected void PrivatesPayOut()
        {
            int count = 0;
            foreach (PrivateCompany priv in companyManager.GetAllPrivateCompanies())
            {
                if (!priv.IsClosed())
                {
                    // Bank Portfolios are not MoneyOwners!
                    if (priv.Owner is IMoneyOwner)
                    {
                        IMoneyOwner recipient = (IMoneyOwner)priv.Owner;
                        int revenue = priv.GetRevenueByPhase(Phase.GetCurrent(this));

                        if (revenue != 0)
                        {
                            if (count++ == 0) ReportBuffer.Add(this, "");
                            string revText = Currency.FromBank(revenue, recipient);
                            ReportBuffer.Add(this, LocalText.GetText("ReceivesFor",
                                    recipient.Id, revText, priv.Id));
                        }
                    }
                }
            }
        }

        override public void Resume()
        {
            if (savedAction is BuyTrain)
            {
                DoBuyTrain((BuyTrain)savedAction);
            }
            else if (savedAction is SetDividend)
            {
                ExecuteSetRevenueAndDividend((SetDividend)savedAction);
            }
            else if (savedAction is RepayLoans)
            {
                ExecuteRepayLoans((RepayLoans)savedAction);
            }
            else if (savedAction == null)
            {
                // nextStep();
            }
            savedAction = null;
            wasInterrupted.Set(true);

            guiHints.SetVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
            guiHints.SetVisibilityHint(GuiDef.Panel.STATUS, true);
            guiHints.ActivePanel = GuiDef.Panel.MAP;
        }

        protected void FinishOR()
        {

            // Check if any privates must be closed
            // (now only applies to 1856 W&SR) - no, that is at end of TURN
            // for (PrivateCompany priv : gameManager.getAllPrivateCompanies()) {
            // priv.checkClosingIfExercised(true);
            // }

            ReportBuffer.Add(this, " ");
            ReportBuffer.Add(this,
                    LocalText.GetText("EndOfOperatingRound", thisOrNumber));

            // Update the worth increase per player
            int orWorthIncrease;
            foreach (Player player in GetRoot.PlayerManager.Players)
            {
                player.SetLastORWorthIncrease();
                orWorthIncrease = player.LastORWorthIncrease.Value;
                ReportBuffer.Add(this, LocalText.GetText("ORWorthIncrease",
                        player.Id, thisOrNumber, Bank.Format(this, orWorthIncrease)));
            }

            // OR done. Inform GameManager.
            FinishRound();
        }

        /*
         * ======================================= 2. CENTRAL PROCESSING FUNCTIONS
         * 2.1. PROCESS USER ACTION=======================================
         */

        override public bool Process(PossibleAction action)
        {
            bool result = false;
            doneAllowed = false;

            /*--- Common OR checks ---*/
            /* Check operating company */
            if (action is PossibleORAction
                    && !(action is DiscardTrain) && !action.IsCorrection)
            {
                PublicCompany company = ((PossibleORAction)action).Company;
                if (company != operatingCompany.Value)
                {
                    DisplayBuffer.Add(this, LocalText.GetText("WrongCompany",
                            company.Id, operatingCompany.Value.Id));
                    return false;
                }
            }

            selectedAction = action;

            if (selectedAction is LayTile)
            {

                LayTile layTileAction = (LayTile)selectedAction;

                switch (layTileAction.LayTileType)
                {
                    case (LayTile.CORRECTION):
                        result = LayTileCorrection(layTileAction);
                        break;
                    default:
                        result = DoLayTile(layTileAction);
                        break;
                }

            }
            else if (selectedAction is LayBaseToken)
            {
                result = DoLayBaseToken((LayBaseToken)selectedAction);
            }
            else if (selectedAction is LayBonusToken)
            {
                result = DoLayBonusToken((LayBonusToken)selectedAction);
            }
            else if (selectedAction is BuyBonusToken)
            {
                result = DoBuyBonusToken((BuyBonusToken)selectedAction);
            }
            else if (selectedAction is OperatingCost)
            {
                result = ExecuteOperatingCost((OperatingCost)selectedAction);
            }
            else if (selectedAction is SetDividend)
            {
                result = SetRevenueAndDividend((SetDividend)selectedAction);
            }
            else if (selectedAction is BuyTrain)
            {
                result = DoBuyTrain((BuyTrain)selectedAction);
            }
            else if (selectedAction is DiscardTrain)
            {
                result = DoDiscardTrain((DiscardTrain)selectedAction);
            }
            else if (selectedAction is BuyPrivate)
            {
                result = DoBuyPrivate((BuyPrivate)selectedAction);
            }
            else if (selectedAction is ReachDestinations)
            {
                result = DoReachDestinations((ReachDestinations)selectedAction);
            }
            else if (selectedAction is TakeLoans)
            {
                result = DoTakeLoans((TakeLoans)selectedAction);
            }
            else if (selectedAction is RepayLoans)
            {
                result = DoRepayLoans((RepayLoans)selectedAction);
            }
            else if (selectedAction is ClosePrivate)
            {
                result = ExecuteClosePrivate((ClosePrivate)selectedAction);
            }
            else if (selectedAction is UseSpecialProperty
                   && ((UseSpecialProperty)selectedAction).SpecialProperty is SpecialRight)
            {
                result = DoBuyRight((UseSpecialProperty)selectedAction);
            }
            else if (selectedAction is NullAction)
            {
                NullAction nullAction = (NullAction)action;
                switch (nullAction.Mode)
                {
                    case NullAction.Modes.DONE:
                    case NullAction.Modes.PASS:
                        result = Done(nullAction);
                        break;
                    case NullAction.Modes.SKIP:
                        Skip(nullAction);
                        result = true;
                        break;
                    default:
                        break;
                }

            }
            else if (ProcessGameSpecificAction(action))
            {

                result = true;

            }
            else
            {

                DisplayBuffer.Add(
                        this,
                        LocalText.GetText("UnexpectedAction",
                                selectedAction.ToString()));
                return false;
            }

            return result;
        }

        /** Stub, to be overridden in game-specific subclasses. */
        virtual public bool ProcessGameSpecificAction(PossibleAction action)
        {
            return false;
        }

        /*
         * ======================================= 2.2. PREPARE NEXT ACTION
         * =======================================
         */

        /**
         * To be called after each change, to re-establish the currently allowed
         * actions. (new method, intended to absorb code from several other
         * methods).
         *
         */
        override public bool SetPossibleActions()
        {

            /* Create a new list of possible actions for the UI */
            possibleActions.Clear();
            selectedAction = null;

            bool forced = false;
            doneAllowed = false; // set default (fix of bug 2954654)

            if (GetStep() == GameDef.OrStep.INITIAL)
            {
                InitTurn();
                if (noMapMode)
                {
                    NextStep(GameDef.OrStep.LAY_TOKEN);
                }
                else
                {
                    InitNormalTileLays(); // new: only called once per turn ?
                    SetStep(GameDef.OrStep.LAY_TRACK);
                }
            }

            GameDef.OrStep step = GetStep();
            if (step == GameDef.OrStep.LAY_TRACK)
            {

                if (!operatingCompany.Value.HasLaidHomeBaseTokens)
                {
                    // This can occur if the home hex has two cities and track,
                    // such as the green OO tile #59

                    // BR: as this is a home token, need to call LayBaseToken with a
                    // MapHex, not a list
                    // to avoid the LayBaseToken action from being a regular token
                    // lay
                    // I am not sure that this will work with multiple home hexes.
                    foreach (MapHex home in operatingCompany.Value.GetHomeHexes())
                    {
                        possibleActions.Add(new LayBaseToken(home));
                    }
                    forced = true;
                }
                else
                {
                    possibleActions.AddAll(GetNormalTileLays(true));
                    possibleActions.AddAll(GetSpecialTileLays(true));
                    possibleActions.Add(new NullAction(NullAction.Modes.SKIP));
                }

            }
            else if (step == GameDef.OrStep.LAY_TOKEN)
            {
                SetNormalTokenLays();
                SetSpecialTokenLays();
                log.Debug("Normal token lays: " + currentNormalTokenLays.Count);
                log.Debug("Special token lays: " + currentSpecialTokenLays.Count);

                possibleActions.AddAll(currentNormalTokenLays);
                possibleActions.AddAll(currentSpecialTokenLays);
                possibleActions.Add(new NullAction(NullAction.Modes.SKIP));
            }
            else if (step == GameDef.OrStep.CALC_REVENUE)
            {
                PrepareRevenueAndDividendAction();
                if (noMapMode) PrepareNoMapActions();
            }
            else if (step == GameDef.OrStep.BUY_TRAIN)
            {
                SetBuyableTrains();
                // TODO Need route checking here.
                // TEMPORARILY allow not buying a train if none owned
                // if (!operatingCompany.getObject().mustOwnATrain()
                // ||
                // operatingCompany.getObject().getPortfolio().getNumberOfTrains() >
                // 0) {
                doneAllowed = true;
                // }
                if (noMapMode && (operatingCompany.Value.GetLastRevenue() == 0))
                    PrepareNoMapActions();
            }
            else if (step == GameDef.OrStep.DISCARD_TRAINS)
            {
                forced = true;
                SetTrainsToDiscard();
            }

            // The following additional "common" actions are only available if the
            // primary action is not forced.
            if (!forced)
            {
                SetBonusTokenLays();

                SetDestinationActions();

                SetGameSpecificPossibleActions();

                // Private Company manually closure
                foreach (PrivateCompany priv in companyManager.GetAllPrivateCompanies())
                {
                    if (!priv.IsClosed() && priv.ClosesManually)
                        possibleActions.Add(new ClosePrivate(priv));
                }

                // Can private companies be bought?
                if (IsPrivateSellingAllowed)
                {
                    // Create a list of players with the current one in front
                    int currentPlayerIndex = operatingCompany.Value.GetPresident().Index;
                    Player player;
                    int minPrice, maxPrice;
                    List<Player> players = new List<Player>(playerManager.Players);
                    int numberOfPlayers = playerManager.NumberOfPlayers;
                    for (int i = currentPlayerIndex; i < currentPlayerIndex
                                                         + numberOfPlayers; i++)
                    {
                        player = players[i % numberOfPlayers];
                        if (!MaySellPrivate(player)) continue;
                        foreach (PrivateCompany privComp in player.PortfolioModel.PrivateCompanies)
                        {

                            // check to see if the private can be sold to a company
                            if (!privComp.TradeableToCompany)
                            {
                                continue;
                            }

                            minPrice = GetPrivateMinimumPrice(privComp);

                            maxPrice = GetPrivateMaximumPrice(privComp);

                            possibleActions.Add(new BuyPrivate(privComp, minPrice, maxPrice));
                        }
                    }
                }

                if (operatingCompany.Value.CanUseSpecialProperties)
                {

                    // Are there any "common" special properties,
                    // i.e. properties that are available to everyone?
                    List<SpecialProperty> commonSP = gameManager.GetCommonSpecialProperties();
                    if (commonSP != null)
                    {
                        SellBonusToken sbt;
                        foreach (SpecialProperty sp in commonSP)
                        {
                            if (sp is SellBonusToken)
                            {
                                sbt = (SellBonusToken)sp;
                                // Can't buy if already owned
                                if (operatingCompany.Value.GetBonuses() != null)
                                {
                                    foreach (Bonus bonus in operatingCompany.Value.GetBonuses())
                                    {
                                        if (bonus.Name.Equals(sp.Id))
                                            goto end_of_loop;
                                    }
                                }
                                possibleActions.Add(new BuyBonusToken(sbt));
                            }

                            end_of_loop: { }
                        }
                    }

                    // Are there other step-independent special properties owned by
                    // the company?
                    List<SpecialProperty> orsps =
                            operatingCompany.Value.PortfolioModel.GetAllSpecialProperties();

                    // TODO: Do we still need this directly from the operating
                    // company?
                    // List<SpecialProperty> compsps =
                    // operatingCompany.get().getSpecialProperties();
                    // if (compsps != null) orsps.addAll(compsps);

                    if (orsps != null)
                    {
                        foreach (SpecialProperty sp in orsps)
                        {
                            if (!sp.IsExercised() && sp.IsUsableIfOwnedByCompany
                                && sp.IsUsableDuringOR(step))
                            {
                                if (sp is SpecialBaseTokenLay)
                                {
                                    if (GetStep() != GameDef.OrStep.LAY_TOKEN)
                                    {
                                        possibleActions.Add(new LayBaseToken((SpecialBaseTokenLay)sp));
                                    }
                                }
                                else if (!(sp is SpecialTileLay))
                                {
                                    possibleActions.Add(new UseSpecialProperty(sp));
                                }
                            }
                        }
                    }
                    // Are there other step-independent special properties owned by
                    // the president?
                    orsps = playerManager.CurrentPlayer.PortfolioModel.GetAllSpecialProperties();
                    if (orsps != null)
                    {
                        foreach (SpecialProperty sp in orsps)
                        {
                            if (!sp.IsExercised() && sp.IsUsableIfOwnedByPlayer
                                && sp.IsUsableDuringOR(step))
                            {
                                if (sp is SpecialBaseTokenLay)
                                {
                                    if (GetStep() != GameDef.OrStep.LAY_TOKEN)
                                    {
                                        possibleActions.Add(new LayBaseToken((SpecialBaseTokenLay)sp));
                                    }
                                }
                                else
                                {
                                    possibleActions.Add(new UseSpecialProperty(sp));
                                }
                            }
                        }
                    }
                }
            }

            if (doneAllowed)
            {
                possibleActions.Add(new NullAction(NullAction.Modes.DONE));
            }

            foreach (PossibleAction pa in possibleActions.GetList())
            {
                try
                {
                    log.Debug(operatingCompany.Value.Id + " may: "
                              + pa.ToString());
                }
                catch (Exception e)
                {
                    log.Error("Error in toString() of " + pa.GetType(), e);
                }
            }

            return true;
        }

        /** Stub, can be overridden by subclasses */
        virtual protected void SetGameSpecificPossibleActions()
        {

        }

        /*
         * ======================================= 2.3. TURN CONTROL
         * =======================================
         */

        protected void InitTurn()
        {
            log.Debug("Starting turn of " + operatingCompany.Value.Id);
            ReportBuffer.Add(this, " ");
            ReportBuffer.Add(this, LocalText.GetText("CompanyOperates",
                    operatingCompany.Value.Id,
                    operatingCompany.Value.GetPresident().Id));
            playerManager.SetCurrentPlayer(operatingCompany.Value.GetPresident());

            if (noMapMode && !operatingCompany.Value.HasLaidHomeBaseTokens)
            {
                // Lay base token in noMapMode
                BaseToken token = operatingCompany.Value.GetNextBaseToken();
                if (token == null)
                {
                    log.Error("Company " + operatingCompany.Value.Id
                              + " has no free token to lay base token");
                }
                else
                {
                    log.Debug("Company " + operatingCompany.Value.Id
                              + " lays base token in nomap mode");
                    // FIXME: This has to be rewritten
                    // Where are the nomap base tokens to be stored?
                    // bank.getUnavailable().addBonusToken(token);
                }
            }
            operatingCompany.Value.InitTurn();
            trainsBoughtThisTurn.Clear();
        }

        protected void FinishTurn()
        {
            if (!operatingCompany.Value.IsClosed())
            {
                operatingCompany.Value.SetOperated();
                companiesOperatedThisRound.Add(operatingCompany.Value);

                foreach (PrivateCompany priv in operatingCompany.Value.PortfolioModel.PrivateCompanies)
                {
                    priv.CheckClosingIfExercised(true);
                }
            }

            if (!FinishTurnSpecials()) return;

            if (SetNextOperatingCompany(false))
            {
                SetStep(GameDef.OrStep.INITIAL);
            }
            else
            {
                FinishOR();
            }
        }

        /**
         * Stub, may be overridden in subclasses Return value: TRUE = normal turn
         * end; FALSE = return immediately from finishTurn().
         */
        virtual protected bool FinishTurnSpecials()
        {
            return true;
        }

        protected bool SetNextOperatingCompany(bool initial)
        {
            while (true)
            {
                if (initial || operatingCompany.Value == null
                    || operatingCompany == null)
                {
                    SetOperatingCompany(operatingCompanies.Get(0));
                    initial = false;
                }
                else
                {
                    int index = operatingCompanies.IndexOf(operatingCompany.Value);
                    if (++index >= operatingCompanies.Count)
                    {
                        return false;
                    }

                    // Check if the operating order has changed
                    List<PublicCompany> newOperatingCompanies =
                            SetOperatingCompanies(new List<PublicCompany>(operatingCompanies.View()),
                                    operatingCompany.Value);
                    PublicCompany company;
                    for (int i = 0; i < newOperatingCompanies.Count; i++)
                    {
                        company = newOperatingCompanies[i];
                        if (company != operatingCompanies.Get(i))
                        {
                            log.Debug("Company " + company.Id + " replaces "
                                      + operatingCompanies.Get(i).Id
                                      + " in operating sequence");
                            operatingCompanies.Move(company, i);
                        }
                    }

                    SetOperatingCompany(operatingCompanies.Get(index));
                }

                if (operatingCompany.Value.IsClosed()) continue;

                return true;
            }
        }

        protected void SetOperatingCompany(PublicCompany company)
        {
            operatingCompany.Set(company);
        }

        /**
         * Get the public company that has the turn to operate.
         *
         * @return The currently operating company object.
         */
        public PublicCompany GetOperatingCompany()
        {
            return operatingCompany.Value;
        }

        public IReadOnlyCollection<PublicCompany> GetOperatingCompanies()
        {
            return operatingCompanies.View();
        }

        public int GetOperatingCompanyndex()
        {
            int index = operatingCompanies.IndexOf(GetOperatingCompany());
            return index;
        }

        /*
         * ======================================= 2.4. STEP CONTROL
         * =======================================
         */

        /**
         * Get the current operating round step (i.e. the next action).
         *
         * @return The number that defines the next action.
         */
        public GameDef.OrStep GetStep()
        {
            return (GameDef.OrStep)stepObject.Value;
        }

        /**
         * Bypass normal order of operations and explicitly set round step. This
         * should only be done for specific rails.game exceptions, such as forced
         * train purchases.
         *
         * @param step
         */
        protected void SetStep(GameDef.OrStep step)
        {

            stepObject.Set(step);

        }

        /**
         * Internal method: change the OR state to the next step. If the currently
         * Operating Company is done, notify this.
         *
         * @param company The current company.
         */
        public void NextStep()
        {
            NextStep(GetStep());
        }

        /** Take the next step after a given one (see nextStep()) */
        protected void NextStep(GameDef.OrStep step)
        {

            PublicCompany company = operatingCompany.Value;

            // Cycle through the steps until we reach one where a user action is
            // expected.
            int stepIndex;
            for (stepIndex = 0; stepIndex < steps.Length; stepIndex++)
            {
                if (steps[stepIndex] == step) break;
            }
            while (++stepIndex < steps.Length)
            {
                step = steps[stepIndex];
                log.Debug("OR considers step " + step);

                if (step == GameDef.OrStep.LAY_TOKEN
                    && company.GetNumberOfFreeBaseTokens() == 0)
                {
                    log.Debug("OR skips " + step + ": No freeBaseTokens");
                    continue;
                }

                if (step == GameDef.OrStep.CALC_REVENUE)
                {

                    if (!company.CanRunTrains())
                    {
                        // No trains, then the revenue is zero.
                        log.Debug("OR skips " + step + ": Cannot run trains");
                        ExecuteSetRevenueAndDividend(new SetDividend(0, false,
                                new int[] { SetDividend.NO_TRAIN }));
                        // TODO: This probably does not handle share selling
                        // correctly
                        continue;
                    }
                }

                if (step == GameDef.OrStep.PAYOUT)
                {
                    // This step is now obsolete
                    log.Debug("OR skips " + step + ": Always skipped");
                    continue;
                }

                if (step == GameDef.OrStep.TRADE_SHARES)
                {

                    // Is company allowed to trade treasury shares?
                    if (!company.MayTradeShares || !company.HasOperated())
                    {
                        continue;
                    }

                    /*
                     * Check if any trading is possible. If not, skip this step.
                     * (but register a Done action for BACKWARDS COMPATIBILITY only)
                     */
                    // Preload some expensive results
                    int ownShare = company.PortfolioModel.GetShare(company);
                    int poolShare = pool.GetShare(company); // Expensive, do it once
                                                            // Can it buy?
                    bool canBuy =
                            ownShare < GameDef.GetGameParameterAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT)
                                    && company.Cash >= company.GetCurrentSpace().Price
                                    && poolShare > 0;
                    // Can it sell?
                    bool canSell =
                            company.PortfolioModel.GetShare(company) > 0
                                    && poolShare < GameDef.GetGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT);
                    // Above we ignore the possible existence of double shares (as
                    // in 1835).

                    if (!canBuy && !canSell)
                    {
                        // XXX For BACKWARDS COMPATIBILITY only,
                        // register a Done skip action during reloading.
                        if (gameManager.IsReloading)
                        {
                            gameManager.SetSkipDone(GameDef.OrStep.TRADE_SHARES);
                            log.Debug("If the next saved action is 'Done', skip it");
                        }
                        log.Info("Skipping Treasury share trading step");
                        continue;
                    }

                    gameManager.StartTreasuryShareTradingRound(operatingCompany.Value);

                }

                if (!GameSpecificNextStep(step))
                {
                    log.Debug("OR skips " + step + ": Not game specific");
                    continue;
                }

                // No reason found to skip this step
                break;
            }

            if (step == GameDef.OrStep.FINAL)
            {
                FinishTurn();
            }
            else
            {
                SetStep(step);
            }
        }

        /** Stub, can be overridden in subclasses to check for extra steps */
        virtual protected bool GameSpecificNextStep(GameDef.OrStep step)
        {
            return true;
        }

        /**
         * This method is only called at the start of each step (unlike
         * updateStatus(), which is called after each user action)
         */
        virtual protected void PrepareStep()
        {
            GameDef.OrStep step = stepObject.Value;

            if (step == GameDef.OrStep.LAY_TRACK)
            {
                // getNormalTileLays();
            }
            else if (step == GameDef.OrStep.LAY_TOKEN)
            {

            }
        }

        /*
         * ======================================= 3. COMMON ACTIONS (not bound to
         * steps) 3.1. NOOPS=======================================
         */

        public void Skip(NullAction action)
        {
            log.Debug("Skip step " + stepObject.Value);

            NextStep();
        }

        /**
         * The current Company is done operating.
         * 
         * @param action TODO
         * @param company Name of the company that finished operating.
         *
         * @return False if an error is found.
         */
        public bool Done(NullAction action)
        {

            if (operatingCompany.Value.PortfolioModel.NumberOfTrains == 0
                && operatingCompany.Value.MustOwnATrain)
            {
                // FIXME: Need to check for valid route before throwing an
                // error.
                /*
                 * Check TEMPORARILY disabled errMsg =
                 * LocalText.GetText("CompanyMustOwnATrain",
                 * operatingCompany.getObject().getName()); setStep(STEP_BUY_TRAIN);
                 * DisplayBuffer.add(this, errMsg); return false;
                 */
            }

            NextStep();

            if (GetStep() == GameDef.OrStep.FINAL)
            {
                FinishTurn();
            }

            return true;
        }

        /*
         * ======================================= 3.2. DISCARDING TRAINS
         * =======================================
         */

        public bool DoDiscardTrain(DiscardTrain action)
        {

            Train train = action.DiscardedTrain;
            PublicCompany company = action.Company;
            string companyName = company.Id;

            string errMsg = null;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Must be correct step
                if (GetStep() != GameDef.OrStep.BUY_TRAIN
                    && GetStep() != GameDef.OrStep.DISCARD_TRAINS)
                {
                    errMsg = LocalText.GetText("WrongActionNoDiscardTrain");
                    break;
                }

                if (train == null && action.IsForced)
                {
                    errMsg = LocalText.GetText("NoTrainSpecified");
                    break;
                }

                // Does the company own such a train?

                if (!Util.Util.ListContains<Train>(company.PortfolioModel.GetTrainList(), train))
                {
                    errMsg = LocalText.GetText("CompanyDoesNotOwnTrain",
                                    company.Id, train.ToText());
                    break;
                }

                break;
            }
            if (errMsg != null)
            {
                DisplayBuffer.Add(this,
                        LocalText.GetText("CannotDiscardTrain", companyName,
                                (train != null ? train.ToText() : "?"), errMsg));
                return false;
            }

            /* End of validation, start of execution */

            // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();

            // Reset type of dual trains
            if (train.CertType.GetPotentialTrainTypes().Count > 1)
            {
                train.SetTrainType(null);
            }

            train.Discard();

            // Check if any more companies must discard trains,
            // otherwise continue train buying
            if (!CheckForExcessTrains())
            {
                // Trains may have been discarded by other players
                playerManager.SetCurrentPlayer(operatingCompany.Value.GetPresident());
                stepObject.Set(GameDef.OrStep.BUY_TRAIN);
            }

            // setPossibleActions();

            return true;
        }

        protected void SetTrainsToDiscard()
        {
            // Scan the players in SR sequence, starting with the current player
            foreach (Player player in GetRoot.PlayerManager.GetNextPlayers(true))
            {
                if (excessTrainCompanies.ContainsKey(player))
                {
                    playerManager.SetCurrentPlayer(player);
                    List<PublicCompany> list = excessTrainCompanies[player];
                    foreach (PublicCompany comp in list)
                    {
                        possibleActions.Add(new DiscardTrain(comp,
                                comp.PortfolioModel.GetUniqueTrains(), true));
                        // We handle one company at at time.
                        // We come back here until all excess trains have been
                        // discarded.
                        return;
                    }
                }
            }
        }

        /*
         * ======================================= 3.3. PRIVATES (BUYING, SELLING,
         * CLOSING)=======================================
         */

        public bool DoBuyPrivate(BuyPrivate action)
        {
            string errMsg = null;
            PublicCompany publicCompany = action.Company;
            string publicCompanyName = publicCompany.Id;
            PrivateCompany privateCompany = action.PrivateCompany;
            string privateCompanyName = privateCompany.Id;
            int price = action.Price;
            IOwner owner = null;
            Player player = null;
            int upperPrice;
            int lowerPrice;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Does private exist?
                if ((privateCompany =
                        companyManager.GetPrivateCompany(privateCompanyName)) == null)
                {
                    errMsg = LocalText.GetText("PrivateDoesNotExist", privateCompanyName);
                    break;
                }
                // Is private still open?
                if (privateCompany.IsClosed())
                {
                    errMsg = LocalText.GetText("PrivateIsAlreadyClosed", privateCompanyName);
                    break;
                }
                // Is private owned by a player?
                owner = privateCompany.Owner;
                if (!(owner is Player))
                {
                    errMsg = LocalText.GetText("PrivateIsNotOwnedByAPlayer", privateCompanyName);
                    break;
                }
                player = (Player)owner;
                upperPrice = privateCompany.GetUpperPrice();
                lowerPrice = privateCompany.GetLowerPrice();

                // Is private buying allowed?
                if (!IsPrivateSellingAllowed)
                {
                    errMsg = LocalText.GetText("PrivateBuyingIsNotAllowed");
                    break;
                }

                // Price must be in the allowed range
                if (lowerPrice != PrivateCompany.NO_PRICE_LIMIT
                    && price < lowerPrice)
                {
                    errMsg =
                            LocalText.GetText("PriceBelowLowerLimit",
                                    Bank.Format(this, price),
                                    Bank.Format(this, lowerPrice),
                                    privateCompanyName);
                    break;
                }
                if (upperPrice != PrivateCompany.NO_PRICE_LIMIT
                    && price > upperPrice)
                {
                    errMsg =
                            LocalText.GetText("PriceAboveUpperLimit",
                                    Bank.Format(this, price),
                                    Bank.Format(this, lowerPrice),
                                    privateCompanyName);
                    break;
                }
                // Does the company have the money?
                if (price > operatingCompany.Value.Cash)
                {
                    errMsg =
                            LocalText.GetText("NotEnoughMoney", publicCompanyName,
                                    Bank.Format(this,
                                            operatingCompany.Value.Cash),
                                    Bank.Format(this, price));
                    break;
                }
                break;
            }
            if (errMsg != null)
            {
                if (owner != null)
                {
                    DisplayBuffer.Add(this, LocalText.GetText(
                            "CannotBuyPrivateFromFor", publicCompanyName,
                            privateCompanyName, owner.Id,
                            Bank.Format(this, price), errMsg));
                }
                else
                {
                    DisplayBuffer.Add(this, LocalText.GetText(
                            "CannotBuyPrivateFor", publicCompanyName,
                            privateCompanyName, Bank.Format(this, price), errMsg));
                }
                return false;
            }

            operatingCompany.Value.BuyPrivate(privateCompany, player, price);

            return true;
        }

        protected bool IsPrivateSellingAllowed
        {
            get
            {
                return Phase.GetCurrent(this).IsPrivateSellingAllowed;
            }
        }

        protected int GetPrivateMinimumPrice(PrivateCompany privComp)
        {
            int minPrice = privComp.GetLowerPrice();
            if (minPrice == PrivateCompany.NO_PRICE_LIMIT)
            {
                minPrice = 0;
            }
            return minPrice;
        }

        protected int GetPrivateMaximumPrice(PrivateCompany privComp)
        {
            int maxPrice = privComp.GetUpperPrice();
            if (maxPrice == PrivateCompany.NO_PRICE_LIMIT)
            {
                maxPrice = operatingCompany.Value.Cash;
            }
            return maxPrice;
        }

        protected bool MaySellPrivate(Player player)
        {
            return true;
        }

        protected bool ExecuteClosePrivate(ClosePrivate action)
        {

            PrivateCompany priv = action.PrivateCompany;

            log.Debug("Executed close private action for private " + priv.Id);

            string errMsg = null;

            if (priv.IsClosed())
                errMsg = LocalText.GetText("PrivateAlreadyClosed", priv.Id);

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, errMsg);
                return false;
            }

            priv.SetClosed();

            return true;
        }

        /*
         * ======================================= 3.4. DESTINATIONS
         * =======================================
         */

        /**
         * Stub for applying any follow-up actions when a company reaches it
         * destinations. Default version: no actions.
         * 
         * @param company
         */
        virtual protected void ReachDestination(PublicCompany company)
        {

        }

        public bool DoReachDestinations(ReachDestinations action)
        {

            List<PublicCompany> destinedCompanies = action.ReachedCompanies;
            if (destinedCompanies != null)
            {
                foreach (PublicCompany company in destinedCompanies)
                {
                    if (company.HasDestination
                        && !company.HasReachedDestination())
                    {
                        company.SetReachedDestination(true);
                        ReportBuffer.Add(this, LocalText.GetText(
                                "DestinationReached", company.Id,
                                company.DestinationHex.Id));
                        // Process any consequences of reaching a destination
                        // (default none)
                        ReachDestination(company);
                    }
                }
            }
            return true;
        }

        /**
         * This is currently a stub, as it is unclear if there is a common rule for
         * setting destination reaching options. See OperatingRound_1856 for a first
         * implementation of such rules.
         */
        virtual protected void SetDestinationActions()
        {

        }

        /*
         * ======================================= 3.5. LOANS
         * =======================================
         */

        protected bool DoTakeLoans(TakeLoans action)
        {

            string errMsg = ValidateTakeLoans(action);

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotTakeLoans",
                        action.CompanyName, action.NumberTaken,
                        Bank.Format(this, action.Price), errMsg));

                return false;
            }

            ExecuteTakeLoans(action);

            return true;
        }

        protected string ValidateTakeLoans(TakeLoans action)
        {

            string errMsg = null;
            PublicCompany company = action.Company;
            string companyName = company.Id;
            int number = action.NumberTaken;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Is company operating?
                if (company != operatingCompany.Value)
                {
                    errMsg =
                            LocalText.GetText("WrongCompany", companyName,
                                    action.CompanyName);
                    break;
                }
                // Does company allow any loans?
                if (company.MaxNumberOfLoans == 0)
                {
                    errMsg = LocalText.GetText("LoansNotAllowed", companyName);
                    break;
                }
                // Does the company exceed the maximum number of loans?
                if (company.MaxNumberOfLoans > 0
                    && company.GetCurrentNumberOfLoans() + number > company.MaxNumberOfLoans)
                {
                    errMsg =
                            LocalText.GetText("MoreLoansNotAllowed", companyName,
                                    company.MaxNumberOfLoans);
                    break;
                }
                break;
            }

            return errMsg;
        }

        protected void ExecuteTakeLoans(TakeLoans action)
        {

            int number = action.NumberTaken;
            int amount = CalculateLoanAmount(number);
            operatingCompany.Value.AddLoans(number);
            Currency.FromBank(amount, operatingCompany.Value);
            if (number == 1)
            {
                ReportBuffer.Add(this, LocalText.GetText("CompanyTakesLoan",
                        operatingCompany.Value.Id, Bank.Format(this,
                                operatingCompany.Value.ValuePerLoan),
                        Bank.Format(this, amount)));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("CompanyTakesLoans",
                        operatingCompany.Value.Id, number, Bank.Format(this,
                                operatingCompany.Value.ValuePerLoan),
                        Bank.Format(this, amount)));
            }

            if (operatingCompany.Value.MaxLoansPerRound > 0)
            {
                int oldLoansThisRound = 0;
                if (loansThisRound == null)
                {
                    loansThisRound = DictionaryState<PublicCompany, int>.Create(this, "loansThisRound");
                }
                else if (loansThisRound.ContainsKey(operatingCompany.Value))
                {
                    oldLoansThisRound = loansThisRound.Get(operatingCompany.Value);
                }
                loansThisRound.Put(operatingCompany.Value, oldLoansThisRound + number);
            }
        }

        protected bool DoRepayLoans(RepayLoans action)
        {
            string errMsg = ValidateRepayLoans(action);

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotRepayLoans",
                        action.CompanyName, action.NumberRepaid,
                        Bank.Format(this, action.Price), errMsg));

                return false;
            }

            int repayment = action.NumberRepaid * operatingCompany.Value.ValuePerLoan;
            if (repayment > 0 && repayment > operatingCompany.Value.Cash)
            {
                // President must contribute
                int remainder = repayment - operatingCompany.Value.Cash;
                Player president = operatingCompany.Value.GetPresident();
                int presCash = president.CashValue;
                if (remainder > presCash)
                {
                    // Start a share selling round
                    int cashToBeRaisedByPresident = remainder - presCash;
                    log.Info("A share selling round must be started as the president cannot pay $"
                             + remainder + " loan repayment");
                    log.Info("President has $" + presCash + ", so $"
                             + cashToBeRaisedByPresident + " must be added");
                    savedAction = action;

                    gameManager.StartShareSellingRound(
                            operatingCompany.Value.GetPresident(),
                            cashToBeRaisedByPresident, operatingCompany.Value,
                            false);
                    return true;
                }
            }

            if (repayment > 0) ExecuteRepayLoans(action);

            return true;
        }

        virtual protected string ValidateRepayLoans(RepayLoans action)
        {
            string errMsg = null;

            return errMsg;
        }

        protected void ExecuteRepayLoans(RepayLoans action)
        {

            int number = action.NumberRepaid;
            int payment;
            int remainder = 0;

            operatingCompany.Value.AddLoans(-number);
            int amount = number * operatingCompany.Value.ValuePerLoan;
            payment = Math.Min(amount, operatingCompany.Value.Cash);
            remainder = amount - payment;
            if (payment > 0)
            {
                string paymentText = Currency.ToBank(operatingCompany.Value, payment);
                ReportBuffer.Add(this, LocalText.GetText(
                        "CompanyRepaysLoans",
                        operatingCompany.Value.Id,
                        paymentText,
                        bank.Currency.Format(amount), // TODO: Do this nicer
                        number,
                        bank.Currency.Format(
                                operatingCompany.Value.ValuePerLoan))); // TODO:
                                                                        // Do
                                                                        // this
                                                                        // nicer
            }
            if (remainder > 0)
            {
                Player president = operatingCompany.Value.GetPresident();
                if (president.CashValue >= remainder)
                {
                    payment = remainder;
                    string paymentText = Currency.ToBank(president, payment);
                    ReportBuffer.Add(this, LocalText.GetText(
                            "CompanyRepaysLoansWithPresCash",
                            operatingCompany.Value.Id,
                            paymentText,
                            bank.Currency.Format(amount), // TODO: Do this
                                                          // nicer
                            number,
                            bank.Currency.Format(
                                    operatingCompany.Value.ValuePerLoan), // TODO:
                                                                          // Do
                                                                          // this
                                                                          // nicer
                            president.Id));
                }
            }
        }

        protected int CalculateLoanAmount(int numberOfLoans)
        {
            return numberOfLoans * operatingCompany.Value.ValuePerLoan;
        }

        // TODO UNUSED??
        // public void payLoanInterest () {
        // int amount = operatingCompany.Value.getCurrentLoanValue()
        // * operatingCompany.Value.getLoanInterestPct() / 100;
        //
        // MoneyModel.cashMove (operatingCompany.Value, bank, amount);
        // DisplayBuffer.add(this, LocalText.GetText("CompanyPaysLoanInterest",
        // operatingCompany.Value.Id,
        // Currency.format(this, amount),
        // operatingCompany.Value.getLoanInterestPct(),
        // operatingCompany.Value.getCurrentNumberOfLoans(),
        // Currency.format(this, operatingCompany.Value.getValuePerLoan())));
        // }

        /*
         * ======================================= 3.6. RIGHTS
         * =======================================
         */

        protected bool DoBuyRight(UseSpecialProperty action)
        {

            string errMsg = null;
            string rightName = "";
            string rightValue = "";
            SpecialRight right = null;
            int cost = 0;

            SpecialProperty sp = action.SpecialProperty;

            while (true)
            {
                if (!(sp is SpecialRight))
                {
                    errMsg = "Wrong right property class: " + sp.ToString();
                    break;
                }
                right = (SpecialRight)sp;
                rightName = right.Name;
                rightValue = right.Value;
                cost = right.Cost;

                if (cost > 0 && cost > operatingCompany.Value.Cash)
                {
                    errMsg = LocalText.GetText("NoMoney");
                    break;
                }
                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotBuyRight",
                        action.CompanyName, rightName,
                        bank.Currency.Format(cost), // TODO: Do this nicer
                        errMsg));

                return false;
            }

            operatingCompany.Value.SetRight(right);
            // TODO: Creates a zero cost transfer if cost == 0
            string costText = Currency.ToBank(operatingCompany.Value, cost);

            ReportBuffer.Add(this, LocalText.GetText("BuysRight",
                    operatingCompany.Value.Id, rightName, costText));

            sp.SetExercised();

            return true;
        }

        /*
         * ======================================= 4. LAYING TILES
         * =======================================
         */

        public bool DoLayTile(LayTile action)
        {
            string errMsg = null;
            int cost = 0;
            SpecialTileLay stl = null;
            bool extra = false;

            PublicCompany company = action.Company;
            string companyName = company.Id;
            Tile tile = action.LaidTile;
            MapHex hex = action.ChosenHex;
            int orientation = action.Orientation;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Must be correct company.
                if (!companyName.Equals(operatingCompany.Value.Id))
                {
                    errMsg =
                            LocalText.GetText("WrongCompany", companyName,
                                    operatingCompany.Value.Id);
                    break;
                }
                // Must be correct step
                if (GetStep() != GameDef.OrStep.LAY_TRACK)
                {
                    errMsg = LocalText.GetText("WrongActionNoTileLay");
                    break;
                }

                if (tile == null) break;

                if (!Phase.GetCurrent(this).IsTileColorAllowed(tile.ColorText))
                {
                    errMsg = LocalText.GetText("TileNotYetAvailable", tile.ToText());
                    break;
                }
                if (tile.FreeCount == 0)
                {
                    errMsg = LocalText.GetText("TileNotAvailable", tile.ToText());
                    break;
                }

                /*
                 * Check if the current tile is allowed via the LayTile allowance.
                 * (currently the set if tiles is always null, which means that this
                 * check is redundant. This may change in the future.
                 */
                if (action != null)
                {
                    List<Tile> tiles = action.GetTiles();
                    if (tiles != null && (tiles.Count > 0) && !tiles.Contains(tile))
                    {
                        errMsg = LocalText.GetText("TileMayNotBeLaidInHex",
                                        tile.ToText(), hex.Id);
                        break;
                    }
                    stl = action.SpecialProperty;
                    if (stl != null) extra = stl.IsExtra;
                }

                /*
                 * If this counts as a normal tile lay, check if the allowed number
                 * of normal tile lays is not exceeded.
                 */
                if (!extra && !ValidateNormalTileLay(tile))
                {
                    errMsg = LocalText.GetText("NumberOfNormalTileLaysExceeded",
                                    tile.ColorText);
                    break;
                }

                // Sort out cost
                if (stl != null && stl.IsFree)
                {
                    cost = 0;
                }
                else
                {
                    cost = hex.GetTileCost();
                    if (stl != null)
                    {
                        cost = Math.Max(0, cost - stl.Discount);
                    }
                }

                // Amount must be non-negative multiple of 10
                if (cost < 0)
                {
                    errMsg = LocalText.GetText("NegativeAmountNotAllowed",
                                    Bank.Format(this, cost));
                    break;
                }
                if (cost % 10 != 0)
                {
                    errMsg = LocalText.GetText("AmountMustBeMultipleOf10",
                                    Bank.Format(this, cost));
                    break;
                }
                // Does the company have the money?
                if (cost > operatingCompany.Value.Cash)
                {
                    errMsg = LocalText.GetText("NotEnoughMoney", companyName,
                            Bank.Format(this, operatingCompany.Value.Cash), Bank.Format(this, cost));
                    break;
                }
                break;
            }
            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotLayTileOn",
                        companyName, tile.ToText(), hex.Id,
                        Bank.Format(this, cost), errMsg));
                return false;
            }

            /* End of validation, start of execution */

            if (tile != null)
            {
                string costText = null;
                if (cost > 0)
                {
                    costText = Currency.ToBank(operatingCompany.Value, cost);
                }
                operatingCompany.Value.LayTile(hex, tile, orientation, cost);

                if (costText == null)
                {
                    ReportBuffer.Add(
                            this,
                            LocalText.GetText(
                                    "LaysTileAt",
                                    companyName,
                                    tile.ToText(),
                                    hex.Id,
                                    hex.GetOrientationName(HexSide.Get(orientation))));
                }
                else
                {
                    ReportBuffer.Add(this, LocalText.GetText("LaysTileAtFor",
                            companyName, tile.ToText(), hex.Id,
                            hex.GetOrientationName(HexSide.Get(orientation)),
                            costText));
                }
                hex.Upgrade(action);

                // Was a special property used?
                if (stl != null)
                {
                    stl.SetExercised();
                    // currentSpecialTileLays.remove(action);
                    log.Debug("This was a special tile lay, "
                              + (extra ? "" : " not") + " extra");

                }
                if (!extra)
                {
                    log.Debug("This was a normal tile lay");
                    RegisterNormalTileLay(tile);
                }
            }

            if (tile == null || !AreTileLaysPossible)
            {
                NextStep();
            }

            return true;
        }

        public bool LayTileCorrection(LayTile action)
        {

            Tile tile = action.LaidTile;
            MapHex hex = action.ChosenHex;
            int orientation = action.Orientation;

            string errMsg = null;
            // tiles have external id defined
            if (tile != null
                    && tile != hex.CurrentTile
                    && tile.FreeCount == 0)
            {
                errMsg = LocalText.GetText("TileNotAvailable",
                                tile.ToText());
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CorrectMapCannotLayTile",
                        tile.ToText(),
                        hex.Id,
                        errMsg));
                ;
                return false;
            }

            // lays tile
            hex.Upgrade(action);

            string msg = LocalText.GetText("CorrectMapLaysTileAt",
                    tile.ToText(), hex.Id, hex.GetOrientationName(orientation));
            ReportBuffer.Add(this, msg);
            return true;
        }

        protected bool ValidateNormalTileLay(Tile tile)
        {
            return CheckNormalTileLay(tile, false);
        }

        protected void RegisterNormalTileLay(Tile tile)
        {
            CheckNormalTileLay(tile, true);
        }

        protected bool CheckNormalTileLay(Tile tile, bool update)
        {

            // Unspecified tile (e.g. 1889 D private, which is free on mountains)
            if (tile == null)
            {
                return !tileLaysPerColor.IsEmpty();
            }

            string color = tile.ColorText;
            if (!tileLaysPerColor.ContainsKey(color)) return false;

            int oldAllowedNumber = tileLaysPerColor.Get(color);
            if (oldAllowedNumber <= 0) return false;

            if (update) UpdateAllowedTileColors(color, oldAllowedNumber);
            return true;
        }

        /*
         * We will assume that in all cases the following assertions hold: 1. If the
         * allowed number for the color of the just laid tile reaches zero, all
         * normal tile lays have been consumed. 2. If any color is laid, no
         * different colors may be laid. THIS MAY NOT BE TRUE FOR ALL GAMES!
         */

        protected void UpdateAllowedTileColors(string color, int oldAllowedNumber)
        {

            if (oldAllowedNumber <= 1)
            {
                tileLaysPerColor.Clear();
                log.Debug("No more normal tile lays allowed");
                // currentNormalTileLays.clear();// Shouldn't be needed anymore ??
            }
            else
            {
                List<string> colorsToRemove = new List<string>();
                foreach (string key in tileLaysPerColor.ViewKeys())
                {
                    if (color.Equals(key))
                    {
                        tileLaysPerColor.Put(key, oldAllowedNumber - 1);
                    }
                    else
                    {
                        colorsToRemove.Add(key);
                    }
                }
                // Two-step removal to prevent ConcurrentModificatioonException.
                foreach (string key in colorsToRemove)
                {
                    tileLaysPerColor.Remove(key);
                }
                log.Debug((oldAllowedNumber - 1) + " additional " + color
                          + " tile lays allowed; no other colours");
            }
        }

        /**
         * Create a List of allowed normal tile lays (see LayTile class). This
         * method should be called only once per company turn in an OR: at the start
         * of the tile laying step.
         */
        protected void InitNormalTileLays()
        {
            // duplicate the phase colors
            Dictionary<string, int> newTileColors = new Dictionary<string, int>();
            foreach (string color in Phase.GetCurrent(this).GetTileColors())
            {
                int allowedNumber = operatingCompany.Value.GetNumberOfTileLays(color);
                // Replace the null map value with the allowed number of lays
                newTileColors[color] = allowedNumber;
            }
            // store to state
            tileLaysPerColor.InitFromMap(newTileColors);
        }

        protected List<LayTile> GetNormalTileLays(bool display)
        {

            /* Normal tile lays */
            List<LayTile> currentNormalTileLays = new List<LayTile>();

            // Check which colors can still be laid
            Dictionary<string, int> remainingTileLaysPerColour = new Dictionary<string, int>();

            int lays = 0;
            foreach (string colorName in tileLaysPerColor.ViewKeys())
            {
                lays = tileLaysPerColor.Get(colorName);
                if (lays != 0)
                {
                    remainingTileLaysPerColour[colorName] = lays;
                }
            }
            if (remainingTileLaysPerColour.Count > 0)
            {
                currentNormalTileLays.Add(new LayTile(remainingTileLaysPerColour));
            }

            // NOTE: in a later stage tile lays will be specified per hex or set of
            // hexes.

            if (display)
            {
                int size = currentNormalTileLays.Count;
                if (size == 0)
                {
                    log.Debug("No normal tile lays");
                }
                else
                {
                    foreach (LayTile tileLay in currentNormalTileLays)
                    {
                        log.Debug("Normal tile lay: " + tileLay.ToString());
                    }
                }
            }
            return currentNormalTileLays;
        }

        /**
         * Create a List of allowed special tile lays (see LayTile class). This
         * method should be called before each user action in the tile laying step.
         */
        protected List<LayTile> GetSpecialTileLays(bool display)
        {

            /* Special-property tile lays */
            List<LayTile> currentSpecialTileLays = new List<LayTile>();

            if (operatingCompany.Value.CanUseSpecialProperties)
            {

                foreach (SpecialTileLay stl in GetSpecialProperties<SpecialTileLay>())
                {

                    LayTile layTile = new LayTile(stl);
                    if (ValidateSpecialTileLay(layTile))
                        currentSpecialTileLays.Add(layTile);
                }
            }

            if (display)
            {
                int size = currentSpecialTileLays.Count;
                if (size == 0)
                {
                    log.Debug("No special tile lays");
                }
                else
                {
                    foreach (LayTile tileLay in currentSpecialTileLays)
                    {
                        log.Debug("Special tile lay: " + tileLay.ToString());
                    }
                }
            }

            return currentSpecialTileLays;
        }

        /**
         * Prevalidate a special tile lay. <p>During prevalidation, the action may
         * be updated (i.e. restricted). TODO <p>Note: The name of this method may
         * suggest that it can also be used for postvalidation (i.e. to validate the
         * action after the player has selected it). This is not yet the case, but
         * it is conceivable that this method can be extended to cover
         * postvalidation as well. Postvalidation is really a different process,
         * which in this context has not yet been considered in detail.
         * 
         * @param layTile A LayTile object embedding a SpecialTileLay property. Any
         * other LayTile objects are rejected. The object may be changed by this
         * method.
         * @return TRUE if allowed.
         */
        protected bool ValidateSpecialTileLay(LayTile layTile)
        {

            if (layTile == null) return false;

            SpecialProperty sp = layTile.SpecialProperty;
            if (sp == null || !(sp is SpecialTileLay)) return false;

            SpecialTileLay stl = (SpecialTileLay)sp;

            if (!stl.IsExtra
                // If the special tile lay is not extra, it is only allowed if
                // normal tile lays are also (still) allowed
                && !CheckNormalTileLay(stl.Tile, false)) return false;

            Tile tile = stl.Tile;

            // What colors can be laid in the current phase?
            List<string> phaseColors = Phase.GetCurrent(this).GetTileColors();

            // Which tile color(s) are specified explicitly...
            string[] stlc = stl.TileColors;
            if ((stlc == null || stlc.Length == 0) && tile != null)
            {
                // ... or implicitly
                stlc = new string[] { tile.ColorText };
            }

            // Which of the specified tile colors can really be laid now?
            List<string> layableColors;
            if (stlc == null)
            {
                layableColors = phaseColors;
            }
            else
            {
                layableColors = new List<string>();
                foreach (string color in stlc)
                {
                    if (phaseColors.Contains(color)) layableColors.Add(color);
                }
                if (layableColors.Count == 0) return false;
            }

            // If any locations are specified, check if tile or color(s) can be
            // laid there.
            Dictionary<string, int> tc = new Dictionary<string, int>();
            List<MapHex> hexes = stl.Locations;
            List<MapHex> remainingHexes = null;
            List<string> remainingColors = null;
            int cash = operatingCompany.Value.Cash;

            if (hexes != null)
            {
                remainingHexes = new List<MapHex>();
                remainingColors = new List<string>();
            }
            foreach (string color in layableColors)
            {
                if (hexes != null)
                {
                    foreach (MapHex hex in hexes)
                    {
                        int cost = Math.Max(0, hex.GetTileCost() - stl.Discount);
                        // Check if the company can pay any costs (if not free)
                        if (!stl.IsFree && cash < cost) continue;

                        // At least one hex does not have that color yet
                        // TODO: Check if this can be rewritten in a simpler fashion
                        // using TileColours directly
                        if (hex.CurrentTile.ColorNumber + 1 == TileColor.ValueOf(color).Number)
                        {
                            tc[color] = 1;
                            remainingColors.Add(color);
                            remainingHexes.Add(hex);
                            continue;
                        }
                    }
                }
                else
                {
                    tc[color] = 1;
                }
            }
            if (tc.Count > 0) layTile.SetTileColors(tc);

            if (hexes != null)
            {
                if (remainingHexes.Count == 0) return false;
                layTile.Locations = remainingHexes;
            }

            return true;
        }

        protected bool AreTileLaysPossible
        {
            get
            {
                return !tileLaysPerColor.IsEmpty()
                       || (GetSpecialTileLays(false).Count > 0);
            }
        }

        /**
         * Reports if a tile lay is allowed by a certain company on a certain hex
         * <p> This method can be used both in restricting possible actions and in
         * validating submitted actions. <p> Currently, only a few standard checks
         * are included. This method can be extended to perform other generic
         * checks, such as if a route exists, and possibly in subclasses for
         * game-specific checks.
         * 
         * @param company The company laying a tile.
         * @param hex The hex on which a tile is laid.
         * @param orientation The orientation in which the tile is laid (-1 is any).
         */
        protected bool IsTileLayAllowed(PublicCompany company, MapHex hex, int orientation)
        {
            bool result = true;

            result = GameSpecificTileLayAllowed(company, hex, orientation);
            return result;
        }

        protected bool GameSpecificTileLayAllowed(PublicCompany company, MapHex hex, int orientation)
        {
            return hex.IsBlockedByPrivateCompany;
        }

        /*
         * ======================================= 5. TOKEN LAYING 5.1. BASE TOKENS
         * =======================================
         */

        public bool DoLayBaseToken(LayBaseToken action)
        {

            string errMsg = null;
            int cost = 0;
            SpecialBaseTokenLay stl = null;
            bool extra = false;

            MapHex hex = action.ChosenHex;
            Stop stop = action.ChosenStop;

            PublicCompany company = action.Company;
            string companyName = company.Id;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Must be correct step (exception: home base lay & some special
                // token lay)
                if (GetStep() != GameDef.OrStep.LAY_TOKEN
                    && action.LayBaseTokenType != LayBaseToken.HOME_CITY
                    && action.LayBaseTokenType != LayBaseToken.SPECIAL_PROPERTY
                    && action.LayBaseTokenType != LayBaseToken.CORRECTION)
                {
                    errMsg = LocalText.GetText("WrongActionNoTokenLay");
                    break;
                }

                if (company.GetNumberOfFreeBaseTokens() == 0)
                {
                    errMsg = LocalText.GetText("HasNoTokensLeft", companyName);
                    break;
                }

                if (!IsTokenLayAllowed(company, hex, stop))
                {
                    errMsg = LocalText.GetText("BaseTokenSlotIsReserved");
                    break;
                }

                if (!stop.HasTokenSlotsLeft)
                {
                    errMsg = LocalText.GetText("CityHasNoEmptySlots");
                    break;
                }

                /*
                 * TODO: the below condition holds for 1830. in some games, separate
                 * cities on one tile may hold tokens of the same company; this case
                 * is not yet covered.
                 */
                if (hex.HasTokenOfCompany(company))
                {
                    errMsg = LocalText.GetText("TileAlreadyHasToken", hex.Id, companyName);
                    break;
                }

                if (action != null)
                {
                    List<MapHex> locations = action.Locations;
                    if (locations != null && locations.Count > 0
                        && !locations.Contains(hex) && !locations.Contains(null))
                    {
                        errMsg = LocalText.GetText("TokenLayingHexMismatch",
                                        hex.Id, action.LocationNameString);
                        break;
                    }
                    stl = (SpecialBaseTokenLay)action.GetSpecialProperty();
                    if (stl != null) extra = stl.IsExtra;
                }

                cost = company.GetBaseTokenLayCost(hex);
                if (stl != null && stl.IsFree) cost = 0;

                // Does the company have the money?
                if (cost > company.Cash)
                {
                    errMsg = LocalText.GetText("NotEnoughMoney", companyName,
                                    Bank.Format(this, company.Cash),
                                    Bank.Format(this, cost));
                    break;
                }
                break;
            }
            if (errMsg != null)
            {
                DisplayBuffer.Add(
                        this,
                        LocalText.GetText("CannotLayBaseTokenOn", companyName,
                                hex.Id, Bank.Format(this, cost), errMsg));
                return false;
            }

            /* End of validation, start of execution */

            if (hex.LayBaseToken(company, stop))
            {
                /* TODO: the false return value must be impossible. */

                company.LayBaseToken(hex, cost);

                // If this is a home base token lay, stop here
                if (action.LayBaseTokenType == LayBaseToken.HOME_CITY)
                {
                    return true;
                }

                StringBuilder text = new StringBuilder();
                if (action.IsCorrection)
                {
                    text.Append(LocalText.GetText("CorrectionPrefix"));
                }
                if (cost > 0)
                {
                    string costText = Currency.ToBank(company, cost);
                    text.Append(LocalText.GetText("LAYS_TOKEN_ON", companyName,
                                    hex.Id, costText));
                    text.Append(" " + stop.ToText());
                }
                else
                {
                    text.Append(LocalText.GetText("LAYS_FREE_TOKEN_ON",
                            companyName, hex.Id));
                }
                ReportBuffer.Add(this, text.ToString());

                // Was a special property used?
                if (stl != null)
                {
                    stl.SetExercised();
                    currentSpecialTokenLays.Remove(action);
                    log.Debug("This was a special token lay, "
                              + (extra ? "" : " not") + " extra");
                }

                // Jump out if we aren't in the token laying step or it is a correction lay
                if (GetStep() != GameDef.OrStep.LAY_TOKEN || action.IsCorrection)
                {
                    return true;
                }

                if (!extra)
                {
                    currentNormalTokenLays.Clear();
                    log.Debug("This was a normal token lay");
                }

                if (currentNormalTokenLays.Count == 0)
                {
                    log.Debug("No more normal token lays are allowed");
                }
                else if (operatingCompany.Value.GetNumberOfFreeBaseTokens() == 0)
                {
                    log.Debug("Normal token lay allowed by no more tokens");
                    currentNormalTokenLays.Clear();
                }
                else
                {
                    log.Debug("A normal token lay is still allowed");
                }
                SetSpecialTokenLays();
                log.Debug("There are now " + currentSpecialTokenLays.Count
                          + " special token lay objects");
                if ((currentNormalTokenLays.Count == 0)
                    && (currentSpecialTokenLays.Count == 0))
                {
                    NextStep();
                }
            }

            return true;
        }

        /**
         * Reports if a token lay is allowed by a certain company on a certain hex
         * and city <p> This method can be used both in restricting possible actions
         * and in validating submitted actions. <p> Currently, only a few standard
         * checks are included. This method can be extended to perform other generic
         * checks, such as if a route exists, and possibly in subclasses for
         * game-specific checks.
         *
         * @param company The company laying a tile.
         * @param hex The hex on which a tile is laid.
         * @param station The number of the station/city on which the token is to be
         * laid (0 if any or immaterial).
         */
        protected bool IsTokenLayAllowed(PublicCompany company, MapHex hex, Stop stop)
        {
            return !hex.IsBlockedForTokenLays(company, stop);
        }

        protected void SetNormalTokenLays()
        {
            /* Normal token lays */
            currentNormalTokenLays.Clear();

            /* For now, we allow one token of the currently operating company */
            if (operatingCompany.Value.GetNumberOfFreeBaseTokens() > 0)
            {
                currentNormalTokenLays.Add(new LayBaseToken((List<MapHex>)null));
            }
        }

        /**
         * Create a List of allowed special token lays (see LayToken class). This
         * method should be called before each user action in the base token laying
         * step. TODO: Token preparation is practically identical to Tile
         * preparation, perhaps the two can be merged to one generic procedure.
         */
        protected void SetSpecialTokenLays()
        {
            /* Special-property tile lays */
            currentSpecialTokenLays.Clear();

            PublicCompany company = operatingCompany.Value;
            if (!company.CanUseSpecialProperties) return;
            // Check if the company still has tokens
            if (company.GetNumberOfFreeBaseTokens() == 0) return;

            /*
             * In 1835, this only applies to major companies. TODO: For now,
             * hardcode this, but it must become configurable later.
             */
            // Removed EV 24-11-2011 - entirely redundant; why did I ever do this??
            // if (operatingCompany.get().getType().getName().equals("Minor"))
            // return;

            foreach (SpecialBaseTokenLay stl in GetSpecialProperties<SpecialBaseTokenLay>())
            {
                // If the special tile lay is not extra, it is only allowed if
                // normal tile lays are also (still) allowed
                if (stl.IsExtra || (currentNormalTokenLays.Count > 0))
                {
                    // If this STL is location specific, check if there
                    // isn't already a token of this company or if it is blocked
                    List<MapHex> locations = stl.Locations;
                    if (locations != null && locations.Count > 0)
                    {
                        bool canLay = false;
                        foreach (MapHex location in locations)
                        {
                            if (location.HasTokenOfCompany(company))
                            {
                                continue;
                            }
                            foreach (Stop stop in location.Stops)
                            {
                                canLay = !location.IsBlockedForTokenLays(company, stop);
                            }
                        }
                        if (!canLay) continue;
                    }
                    currentSpecialTokenLays.Add(new LayBaseToken(stl));
                }
            }
        }

        /*
         * ======================================= 5.2. BONUS TOKENS
         * =======================================
         */

        public bool DoLayBonusToken(LayBonusToken action)
        {
            string errMsg = null;
            int cost = 0; // currently costs are always zero
            SpecialBonusTokenLay stl = null;

            MapHex hex = action.ChosenHex;
            BonusToken token = action.Token;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                MapHex location = action.ChosenHex;
                if (location != hex)
                {
                    errMsg = LocalText.GetText("TokenLayingHexMismatch",
                                    hex.Id, location.Id);
                    break;
                }
                stl = (SpecialBonusTokenLay)action.GetSpecialProperty();
                break;
            }
            if (errMsg != null)
            {
                DisplayBuffer.Add(this,
                        LocalText.GetText("CannotLayBonusTokenOn", token.Id,
                                hex.Id, Bank.Format(this, cost), errMsg));
                return false;
            }

            /* End of validation, start of execution */

            if (hex.LayBonusToken(token, GetRoot.PhaseManager))
            {
                /* TODO: the false return value must be impossible. */

                operatingCompany.Value.AddBonus(
                        new Bonus(operatingCompany.Value, token.Id,
                                token.Value, new List<MapHex>() { hex }));
                token.SetUser(operatingCompany.Value);

                ReportBuffer.Add(this, LocalText.GetText("LaysBonusTokenOn",
                        operatingCompany.Value.Id, token.Id,
                        Bank.Format(this, token.Value), hex.Id));

                // Was a special property used?
                if (stl != null)
                {
                    stl.SetExercised();
                    // #FIXME_invalid_cast_likely
                    currentSpecialTokenLays.Remove((LayBaseToken)(LayToken)action);
                }

            }

            return true;
        }

        public bool DoBuyBonusToken(BuyBonusToken action)
        {
            string errMsg = null;
            int cost;
            SellBonusToken sbt = null;
            IMoneyOwner seller = null;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                sbt = action.SpecialProperty;
                cost = sbt.Price;
                IOwner from = sbt.GetSeller();
                // TODO: Remove redundancy use a generalized check
                if (from is BankPortfolio)
                {
                    seller = bank;
                }
                else
                {
                    seller = (IMoneyOwner)from;
                }

                // Does the company have the money?
                if (cost > operatingCompany.Value.Cash)
                {
                    errMsg = LocalText.GetText("NotEnoughMoney",
                                    operatingCompany.Value.Id, Bank.Format(
                                            this,
                                            operatingCompany.Value.Cash),
                                    Bank.Format(this, cost));
                    break;
                }
                break;
            }
            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotBuyBonusToken",
                        operatingCompany.Value.Id, sbt.Id,
                        seller.Id, bank.Currency.Format(cost), // TODO: Do
                                                               // this
                                                               // nicer
                        errMsg));
                return false;
            }

            /* End of validation, start of execution */

            // TODO: Is text of cost used below?
            Currency.Wire(operatingCompany.Value, cost, seller);

            operatingCompany.Value.AddBonus(new Bonus(operatingCompany.Value,
                sbt.Id, sbt.Value, sbt.Locations));

            ReportBuffer.Add(this, LocalText.GetText("BuysBonusTokenFrom",
                    operatingCompany.Value.Id, sbt.Name,
                    bank.Currency.Format(sbt.Value), // TODO: Do this
                                                     // nicer
                    seller.Id, bank.Currency.Format(sbt.Price))); // TODO:
                                                                  // Do
                                                                  // this
                                                                  // nicer

            sbt.SetExercised();

            return true;
        }

        /**
         * TODO Should be merged with setSpecialTokenLays() in the future.
         * Assumptions: 1. Bonus tokens can be laid anytime during the OR. 2. Bonus
         * token laying is always extra. TODO This assumptions will be made
         * configurable conditions.
         */
        protected void SetBonusTokenLays()
        {

            foreach (SpecialBonusTokenLay stl in GetSpecialProperties<SpecialBonusTokenLay>())
            {
                possibleActions.Add(new LayBonusToken(stl, stl.Token));
            }
        }

        /*
         * ======================================= 6. REVENUE AND DIVIDEND
         * =======================================
         */

        public bool SetRevenueAndDividend(SetDividend action)
        {

            string errMsg = ValidateSetRevenueAndDividend(action);

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotProcessRevenue",
                        Bank.Format(this, action.ActualRevenue),
                        action.CompanyName, errMsg));
                return false;
            }

            ReportBuffer.Add(this, LocalText.GetText("CompanyRevenue",
                    action.CompanyName,
                    Bank.Format(this, action.ActualRevenue)));

            int remainingAmount = CheckForDeductions(action);
            if (remainingAmount < 0)
            {
                // A share selling round will be run to raise cash to pay debts
                return true;
            }

            ExecuteSetRevenueAndDividend(action);

            return true;

        }

        protected string ValidateSetRevenueAndDividend(SetDividend action)
        {
            string errMsg = null;
            PublicCompany company;
            string companyName;
            int amount = 0;
            int revenueAllocation = -1;

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Must be correct company.
                company = action.Company;
                companyName = company.Id;
                if (company != operatingCompany.Value)
                {
                    errMsg = LocalText.GetText("WrongCompany", companyName,
                                    operatingCompany.Value.Id);
                    break;
                }
                // Must be correct step
                if (GetStep() != GameDef.OrStep.CALC_REVENUE)
                {
                    errMsg = LocalText.GetText("WrongActionNoRevenue");
                    break;
                }

                // Amount must be non-negative multiple of 10
                amount = action.ActualRevenue;
                if (amount < 0)
                {
                    errMsg = LocalText.GetText("NegativeAmountNotAllowed", amount.ToString());
                    break;
                }
                if (amount % 10 != 0)
                {
                    errMsg = LocalText.GetText("AmountMustBeMultipleOf10", amount.ToString());
                    break;
                }

                // Check chosen revenue distribution
                if (amount > 0)
                {
                    // Check the allocation type index (see SetDividend for values)
                    revenueAllocation = action.RevenueAllocation;
                    if (revenueAllocation < 0
                        || revenueAllocation >= SetDividend.NUM_OPTIONS)
                    {
                        errMsg = LocalText.GetText("InvalidAllocationTypeIndex", revenueAllocation.ToString());
                        break;
                    }

                    // Validate the chosen allocation type
                    int[] allowedAllocations = ((SetDividend)selectedAction).GetAllowedAllocations();
                    bool valid = false;
                    foreach (int aa in allowedAllocations)
                    {
                        if (revenueAllocation == aa)
                        {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid)
                    {
                        errMsg = LocalText.GetText(SetDividend.GetAllocationNameKey(revenueAllocation));
                        break;
                    }
                }
                else
                {
                    // If there is no revenue, use withhold.
                    action.RevenueAllocation = SetDividend.WITHHOLD;
                }

                if (amount == 0 && operatingCompany.Value.GetNumberOfTrains() == 0)
                {
                    DisplayBuffer.Add(this, LocalText.GetText(
                            "RevenueWithNoTrains",
                            operatingCompany.Value.Id, Bank.Format(this, 0)));
                }

                break;
            }

            return errMsg;
        }

        protected void ExecuteSetRevenueAndDividend(SetDividend action)
        {
            int amount = action.ActualRevenue;
            int revenueAllocation = action.RevenueAllocation;

            operatingCompany.Value.SetLastRevenue(amount);
            operatingCompany.Value.SetLastRevenueAllocation(revenueAllocation);

            // Pay any debts from treasury, revenue and/or president's cash
            // The remaining dividend may be less that the original income
            amount = ExecuteDeductions(action);

            if (amount == 0)
            {
                ReportBuffer.Add(this, LocalText.GetText(
                        "CompanyDoesNotPayDividend",
                        operatingCompany.Value.Id));
                Withhold(amount);
            }
            else if (revenueAllocation == SetDividend.PAYOUT)
            {

                ReportBuffer.Add(this,
                        LocalText.GetText("CompanyPaysOutFull",
                                operatingCompany.Value.Id,
                                Bank.Format(this, amount)));

                Payout(amount);
            }
            else if (revenueAllocation == SetDividend.SPLIT)
            {
                ReportBuffer.Add(this,
                        LocalText.GetText("CompanySplits",
                                operatingCompany.Value.Id,
                                Bank.Format(this, amount)));

                SplitRevenue(amount);
            }
            else if (revenueAllocation == SetDividend.WITHHOLD)
            {

                ReportBuffer.Add(this,
                        LocalText.GetText("CompanyWithholds",
                                operatingCompany.Value.Id,
                                Bank.Format(this, amount)));

                Withhold(amount);
            }

            // Rust any obsolete trains
            operatingCompany.Value.PortfolioModel.RustObsoleteTrains();

            // We have done the payout step, so continue from there
            NextStep(GameDef.OrStep.PAYOUT);
        }

        /**
         * Distribute the dividend amongst the shareholders.
         *
         * @param amount
         */
        public void Payout(int amount)
        {
            if (amount == 0) return;

            int part;
            int shares;

            Dictionary<IMoneyOwner, int> sharesPerRecipient = CountSharesPerRecipient();

            // Calculate, round up, report and add the cash

            // Define a precise sequence for the reporting
            var recipientSet = sharesPerRecipient.Keys;
            foreach (IMoneyOwner recipient in SequenceUtil.SortCashHolders(recipientSet))
            {
                if (recipient is Bank) continue;
                shares = (sharesPerRecipient[recipient]);
                if (shares == 0) continue;
                part = (int)Math.Ceiling(amount * shares
                                        * operatingCompany.Value.GetShareUnit()
                                        / 100.0);

                string partText = Currency.FromBank(part, recipient);
                ReportBuffer.Add(this, LocalText.GetText("Payout",
                        recipient.Id, partText, shares,
                        operatingCompany.Value.GetShareUnit()));
            }

            // Move the token
            operatingCompany.Value.Payout(amount);

        }

        protected Dictionary<IMoneyOwner, int> CountSharesPerRecipient()
        {
            Dictionary<IMoneyOwner, int> sharesPerRecipient = new Dictionary<IMoneyOwner, int>();

            // Changed to accommodate the CGR 5% share roundup rule.
            // For now it is assumed, that actual payouts are always rounded up
            // (the withheld half of split revenues is not handled here, see
            // splitRevenue()).

            // First count the shares per recipient
            foreach (PublicCertificate cert in operatingCompany.Value.GetCertificates())
            {
                IMoneyOwner recipient = GetBeneficiary(cert);
                if (!sharesPerRecipient.ContainsKey(recipient))
                {
                    sharesPerRecipient[recipient] = cert.GetShares();
                }
                else
                {
                    sharesPerRecipient[recipient] =
                            sharesPerRecipient[recipient] + cert.GetShares();
                }
            }
            return sharesPerRecipient;
        }

        /** Who gets the per-share revenue? */
        protected IMoneyOwner GetBeneficiary(PublicCertificate cert)
        {
            IMoneyOwner beneficiary;

            // Special cases apply if the holder is the IPO or the Pool
            if (operatingCompany.Value.PaysOutToTreasury(cert))
            {
                beneficiary = operatingCompany.Value;
            }
            else if (cert.Owner is IMoneyOwner)
            {
                beneficiary = (IMoneyOwner)cert.Owner;
            }
            else
            { // TODO: check if this is a correct assumption that otherwise
              // the money goes to the bank
                beneficiary = bank;
            }
            return beneficiary;
        }

        /**
         * Withhold a given amount of revenue (and store it).
         *
         * @param The revenue amount.
         */
        public void Withhold(int amount)
        {
            PublicCompany company = operatingCompany.Value;

            // Payout revenue to company
            Currency.FromBank(amount, company);

            // Move the token
            company.Withhold(amount);

            if (!company.HasStockPrice) return;

            // Check if company has entered a closing area
            StockSpace newSpace = company.GetCurrentSpace();
            if (newSpace.ClosesCompany && company.CanClose)
            {
                company.SetClosed();
                ReportBuffer.Add(this, LocalText.GetText("CompanyClosesAt",
                        company.Id, newSpace.Id));
                FinishTurn();
                return;
            }
        }

        /**
         * Split a dividend. TODO Optional rounding down the payout
         *
         * @param amount
         */
        public void SplitRevenue(int amount)
        {

            if (amount > 0)
            {
                // Withhold half of it
                // For now, hardcode the rule that payout is rounded up.
                int numberOfShares = operatingCompany.Value.GetNumberOfShares();
                int withheld = (amount / (2 * numberOfShares)) * numberOfShares;
                string withheldText = Currency.FromBank(withheld, operatingCompany.Value);

                ReportBuffer.Add(this, LocalText.GetText("RECEIVES",
                        operatingCompany.Value.Id, withheldText));

                // Payout the remainder
                int payed = amount - withheld;
                Payout(payed);
            }

        }

        /** Default version, to be overridden if need be */
        protected int CheckForDeductions(SetDividend action)
        {
            return action.ActualRevenue;
        }

        /** Default version, to be overridden if need be */
        protected int ExecuteDeductions(SetDividend action)
        {
            return action.ActualRevenue;
        }

        protected bool ExecuteOperatingCost(OperatingCost action)
        {

            string companyName = action.CompanyName;
            OperatingCost.OCTypes typeOC = action.OCType;

            int amount = action.Amount;

            string errMsg = null;

            while (true)
            {
                // Must be correct company.
                if (!companyName.Equals(operatingCompany.Value.Id))
                {
                    errMsg =
                            LocalText.GetText("WrongCompany", companyName,
                                    operatingCompany.Value.Id);
                    break;
                }
                // amount is available
                if ((amount + operatingCompany.Value.Cash) < 0)
                {
                    errMsg =
                            LocalText.GetText("NotEnoughMoney", companyName,
                                    Bank.Format(this,
                                            operatingCompany.Value.Cash),
                                    Bank.Format(this, amount));
                    break;
                }
                if (typeOC == OperatingCost.OCTypes.LAY_BASE_TOKEN
                    && operatingCompany.Value.GetNumberOfFreeBaseTokens() == 0)
                {
                    errMsg = LocalText.GetText("HasNoTokensLeft", companyName);
                    break;
                }
                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("OCExecutionError", companyName, errMsg));
                return false;
            }

            string cashText = null;
            if (amount > 0)
            {
                // positive amounts: remove cash from cashholder
                cashText = Currency.ToBank(operatingCompany.Value, amount);
            }
            else if (amount < 0)
            {
                // negative amounts: add cash to cashholder
                cashText = Currency.FromBank(-amount, operatingCompany.Value);
            }

            if (typeOC == OperatingCost.OCTypes.LAY_TILE)
            {
                operatingCompany.Value.LayTilenNoMapMode(amount);
                ReportBuffer.Add(this, LocalText.GetText("OCLayTileExecuted",
                        operatingCompany.Value.Id, cashText));
            }
            if (typeOC == OperatingCost.OCTypes.LAY_BASE_TOKEN)
            {
                // move token to Bank
                BaseToken token = operatingCompany.Value.GetNextBaseToken();
                if (token == null)
                {
                    log.Error("Company " + operatingCompany.Value.Id
                              + " has no free token");
                    return false;
                }
                else
                {
                    // FIXME: Check where to lay the base tokens in NoMapMode
                    // (bank.getUnavailable().addBonusToken(token));
                }
                operatingCompany.Value.LayBaseTokennNoMapMode(amount);
                ReportBuffer.Add(this, LocalText.GetText("OCLayBaseTokenExecuted",
                        operatingCompany.Value.Id, cashText));
            }

            return true;
        }

        protected void PrepareRevenueAndDividendAction()
        {

            // There is only revenue if there are any trains
            if (operatingCompany.Value.CanRunTrains())
            {
                int[] allowedRevenueActions =
                        operatingCompany.Value.IsSplitAlways
                                ? new int[] { SetDividend.SPLIT }
                                : operatingCompany.Value.IsSplitAllowed
                                        ? new int[] { SetDividend.PAYOUT,
                                            SetDividend.SPLIT,
                                            SetDividend.WITHHOLD } : new int[] {
                                            SetDividend.PAYOUT,
                                            SetDividend.WITHHOLD };

                possibleActions.Add(new SetDividend(
                        operatingCompany.Value.GetLastRevenue(), true,
                        allowedRevenueActions));
            }
        }

        protected void PrepareNoMapActions()
        {
            // LayTile Actions
            foreach (int tc in mapManager.PossibleTileCosts())
            {
                if (tc <= operatingCompany.Value.Cash)
                    possibleActions.Add(new OperatingCost(
                            OperatingCost.OCTypes.LAY_TILE, tc, false));
            }

            // LayBaseToken Actions
            if (operatingCompany.Value.GetNumberOfFreeBaseTokens() != 0)
            {
                HashSet<int> baseCosts = operatingCompany.Value.GetBaseTokenLayCosts();

                // change to set to allow for identity and ordering
                List<int> costsSet = new List<int>(); //new TreeSet<int>();
                foreach (int cost in baseCosts)
                {
                    if (!(cost == 0 && baseCosts.Count != 1)) // fix for sequence
                                                              // based home token
                    {
                        costsSet.Add(cost);
                    }
                }

                costsSet.Sort();

                // SpecialBaseTokenLay Actions - workaround for a better handling of
                // those later
                foreach (SpecialBaseTokenLay stl in GetSpecialProperties<SpecialBaseTokenLay>())
                {
                    log.Debug("Special tokenlay property: " + stl);
                    if (stl.IsFree)
                    {
                        costsSet.Add(0);
                    }
                }

                foreach (int cost in costsSet)
                {
                    // distance method returns home base, but in sequence costsSet can be zero
                    if (cost <= operatingCompany.Value.Cash)
                    {
                        possibleActions.Add(new OperatingCost(OperatingCost.OCTypes.LAY_BASE_TOKEN, cost, false));
                    }
                }
            }

            // Default OperatingCost Actions
            // possibleActions.add(new OperatingCost(
            // OperatingCost.OCType.LAY_TILE, 0, true
            // ));
            // if (operatingCompany.getObject().getNumberOfFreeBaseTokens() != 0
            // && operatingCompany.getObject().getBaseTokenLayCost(null) != 0) {
            // possibleActions.add(new
            // OperatingCost(OperatingCost.OCType.LAY_BASE_TOKEN, 0, true));
            // }

        }

        /*
         * ======================================= 7. TRAIN PURCHASING
         * =======================================
         */

        public bool DoBuyTrain(BuyTrain action)
        {

            Train train = action.Train;
            PublicCompany company = action.Company;
            string companyName = company.Id;
            Train exchangedTrain = action.ExchangedTrain;
            SpecialTrainBuy stb = null;

            string errMsg = null;
            int presidentCash = action.PresidentCashToAdd;
            bool presidentMustSellShares = false;
            int price = action.PricePaid;
            int actualPresidentCash = 0;
            int cashToBeRaisedByPresident = 0;
            Player currentPlayer = operatingCompany.Value.GetPresident();

            // Dummy loop to enable a quick jump out.
            while (true)
            {
                // Checks
                // Must be correct step
                if (GetStep() != GameDef.OrStep.BUY_TRAIN)
                {
                    errMsg = LocalText.GetText("WrongActionNoTrainBuyingCost");
                    break;
                }

                if (train == null)
                {
                    errMsg = LocalText.GetText("NoTrainSpecified");
                    break;
                }

                // Amount must be non-negative
                if (price < 0)
                {
                    errMsg = LocalText.GetText("NegativeAmountNotAllowed",
                                    Bank.Format(this, price));
                    break;
                }

                // Fixed price must be honored
                int fixedPrice = action.FixedCost;
                if (fixedPrice != 0 && fixedPrice != price)
                {
                    errMsg =
                            LocalText.GetText("FixedPriceNotPaid",
                                    Bank.Format(this, price),
                                    Bank.Format(this, fixedPrice));
                }

                // Does the company have room for another train?
                int trainLimit = operatingCompany.Value.GetCurrentTrainLimit();
                if (!CanBuyTrainNow && !action.IsForExchange)
                {
                    errMsg =
                            LocalText.GetText("WouldExceedTrainLimit", trainLimit.ToString());
                    break;
                }

                /* Check if this is an emergency buy */
                if (action.MustPresidentAddCash)
                {
                    // From the Bank
                    presidentCash = action.PresidentCashToAdd;
                    if (currentPlayer.CashValue >= presidentCash)
                    {
                        actualPresidentCash = presidentCash;
                    }
                    else
                    {
                        presidentMustSellShares = true;
                        cashToBeRaisedByPresident =
                                presidentCash - currentPlayer.CashValue;
                    }
                }
                else if (action.MayPresidentAddCash)
                {
                    // From another company
                    presidentCash = price - operatingCompany.Value.Cash;
                    if (presidentCash > action.PresidentCashToAdd)
                    {
                        errMsg =
                                LocalText.GetText(
                                        "PresidentMayNotAddMoreThan",
                                        Bank.Format(this,
                                                action.PresidentCashToAdd));
                        break;
                    }
                    else if (currentPlayer.CashValue >= presidentCash)
                    {
                        actualPresidentCash = presidentCash;
                    }
                    else
                    {
                        presidentMustSellShares = true;
                        cashToBeRaisedByPresident =
                                presidentCash - currentPlayer.CashValue;
                    }

                }
                else
                {
                    // No forced buy - does the company have the money?
                    if (price > operatingCompany.Value.Cash)
                    {
                        errMsg =
                                LocalText.GetText(
                                        "NotEnoughMoney",
                                        companyName,
                                        Bank.Format(this,
                                                operatingCompany.Value.Cash),
                                        Bank.Format(this, price));
                        break;
                    }
                }

                if (action.IsForExchange)
                {
                    if (exchangedTrain == null)
                    {
                        errMsg = LocalText.GetText("NoExchangedTrainSpecified");
                        // TEMPORARY FIX to clean up invalidated saved files - DOES
                        // NOT WORK!!??
                        // exchangedTrain =
                        // operatingCompany.getObject().getPortfolio().getTrainList().get(0);
                        // action.setExchangedTrain(exchangedTrain);
                        break;
                    }
                    else if (operatingCompany.Value.PortfolioModel.GetTrainOfType(
                          exchangedTrain.CertType) == null)
                    {
                        errMsg =
                                LocalText.GetText("CompanyDoesNotOwnTrain",
                                        operatingCompany.Value.Id,
                                        exchangedTrain.ToText());
                        break;
                    }
                }

                stb = action.SpecialProperty;
                // TODO Note: this is not yet validated

                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(
                        this,
                        LocalText.GetText("CannotBuyTrainFor", companyName,
                                train.ToText(), Bank.Format(this, price), errMsg));
                return false;
            }

            /* End of validation, start of execution */

            Phase previousPhase = Phase.GetCurrent(this);

            if (presidentMustSellShares)
            {
                savedAction = action;

                gameManager.StartShareSellingRound(
                        operatingCompany.Value.GetPresident(),
                        cashToBeRaisedByPresident, operatingCompany.Value, true);

                return true;
            }

            if (actualPresidentCash > 0)
            {
                // FIXME: It used to be presidentCash, should it not have been
                // actualPresidentCash
                // MoneyModel.cashMove(currentPlayer, operatingCompany.Value,
                // presidentCash);
                string cashText =
                        Currency.Wire(currentPlayer, actualPresidentCash,
                                operatingCompany.Value);
                ReportBuffer.Add(this, LocalText.GetText("PresidentAddsCash",
                        operatingCompany.Value.Id, currentPlayer.Id,
                        cashText));
            }

            IOwner oldOwner = train.Owner;

            if (exchangedTrain != null)
            {
                Train oldTrain =
                        operatingCompany.Value.PortfolioModel.GetTrainOfType(
                                exchangedTrain.CertType);
                (train.IsObsolete() ? scrapHeap : pool).AddTrain(oldTrain);
                ReportBuffer.Add(this, LocalText.GetText("ExchangesTrain",
                        companyName, exchangedTrain.ToText(), train.ToText(),
                        oldOwner.Id, Bank.Format(this, price)));
            }
            else if (stb == null)
            {
                ReportBuffer.Add(this, LocalText.GetText("BuysTrain", companyName,
                        train.ToText(), oldOwner.Id, Bank.Format(this, price)));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("BuysTrainUsingSP",
                        companyName, train.ToText(), oldOwner.Id,
                        Bank.Format(this, price), stb.OriginalCompany.Id));
            }

            train.SetTrainType(action.TrainType); // Needed for dual trains bought from
                                                  // the Bank

            operatingCompany.Value.BuyTrain(train, price);

            if (oldOwner == ipo.Parent)
            {
                train.CertType.AddToBoughtFromIPO();
                trainManager.SetAnyTrainBought(true);
                // Clone the train if infinitely available
                if (train.CertType.HasInfiniteQuantity)
                {
                    ipo.AddTrain(trainManager.CloneTrain(train.CertType));
                }

            }
            if (oldOwner is BankPortfolio)
            {
                trainsBoughtThisTurn.Add(train.CertType);
            }

            if (stb != null)
            {
                stb.SetExercised();
                log.Debug("This was a special train buy");
            }

            // Check if the phase has changed.
            trainManager.CheckTrainAvailability(train, oldOwner);

            // Check if any companies must discard trains
            if (Phase.GetCurrent(this) != previousPhase && CheckForExcessTrains())
            {
                stepObject.Set(GameDef.OrStep.DISCARD_TRAINS);
            }

            if (trainManager.HasPhaseChanged()) NewPhaseChecks();

            return true;
        }

        /**
         * Can the operating company buy a train now? Normally only calls
         * isBelowTrainLimit() to get the result. May be overridden if other
         * considerations apply (such as having a Pullmann in 18EU).
         * 
         * @return
         */
        protected bool CanBuyTrainNow
        {
            get
            {
                return IsBelowTrainLimit;
            }
        }

        public bool CheckForExcessTrains()
        {

            excessTrainCompanies = new Dictionary<Player, List<PublicCompany>>();
            Player player;
            foreach (PublicCompany comp in operatingCompanies.View())
            {
                if (comp.PortfolioModel.NumberOfTrains > comp.GetCurrentTrainLimit())
                {
                    player = comp.GetPresident();
                    if (!excessTrainCompanies.ContainsKey(player))
                    {
                        excessTrainCompanies[player] = new List<PublicCompany>(2);
                    }
                    excessTrainCompanies[player].Add(comp);
                }

            }
            return excessTrainCompanies.Count > 0;
        }

        /** Stub */
        virtual protected void NewPhaseChecks() { }

        /**
         * Get a list of buyable trains for the currently operating company. Omit
         * trains that the company has no money for. If there is no cash to buy any
         * train from the Bank, prepare for emergency train buying.
         */
        public void SetBuyableTrains()
        {
            if (operatingCompany.Value == null) return;

            int cash = operatingCompany.Value.Cash;

            int cost = 0;
            List<Train> trains;

            bool hasTrains = operatingCompany.Value.PortfolioModel.NumberOfTrains > 0;

            // Cannot buy a train without any cash, unless you have to
            if (cash == 0 && hasTrains) return;

            bool canBuyTrainNow = CanBuyTrainNow;
            bool mustBuyTrain = !hasTrains && operatingCompany.Value.MustOwnATrain;
            bool emergency = false;

            SortedDictionary<int, Train> newEmergencyTrains = new SortedDictionary<int, Train>();
            SortedDictionary<int, Train> usedEmergencyTrains = new SortedDictionary<int, Train>();

            // First check if any more trains may be bought from the Bank
            // Postpone train limit checking, because an exchange might be possible
            if (Phase.GetCurrent(this).CanBuyMoreTrainsPerTurn
                || trainsBoughtThisTurn.IsEmpty)
            {
                bool mayBuyMoreOfEachType =
                        Phase.GetCurrent(this).CanBuyMoreTrainsPerTypePerTurn;

                /* New trains */
                trains = trainManager.GetAvailableNewTrains();
                foreach (Train train in trains)
                {
                    if (!operatingCompany.Value.MayBuyTrainType(train)) continue;
                    if (!mayBuyMoreOfEachType
                        && trainsBoughtThisTurn.Contains(train.CertType))
                    {
                        continue;
                    }

                    // Allow dual trains (since jun 2011)
                    List<TrainType> types = train.CertType.GetPotentialTrainTypes();
                    foreach (TrainType type in types)
                    {
                        cost = type.Cost;
                        if (cost <= cash)
                        {
                            if (canBuyTrainNow)
                            {
                                BuyTrain action = new BuyTrain(train, type, ipo.Parent, cost);
                                action.IsForcedBuyIfNoRoute = mustBuyTrain; // TEMPORARY
                                possibleActions.Add(action);
                            }
                        }
                        else if (mustBuyTrain)
                        {
                            newEmergencyTrains[cost] = train;
                        }
                    }

                    // Even at train limit, exchange is allowed (per 1856)
                    if (train.CanBeExchanged && hasTrains)
                    {
                        cost = train.CertType.ExchangeCost;
                        if (cost <= cash)
                        {
                            var exchangeableTrains = operatingCompany.Value.PortfolioModel.GetUniqueTrains();
                            BuyTrain action = new BuyTrain(train, ipo.Parent, cost);
                            action.SetTrainsForExchange(exchangeableTrains);
                            // if (atTrainLimit) action.setForcedExchange(true);
                            possibleActions.Add(action);
                            canBuyTrainNow = true;
                        }
                    }

                    if (!canBuyTrainNow) continue;

                    // Can a special property be used?
                    // N.B. Assume that this never occurs in combination with
                    // dual trains or train exchanges,
                    // otherwise the below code must be duplicated above.
                    foreach (SpecialTrainBuy stb in GetSpecialProperties<SpecialTrainBuy>())
                    {
                        int reducedPrice = stb.GetPrice(cost);
                        if (reducedPrice > cash) continue;
                        BuyTrain bt = new BuyTrain(train, ipo.Parent, reducedPrice);
                        bt.SpecialProperty = stb;
                        bt.IsForcedBuyIfNoRoute = mustBuyTrain; // TEMPORARY
                        possibleActions.Add(bt);
                    }

                }
                if (!canBuyTrainNow) return;

                /* Used trains */
                trains = pool.GetUniqueTrains();
                foreach (Train train in trains)
                {
                    if (!mayBuyMoreOfEachType && trainsBoughtThisTurn.Contains(train.CertType))
                    {
                        continue;
                    }
                    cost = train.Cost;
                    if (cost <= cash)
                    {
                        BuyTrain bt = new BuyTrain(train, pool.Parent, cost);
                        bt.IsForcedBuyIfNoRoute = mustBuyTrain; // TEMPORARY
                        possibleActions.Add(bt);
                    }
                    else if (mustBuyTrain)
                    {
                        usedEmergencyTrains[cost] = train;
                    }
                }

                emergency = mustBuyTrain && possibleActions.GetActionType<BuyTrain>().Count == 0;

                // If we must buy a train and haven't found one yet, the president
                // must add cash.
                if (emergency
                    // Some people think it's allowed in 1835 to buy a new train
                    // with president cash
                    // even if the company has enough cash to buy a used train.
                    // Players who think differently can ignore that extra option.
                    || GameDef.GetGameParameterAsBoolean(this, GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN)
                    && (newEmergencyTrains.Count > 0))
                {
                    if (GameDef.GetGameParameterAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN))
                    {
                        // Find the cheapest one
                        // Assume there is always one available from IPO
                        var firstTrain = newEmergencyTrains.First();
                        int cheapestTrainCost = firstTrain.Key;
                        Train cheapestTrain = firstTrain.Value;
                        if ((usedEmergencyTrains.Count > 0)
                            && usedEmergencyTrains.First().Key < cheapestTrainCost)
                        {
                            cheapestTrainCost = usedEmergencyTrains.First().Key;
                            cheapestTrain = usedEmergencyTrains[cheapestTrainCost];
                        }
                        BuyTrain bt = new BuyTrain(cheapestTrain, cheapestTrain.Owner, cheapestTrainCost);
                        bt.SetPresidentMustAddCash(cheapestTrainCost - cash);
                        bt.IsForcedBuyIfNoRoute = mustBuyTrain; // TODO TEMPORARY
                        possibleActions.Add(bt);
                    }
                    else
                    {
                        // All possible bank trains are buyable
                        foreach (Train train in newEmergencyTrains.Values)
                        {
                            BuyTrain bt = new BuyTrain(train, ipo.Parent, train.Cost);
                            bt.SetPresidentMustAddCash(train.Cost - cash);
                            bt.IsForcedBuyIfNoRoute = mustBuyTrain; // TODO TEMPORARY
                            possibleActions.Add(bt);
                        }
                        foreach (Train train in usedEmergencyTrains.Values)
                        {
                            BuyTrain bt = new BuyTrain(train, pool.Parent, train.Cost);
                            bt.SetPresidentMustAddCash(train.Cost - cash);
                            bt.IsForcedBuyIfNoRoute = mustBuyTrain; // TODO TEMPORARY
                            possibleActions.Add(bt);
                        }
                    }
                }
            }

            if (!canBuyTrainNow) return;

            /* Other company trains, sorted by president (current player first) */
            if (Phase.GetCurrent(this).IsTrainTradingAllowed)
            {
                BuyTrain bt;
                Player p;
                int index;
                int numberOfPlayers = playerManager.NumberOfPlayers;
                int presidentCash =
                        operatingCompany.Value.GetPresident().CashValue;

                // Set up a list per player of presided companies
                List<List<PublicCompany>> companiesPerPlayer = new List<List<PublicCompany>>(numberOfPlayers);
                for (int i = 0; i < numberOfPlayers; i++)
                    companiesPerPlayer.Add(new List<PublicCompany>(4));
                List<PublicCompany> companies;
                // Sort out which players preside over which companies.
                foreach (PublicCompany c in companyManager.GetAllPublicCompanies())
                {
                    if (!c.HasFloated()) continue;
                    if (c.IsClosed() || c == operatingCompany.Value) continue;
                    p = c.GetPresident();
                    index = p.Index;
                    companiesPerPlayer[index].Add(c);
                }
                // Scan trains per company per player, operating company president
                // first
                int currentPlayerIndex =
                        playerManager.CurrentPlayer.Index;
                for (int i = currentPlayerIndex; i < currentPlayerIndex + numberOfPlayers; i++)
                {
                    companies = companiesPerPlayer[i % numberOfPlayers];
                    foreach (PublicCompany company in companies)
                    {
                        trains = company.PortfolioModel.GetUniqueTrains();
                        foreach (Train train in trains)
                        {
                            if (train.IsObsolete() || !train.IsTradeable)
                                continue;
                            bt = null;
                            if (i != currentPlayerIndex
                                && GameDef.GetGameParameterAsBoolean(this, GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS)
                                || operatingCompany.Value.MustTradeTrainsAtFixedPrice
                                || company.MustTradeTrainsAtFixedPrice)
                            {
                                // Fixed price
                                if ((cash >= train.Cost)
                                    && (operatingCompany.Value.MayBuyTrainType(train)))
                                {
                                    // TODO: Check if this still works, as now the
                                    // company is the from type
                                    bt = new BuyTrain(train, company, train.Cost);
                                }
                                else
                                {
                                    continue;
                                }
                            }
                            else if (cash > 0
                                     || emergency
                                     && GameDef.GetGameParameterAsBoolean(this, GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY))
                            {
                                // TODO: Check if this still works, as now the
                                // company is the from type
                                bt = new BuyTrain(train, company, 0);

                                // In some games the president may add extra cash up
                                // to the list price
                                if (emergency && cash < train.Cost)
                                {
                                    bt.SetPresidentMayAddCash(Math.Min(train.Cost - cash, presidentCash));
                                }
                            }
                            if (bt != null) possibleActions.Add(bt);
                        }
                    }
                }
            }

            if (!operatingCompany.Value.MustOwnATrain
                || operatingCompany.Value.PortfolioModel.NumberOfTrains > 0)
            {
                doneAllowed = true;
            }
        }

        /**
         * Returns whether or not the company is allowed to buy a train, considering
         * its train limit.
         *
         * @return
         */
        protected bool IsBelowTrainLimit
        {
            get
            {
                return operatingCompany.Value.GetNumberOfTrains() < operatingCompany.Value.GetCurrentTrainLimit();
            }
        }

        public void CheckForeignSales()
        {
            if (GameDef.GetGameParameterAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                && trainManager.IsAnyTrainBought())
            {
                Train train = trainManager.GetAvailableNewTrains()[0];
                if (train.CertType.HasInfiniteQuantity) return;
                scrapHeap.AddTrain(train);
                ReportBuffer.Add(this, LocalText.GetText("RemoveTrain", train.ToText()));
            }
        }

        /*
         * ======================================= 8. VARIOUS UTILITIES
         * =======================================
         */

        protected List<T> GetSpecialProperties<T>() where T : SpecialProperty
        {
            List<T> specialProperties = new List<T>();
            if (!operatingCompany.Value.IsClosed())
            {
                // OC may have closed itself (e.g. in 1835 when M2 buys 1st 4T and
                // starts PR)
                specialProperties.AddRange(operatingCompany.Value.PortfolioModel.GetSpecialProperties<T>(false));
                specialProperties.AddRange(operatingCompany.Value.GetPresident().PortfolioModel.GetSpecialProperties<T>(false));
            }
            return specialProperties;
        }

        /**
         * Update the status if the step has changed by an Undo or Redo
         */
        public void Update(string text)
        {
            PrepareStep();
        }

        override public string ToString()
        {
            return "OperatingRound " + thisOrNumber;
        }

        /** @Overrides */
        public bool Equals(IRoundFacade round)
        {
            return (round is OperatingRound)
                       && thisOrNumber.Equals(((OperatingRound)round).thisOrNumber);
        }

        override public string RoundName
        {
            get
            {
                return ToString();
            }
        }

        // Observer methods
        public Observable Observable
        {
            get
            {
                return stepObject;
            }
        }
    }
}
