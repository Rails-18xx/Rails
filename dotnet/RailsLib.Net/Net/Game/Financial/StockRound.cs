using GameLib.Net.Common;
using GameLib.Rails.Game.Action;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using GameLib.Net.Game;
using GameLib.Net.Game.Financial;
using Wintellect.PowerCollections;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.Special;

/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */

namespace GameLib.Net.Game.Financial
{
    public class StockRound : Round, ICreatable
    {
        /* Transient memory (per round only) */
        protected int numberOfPlayers;
        protected Player currentPlayer;
        protected Player startingPlayer;

        protected GenericState<PublicCompany> companyBoughtThisTurnWrapper;

        protected BooleanState hasSoldThisTurnBeforeBuying;

        protected BooleanState hasActed;

        protected IntegerState numPasses;

        protected DictionaryState<PublicCompany, StockSpace> sellPrices;

        /** Records lifted share selling obligations in the current round<p>
         * Example: >60% ownership allowed after a merger in 18EU.
         */
        protected SetState<PublicCompany> sellObligationLifted = null;


        /* Rule constants */
        public static readonly int SELL_BUY_SELL = 0;
        public static readonly int SELL_BUY = 1;
        public static readonly int SELL_BUY_OR_BUY_SELL = 2;

        /* Action constants */
        static public readonly int BOUGHT = 0;
        static public readonly int SOLD = 1;

        /* Rules */
        protected int sequenceRule;
        protected bool raiseIfSoldOut = false;

        /* Temporary variables */
        protected bool isOverLimits = false;
        protected string overLimitsDetail = null;

        /** Autopasses */
        private readonly ListState<Player> autopasses;
        private readonly ListState<Player> canRequestTurn;
        private readonly ListState<Player> hasRequestedTurn;

        /**
         * Constructed via Configure
         */
        public StockRound(GameManager parent, string id) : base(parent, id)
        {
            autopasses = ListState<Player>.Create(this, "autopasses");
            canRequestTurn = ListState<Player>.Create(this, "canRequestTurn");
            hasRequestedTurn = ListState<Player>.Create(this, "hasRequestedTurn");
            sellPrices = DictionaryState<PublicCompany, StockSpace>.Create(this, "sellPrices");
            hasSoldThisTurnBeforeBuying =
                BooleanState.Create(this, "hasSoldThisTurnBeforeBuying");

            companyBoughtThisTurnWrapper =
                GenericState<PublicCompany>.Create(this, "companyBoughtThisTurnWrapper");

            hasActed = BooleanState.Create(this, "hasActed");
            numPasses = IntegerState.Create(this, "numPasses");

            if (numberOfPlayers == 0)
                numberOfPlayers = GetRoot.PlayerManager.Players.Count;

            sequenceRule = GameDef.GetGameParameterAsInt(this, GameDef.Parm.STOCK_ROUND_SEQUENCE);

            guiHints.SetVisibilityHint(GuiDef.Panel.MAP, true);
            guiHints.SetVisibilityHint(GuiDef.Panel.STOCK_MARKET, true);
            guiHints.ActivePanel = GuiDef.Panel.STATUS;
        }

        /** Start the Stock Round. <p>
         * Please note: subclasses that are NOT real stock rounds should NOT call this method
         * (or set raiseIfSoldOut to false after calling this method).
         */
        // called by:
        // GameManager: startStockRound
        // StockRound 1837, 18EU: (start)

        // overridden by:
        // StockRound 1837, 18EU
        // NationalFormationRound, PrussianFormationRound
        public void Start()
        {

            ReportBuffer.Add(this, LocalText.GetText("StartStockRound", StockRoundNumber));

            playerManager.SetCurrentToPriorityPlayer();
            startingPlayer = playerManager.CurrentPlayer; // For the Report
            ReportBuffer.Add(this, LocalText.GetText("HasPriority",
                    startingPlayer.Id));

            InitPlayer();

            raiseIfSoldOut = true;

        }

        /*----- General methods -----*/
        // called by
        // StockRound: checkFirstRoundSellRestriction, finishRound, getRoundName, start
        // StockRound 1837, 1880: finishRound
        // StatusWindow: updateStatus

        // not overridden
        public int StockRoundNumber
        {
            get
            {
                return gameManager.GetSRNumber();
            }
        }

        // called by:
        // GameManager: process, processOnReload
        // GameLoader: replayGame
        // StockRound 1837, 1856, 18EU: setPossibleActions

        // overridden by
        // ShareSellingRound
        // TreasuryShareRound
        // NationalFormationRound, PrussianFormationRound
        // StockRound 1837, 1856, 18EU
        // ShareSellingRound 1880
        // FinalMinorExchangeRound, FinalCoalExchangeRound
        override public bool SetPossibleActions()
        {

            // fix of the forced undo bug
            currentPlayer = playerManager.CurrentPlayer;

            bool passAllowed = false;

            SetSellableShares();

            // Certificate limits must be obeyed by selling excess shares
            // before any other action is allowed.
            if (isOverLimits)
            {
                return true;
            }

            passAllowed = true;

            SetBuyableCerts();

            SetSpecialActions();

            SetGameSpecificActions();

            if (passAllowed)
            {
                if (hasActed.Value)
                {
                    possibleActions.Add(new NullAction(NullAction.Modes.DONE));
                }
                else
                {
                    possibleActions.Add(new NullAction(NullAction.Modes.PASS));
                    possibleActions.Add(new NullAction(NullAction.Modes.AUTOPASS));
                }
            }

            if (GetAutopasses() != null)
            {
                foreach (Player player in GetAutopasses())
                {
                    possibleActions.Add(new RequestTurn(player));
                }
            }

            return true;
        }

        /** Stub, can be overridden in subclasses */
        // called by:
        // StockRound: setPossibleActions

        // overridden by:
        // StockRound 1837, 18EU
        virtual protected void SetGameSpecificActions()
        {

        }

        /**
         * Create a list of certificates that a player may buy in a Stock Round,
         * taking all rules into account.
         *
         * @return List of buyable certificates.
         */
        // called by
        // StockRound: setPossibleActions
        // StockRound 1835: (setBuyableCerts)

        // overridden by
        // TreasuryShareRound
        // StockRound 1835, 1880,18EU
        virtual public void SetBuyableCerts()
        {
            if (!MayCurrentPlayerBuyAnything()) return;

            ICollection<PublicCertificate> certs;
            PublicCertificate cert;
            StockSpace stockSpace;
            PortfolioModel from;
            int price;
            int number;
            int unitsForPrice;

            int playerCash = currentPlayer.CashValue;

            /* Get the next available IPO certificates */
            // Never buy more than one from the IPO
            PublicCompany companyBoughtThisTurn = companyBoughtThisTurnWrapper.Value;
            if (companyBoughtThisTurn == null)
            {
                from = ipo;
                MultiDictionaryBase<PublicCompany, PublicCertificate> map =
                    from.GetCertsPerCompanyMap();

                foreach (PublicCompany comp in map.Keys)
                {
                    certs = map[comp];
                    // if (certs.isEmpty()) continue; // TODO: is this removal correct?

                    /* Only the top certificate is buyable from the IPO */
                    // TODO: This is code that should be deprecated
                    int lowestIndex = 99;
                    cert = null;
                    int index;
                    foreach (PublicCertificate c in certs)
                    {
                        index = c.IndexInCompany;
                        if (index < lowestIndex)
                        {
                            lowestIndex = index;
                            cert = c;
                        }
                    }

                    unitsForPrice = comp.ShareUnitsForSharePrice;
                    if (currentPlayer.HasSoldThisRound(comp)) continue;
                    if (MaxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                            cert.Share) < 1) continue;

                    /* Would the player exceed the total certificate limit? */
                    stockSpace = comp.GetCurrentSpace();
                    if ((stockSpace == null || !stockSpace.IsNoCertLimit) && !MayPlayerBuyCertificate(
                            currentPlayer, comp, cert.CertificateCount)) continue;

                    if (!cert.IsPresidentShare)
                    {
                        price = comp.GetIPOPrice() / unitsForPrice;
                        if ((price * cert.GetShares()) <= playerCash)
                        {
                            possibleActions.Add(new BuyCertificate(comp, cert.Share, from.Parent, price));
                        }
                    }
                    else if (!comp.HasStarted())
                    {
                        if (comp.GetIPOPrice() != 0)
                        {
                            price = comp.GetIPOPrice() * cert.GetShares() / unitsForPrice;
                            if (price <= playerCash)
                            {
                                possibleActions.Add(new StartCompany(comp, price));
                            }
                        }
                        else
                        {
                            List<int> startPrices = new List<int>();
                            foreach (int startPrice in stockMarket.GetStartPrices())
                            {
                                if (startPrice * cert.GetShares() <= playerCash)
                                {
                                    startPrices.Add(startPrice);
                                }
                            }
                            if (startPrices.Count > 0)
                            {
                                int[] prices = new int[startPrices.Count];
                                Array.Sort(prices);
                                for (int i = 0; i < prices.Length; i++)
                                {
                                    prices[i] = startPrices[i];
                                }
                                possibleActions.Add(new StartCompany(comp, prices));
                            }
                        }
                    }

                }
            }

            /* Get the unique Pool certificates and check which ones can be bought */
            from = pool;
            MultiDictionaryBase<PublicCompany, PublicCertificate> map2 = from.GetCertsPerCompanyMap();
            /* Allow for multiple share unit certificates (e.g. 1835) */
            PublicCertificate[] uniqueCerts;
            int[] numberOfCerts;
            int shares;
            int shareUnit;
            int maxNumberOfSharesToBuy;

            foreach (PublicCompany comp in map2.Keys)
            {
                certs = map2[comp];
                // if (certs.isEmpty()) continue; // TODO: Is this removal correct?

                stockSpace = comp.GetCurrentSpace();
                unitsForPrice = comp.ShareUnitsForSharePrice;
                price = stockSpace.Price / unitsForPrice;
                shareUnit = comp.GetShareUnit();
                maxNumberOfSharesToBuy = MaxAllowedNumberOfSharesToBuy(currentPlayer, comp, shareUnit);

                /* Checks if the player can buy any shares of this company */
                if (maxNumberOfSharesToBuy < 1) continue;
                if (currentPlayer.HasSoldThisRound(comp)) continue;
                if (companyBoughtThisTurn != null)
                {
                    // If a cert was bought before, only brown zone ones can be
                    // bought again in the same turn
                    if (comp != companyBoughtThisTurn) continue;
                    if (!stockSpace.IsNoBuyLimit) continue;
                }

                /* Check what share multiples are available
                 * Normally only 1, but 1 and 2 in 1835. Allow up to 4.
                 */
                uniqueCerts = new PublicCertificate[5];
                numberOfCerts = new int[5];
                foreach (PublicCertificate cert2 in certs)
                {
                    shares = cert2.GetShares();
                    if (maxNumberOfSharesToBuy < shares) continue;
                    numberOfCerts[shares]++;
                    if (uniqueCerts[shares] != null) continue;
                    uniqueCerts[shares] = cert2;
                }

                /* Create a BuyCertificate action per share size */
                for (shares = 1; shares < 5; shares++)
                {
                    /* Only certs in the brown zone may be bought all at once */
                    number = numberOfCerts[shares];
                    if (number == 0) continue;

                    if (!stockSpace.IsNoBuyLimit)
                    {
                        number = 1;
                        /* Would the player exceed the per-company share hold limit? */
                        if (!CheckAgainstHoldLimit(currentPlayer, comp, number)) continue;

                        /* Would the player exceed the total certificate limit? */
                        if (!stockSpace.IsNoCertLimit
                                && !MayPlayerBuyCertificate(currentPlayer, comp,
                                        number * uniqueCerts[shares].CertificateCount))
                            continue;
                    }

                    // Does the player have enough cash?
                    while (number > 0 && playerCash < number * price * shares)
                    {
                        number--;
                    }

                    if (number > 0)
                    {
                        possibleActions.Add(new BuyCertificate(comp,
                                uniqueCerts[shares].Share, from.Parent, price, number));
                    }
                }
            }

            // Get any shares in company treasuries that can be bought
            if (gameManager.CanAnyCompanyHoldShares)
            {
                foreach (PublicCompany company in companyManager.GetAllPublicCompanies())
                {
                    // TODO: Has to be rewritten (director)
                    List<PublicCertificate> certs3 = new List<PublicCertificate>(company.PortfolioModel.GetCertificates(company));
                    if (certs3.Count == 0) continue;
                    cert = certs3[0];
                    if (currentPlayer.HasSoldThisRound(company)) continue;
                    if (!CheckAgainstHoldLimit(currentPlayer, company, 1)) continue;
                    if (MaxAllowedNumberOfSharesToBuy(currentPlayer, company,
                            cert.Share) < 1) continue;
                    stockSpace = company.GetCurrentSpace();
                    if (!stockSpace.IsNoCertLimit
                            && !MayPlayerBuyCertificate(currentPlayer, company, 1)) continue;
                    if (company.GetMarketPrice() <= playerCash)
                    {
                        possibleActions.Add(new BuyCertificate(company, cert.Share,
                                company, company.GetMarketPrice()));
                    }
                }
            }
        }

        /**
         * Create a list of certificates that a player may sell in a Stock Round,
         * taking all rules taken into account.
         *
         * @return List of sellable certificates.
         */

        // FIXME Rails 2.0: 
        // This is rewritten taken into account that actions will not be changed for now
        // A change of action will allow to simplify this strongly

        // called by:
        // StockRound: setPossibleActions

        // overridden by:
        // ShareSellingRound
        virtual public void SetSellableShares()
        {

            if (!MayCurrentPlayerSellAnything()) return;

            //bool choiceOfPresidentExchangeCerts = false;
            isOverLimits = false;
            overLimitsDetail = null;

            StringBuilder violations = new StringBuilder();
            PortfolioModel playerPortfolio = currentPlayer.PortfolioModel;

            /*
             * First check of which companies the player owns stock, and what
             * maximum percentage he is allowed to sell.
             */
            foreach (PublicCompany company in companyManager.GetAllPublicCompanies())
            {

                // Check if shares of this company can be sold at all
                if (!MayPlayerSellShareOfCompany(company))
                {
                    continue;
                }

                int ownedShare = playerPortfolio.GetShareNumber(company);
                if (ownedShare == 0)
                {
                    continue;
                }

                /* May not sell more than the Pool can accept */
                int poolAllowsShares = PlayerShareUtils.PoolAllowsShareNumbers(company);
                log.Debug("company = " + company);
                log.Debug("poolAllowShares = " + poolAllowsShares);
                int maxShareToSell = Math.Min(ownedShare, poolAllowsShares);

                // if no share can be sold
                if (maxShareToSell == 0)
                {
                    continue;
                }

                // Is player over the hold limit of this company?
                if (!CheckAgainstHoldLimit(currentPlayer, company, 0))
                {
                    // The first time this happens, remove all non-over-limits sell options
                    if (!isOverLimits) possibleActions.Clear();
                    isOverLimits = true;
                    violations.Append(LocalText.GetText("ExceedCertificateLimitCompany",
                            company.Id,
                            playerPortfolio.GetShare(company),
                            GameDef.GetGameParameterAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)
                    ));

                }
                else
                {
                    // If within limits, but an over-limits situation exists: correct that first.
                    if (isOverLimits) continue;
                }

                /*
                 * If the current Player is president, check if there is a play to dump on
                 * => dumpThreshold = how many shareNumbers have to be sold for dump
                 * => possibleSharesToSell = list of shareNumbers that can be sold 
                 *    (includes check for swapping the presidency)
                 * => dumpIsPossible = true
                 */
                int dumpThreshold = 0;
                SortedSet<int> possibleSharesToSell = null;
                bool dumpIsPossible = false;
                if (company.GetPresident() == currentPlayer)
                {
                    Player potential = company.FindPlayerToDump();
                    if (potential != null)
                    {
                        dumpThreshold = ownedShare - potential.PortfolioModel.GetShareNumber(company) + 1;
                        possibleSharesToSell = PlayerShareUtils.SharesToSell(company, currentPlayer);
                        dumpIsPossible = true;
                        log.Debug("dumpThreshold = " + dumpThreshold);
                        log.Debug("possibleSharesToSell = " + possibleSharesToSell);
                        log.Debug("dumpIsPossible = " + dumpIsPossible);
                    }
                }

                /*
                 * Check what share units the player actually owns. In some games
                 * (e.g. 1835) companies may have different ordinary shares: 5% and
                 * 10%, or 10% and 20%. The president's share counts as a multiple
                 * of the smallest ordinary share unit type.
                 */


                // Check the price. If a cert was sold before this turn, the original price is still valid.
                int price = GetCurrentSellPrice(company);

                /* Allow for different share units (as in 1835) */
                OrderedBag<int> certCount = playerPortfolio.GetCertificateTypeCounts(company);
                // #FIXME this set stuff is probably broken
                // Make sure that single shares are always considered (due to possible dumping)
                SortedSet<int> certSizeElements = new SortedSet<int>(certCount);//.ElementSet());
                certSizeElements.Add(1);

                foreach (int shareSize in certSizeElements)
                {
                    int number = certCount.NumberOfCopies(shareSize);// count(shareSize);

                    // If you can dump a presidency, you add the shareNumbers of the presidency
                    // to the single shares to be sold
                    if (dumpIsPossible && shareSize == 1 && number + company.GetPresidentsShare().GetShares() >= dumpThreshold)
                    {
                        number += company.GetPresidentsShare().GetShares();
                        // but limit this to the pool 
                        number = Math.Min(number, poolAllowsShares);
                        log.Debug("Dump is possible increased single shares to " + number);
                    }

                    if (number == 0)
                    {
                        continue;
                    }

                    /* In some games (1856), a just bought share may not be sold */
                    // This code ignores the possibility of different share units
                    if ((bool)gameManager.GetGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT)
                            && company.Equals(companyBoughtThisTurnWrapper.Value)
                            /* An 1856 clarification by Steve Thomas (backed by Bill Dixon) states that
                             * in this situation a half-presidency may be sold
                             * (apparently even if a dump would otherwise not be allowed),
                             * as long as the number of shares does not become zero.
                             * So the rule "can't sell a just bought share" only means,
                             * that the number of shares may not be sold down to zero.
                             * Added 4jun2012 by EV */
                            && number == ownedShare)
                    {
                        number--;
                    }

                    if (number <= 0)
                    {
                        continue;
                    }

                    // Check against the maximum share that can be sold
                    number = Math.Min(number, maxShareToSell / shareSize);

                    if (number <= 0)
                    {
                        continue;
                    }

                    for (int i = 1; i <= number; i++)
                    {
                        // check if selling would dump the company
                        if (dumpIsPossible && i * shareSize >= dumpThreshold)
                        {
                            // dumping requires that the total is in the possibleSharesToSell list and that shareSize == 1
                            // multiple shares have to be sold separately
                            if (shareSize == 1 && possibleSharesToSell.Contains(i * shareSize))
                            {
                                possibleActions.Add(new SellShares(company, shareSize, i, price, 1));
                            }
                        }
                        else
                        {
                            // ... no dumping: standard sell
                            possibleActions.Add(new SellShares(company, shareSize, i, price, 0));
                        }
                    }
                }
            }

            // Is player over the total certificate hold limit?
            float certificateCount = playerPortfolio.GetCertificateCount();
            int certificateLimit = gameManager.GetPlayerCertificateLimit(currentPlayer);
            if (certificateCount > certificateLimit)
            {
                violations.Append(LocalText.GetText("ExceedCertificateLimitTotal",
                        certificateCount,
                        certificateLimit));
                isOverLimits = true;
            }

            if (isOverLimits)
            {
                DisplayBuffer.Add(this, LocalText.GetText("ExceedCertificateLimit"
                        , currentPlayer.Id, violations.ToString()));
            }
        }

        // called by:
        // StockRound: setPossibleActions

        // not overridden
        protected void SetSpecialActions()
        {

            List<SpecialProperty> sps =
                currentPlayer.PortfolioModel.GetSpecialProperties<SpecialProperty>(false);
            foreach (SpecialProperty sp in sps)
            {
                if (sp.IsUsableDuringSR)
                {
                    possibleActions.Add(new UseSpecialProperty(sp));
                }
            }
        }

        /*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/
        // called by:
        // GameManager: process, processOnReload
        // StockRound 1880: (process)
        // ShareSellingRound 1880: (process)

        // overridden by
        // StockRound 1880
        // ShareSellingRound 1880
        override public bool Process(PossibleAction action)
        {
            bool result = false;
            string playerName = action.PlayerName;
            currentPlayer = playerManager.CurrentPlayer;

            if (action is NullAction)
            {

                NullAction nullAction = (NullAction)action;
                switch (nullAction.Mode)
                {
                    case NullAction.Modes.PASS:
                    case NullAction.Modes.DONE:
                        result = Done((NullAction)action, playerName, false);
                        break;
                    case NullAction.Modes.AUTOPASS:
                        result = Done(null, playerName, true);
                        break;
                    default:
                        break;
                }

            }
            else if (action is StartCompany)
            {
                StartCompany startCompanyAction = (StartCompany)action;
                result = DoStartCompany(playerName, startCompanyAction);

            }
            else if (action is BuyCertificate)
            {
                result = DoBuyShares(playerName, (BuyCertificate)action);
            }
            else if (action is SellShares)
            {
                result = DoSellShares((SellShares)action);
            }
            else if (action is UseSpecialProperty)
            {
                result = DoUseSpecialProperty((UseSpecialProperty)action);
            }
            else if (action is RequestTurn)
            {
                result = DoRequestTurn((RequestTurn)action);
            }
            else if (result = ProcessGameSpecificAction(action))
            {

            }
            else
            {

                DisplayBuffer.Add(this, LocalText.GetText("UnexpectedAction", action.ToString()));
            }

            return result;
        }

        // Return value indicates whether the action has been processed.
        // called by:
        // StockRound: process

        // overridden by:
        // StockRound 1837, 18EU
        // PrussianFormationRound, NationalFormationRound
        virtual protected bool ProcessGameSpecificAction(PossibleAction action)
        {

            return false;
        }

        /**
         * Start a company by buying one or more shares (more applies to e.g. 1841)
         *
         * @param player The player that wants to start a company.
         * @param company The company to start.
         * @param price The start (par) price (ignored if the price is fixed).
         * @param shares The number of shares to buy (can be more than 1 in e.g.
         * 1841).
         * @return True if the company could be started. False indicates an error.
         */
        // called by:
        // StockRound: process
        // StockRound 1880: startCompany (not overridden!)

        // overridden by:
        // StockRound 18EU
        virtual public bool DoStartCompany(string playerName, StartCompany action)
        {
            PublicCompany company = action.Company;
            int price = action.Price;
            int shares = action.NumberBought;

            string errMsg = null;
            StockSpace startSpace = null;
            int numberOfCertsToBuy = 0;
            PublicCertificate cert = null;
            string companyName = company.Id;
            int cost = 0;

            currentPlayer = playerManager.CurrentPlayer;

            // Dummy loop to allow a quick jump out
            while (true)
            {

                // Check everything
                // Only the player that has the turn may buy
                if (!playerName.Equals(currentPlayer.Id))
                {
                    errMsg = LocalText.GetText("WrongPlayer", playerName, currentPlayer.Id);
                    break;
                }

                // The player may not have bought this turn.
                if (companyBoughtThisTurnWrapper.Value != null)
                {
                    errMsg = LocalText.GetText("AlreadyBought", playerName);
                    break;
                }

                // Check company
                if (company == null)
                {
                    errMsg = LocalText.GetText("CompanyDoesNotExist", companyName);
                    break;
                }
                // The company may not have started yet.
                if (company.HasStarted())
                {
                    errMsg =
                        LocalText.GetText("CompanyAlreadyStarted", companyName);
                    break;
                }

                // Find the President's certificate
                cert = ipo.FindCertificate(company, true);
                // Make sure that we buy at least one!
                if (shares < cert.GetShares()) shares = cert.GetShares();

                // Determine the number of Certificates to buy
                // (shortcut: assume that any additional certs are one share each)
                numberOfCertsToBuy = shares - (cert.GetShares() - 1);
                // Check if the player may buy that many certificates.
                if (!MayPlayerBuyCertificate(currentPlayer, company, numberOfCertsToBuy))
                {
                    errMsg = LocalText.GetText("CantBuyMoreCerts");
                    break;
                }

                // Check if the company has a fixed par price (1835).
                startSpace = company.GetStartSpace();
                if (startSpace != null)
                {
                    // If so, it overrides whatever is given.
                    price = startSpace.Price;
                }
                else
                {
                    // Else the given price must be a valid start price
                    if ((startSpace = stockMarket.GetStartSpace(price)) == null)
                    {
                        errMsg = LocalText.GetText("InvalidStartPrice",
                                Bank.Format(this, price),
                                company.Id);
                        break;
                    }
                }

                // Check if the Player has the money.
                cost = shares * price;
                if (currentPlayer.CashValue < cost)
                {
                    errMsg = LocalText.GetText("NoMoney");
                    break;
                }

                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CantStart",
                        playerName,
                        companyName,
                        Bank.Format(this, price),
                        errMsg));
                return false;
            }



            // All is OK, now start the company
            company.Start(startSpace);

            IMoneyOwner priceRecipient = GetSharePriceRecipient(company, ipo.Parent, price);

            // Transfer the President's certificate
            cert.MoveTo(currentPlayer);

            // If more than one certificate is bought at the same time, transfer
            // these too.
            for (int i = 1; i < numberOfCertsToBuy; i++)
            {
                cert = ipo.FindCertificate(company, false);
                cert.MoveTo(currentPlayer);
            }

            // Pay for these shares
            string costText = Currency.Wire(currentPlayer, cost, priceRecipient);

            ReportBuffer.Add(this, LocalText.GetText("START_COMPANY_LOG",
                    playerName,
                    companyName,
                    bank.Currency.Format(price), // TODO: Do this nicer
                    costText,
                    shares,
                    cert.Share,
                    priceRecipient.Id));
            ReportBuffer.GetAllWaiting(this);

            CheckFlotation(company);

            companyBoughtThisTurnWrapper.Set(company);
            hasActed.Set(true);
            SetPriority();

            // Check for any game-specific consequences
            // (such as making another company available in the IPO)
            GameSpecificChecks(ipo, company);

            return true;
        }

        /**
         * Buying one or more single or double-share certificates (more is sometimes
         * possible)
         *
         * @param player The player that wants to buy shares.
         * @param action The executed BuyCertificates action
         * @return True if the certificates could be bought. False indicates an
         * error.
         */
        // called by:
        // StockRound: process

        // overridden by:
        // TreasuryShareRound
        virtual public bool DoBuyShares(string playerName, BuyCertificate action)
        {

            PublicCompany company = action.Company;
            PortfolioModel from = action.FromPortfolio;
            string companyName = company.Id;
            int number = action.NumberBought;
            int shareUnit = company.GetShareUnit();
            int sharePerCert = action.SharePerCertificate;
            int share = number * sharePerCert;
            int shares = share / shareUnit;

            string errMsg = null;
            int price = 0;
            int cost = 0;

            currentPlayer = playerManager.CurrentPlayer;

            // Dummy loop to allow a quick jump out
            while (true)
            {

                // Check everything
                // Only the player that has the turn may buy
                if (!playerName.Equals(currentPlayer.Id))
                {
                    errMsg = LocalText.GetText("WrongPlayer", playerName, currentPlayer.Id);
                    break;
                }

                // Check company
                company = companyManager.GetPublicCompany(companyName);
                if (company == null)
                {
                    errMsg = LocalText.GetText("CompanyDoesNotExist", companyName);
                    break;
                }

                // The player may not have sold the company this round.
                if (currentPlayer.HasSoldThisRound(company))
                {
                    errMsg =
                        LocalText.GetText("AlreadySoldThisTurn",
                                currentPlayer.Id,
                                companyName);
                    break;
                }

                if (!company.IsBuyable())
                {
                    errMsg = LocalText.GetText("NotYetStarted", companyName);
                    break;
                }

                // The player may not have bought this turn, unless the company
                // bought before and now is in the brown area.
                PublicCompany companyBoughtThisTurn =
                    (PublicCompany)companyBoughtThisTurnWrapper.Value;
                if (companyBoughtThisTurn != null
                        && (companyBoughtThisTurn != company || !company.GetCurrentSpace().IsNoBuyLimit))
                {
                    errMsg = LocalText.GetText("AlreadyBought", playerName);
                    break;
                }

                // Check if that many shares are available
                if (shares > from.GetShare(company))
                {
                    errMsg = LocalText.GetText("NotAvailable", companyName, from.Id);
                    break;
                }

                StockSpace currentSpace;
                if (from == ipo && company.HasParPrice)
                {
                    currentSpace = company.GetStartSpace();
                }
                else
                {
                    currentSpace = company.GetCurrentSpace();
                }

                // Check if it is allowed to buy more than one certificate (if
                // requested)
                if (number > 1 && !currentSpace.IsNoBuyLimit)
                {
                    errMsg = LocalText.GetText("CantBuyMoreThanOne", companyName);
                    break;
                }

                // Check if player would not exceed the certificate limit.
                // (shortcut: assume 1 cert == 1 certificate)
                PublicCertificate cert = from.FindCertificate(company, sharePerCert / shareUnit, false);
                if (cert == null)
                {
                    log.Error("Cannot find " + sharePerCert + "% of " + company.Id + " in " + from.Id);
                }
                if (!currentSpace.IsNoCertLimit
                        && !MayPlayerBuyCertificate(currentPlayer, company, number * cert.CertificateCount))
                {
                    errMsg =
                        currentPlayer.Id
                        + LocalText.GetText("WouldExceedCertLimit", gameManager.GetPlayerCertificateLimit(currentPlayer).ToString());
                    break;
                }

                // Check if player would exceed the per-company share limit
                if (!currentSpace.IsNoHoldLimit
                        && !CheckAgainstHoldLimit(currentPlayer, company, shares))
                {
                    errMsg = LocalText.GetText("WouldExceedHoldLimit",
                            currentPlayer.Id,
                            GameDef.Parm.PLAYER_SHARE_LIMIT.DefaultValueAsInt());
                    break;
                }

                price = GetBuyPrice(action, currentSpace);
                cost = shares * price / company.ShareUnitsForSharePrice;

                // Check if the Player has the money.
                if (currentPlayer.CashValue < cost)
                {
                    errMsg = LocalText.GetText("NoMoney");
                    break;
                }

                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CantBuy",
                        playerName,
                        shares,
                        companyName,
                        from.Id,
                        errMsg));
                return false;
            }

            // All seems OK, now buy the shares.


            IMoneyOwner priceRecipient = GetSharePriceRecipient(company, from.Parent, cost);

            if (number == 1)
            {
                ReportBuffer.Add(this, LocalText.GetText("BUY_SHARE_LOG",
                        playerName,
                        share,
                        companyName,
                        from.Name,
                        Bank.Format(this, cost)));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("BUY_SHARES_LOG",
                        playerName,
                        number,
                        share,
                        shares,
                        companyName,
                        from.Name,
                        Bank.Format(this, cost)));
            }
            ReportBuffer.GetAllWaiting(this);

            PublicCertificate cert2;
            for (int i = 0; i < number; i++)
            {
                cert2 = from.FindCertificate(company, sharePerCert / shareUnit, false);
                if (cert2 == null)
                {
                    log.Error("Cannot find " + companyName + " " + shareUnit * sharePerCert
                            + "% share in " + from.Id);
                }
                cert2.MoveTo(currentPlayer);
            }

            string costText = Currency.Wire(currentPlayer, cost, priceRecipient);
            if (priceRecipient != from.MoneyOwner)
            {
                ReportBuffer.Add(this, LocalText.GetText("PriceIsPaidTo",
                        costText,
                        priceRecipient.Id));
            }

            companyBoughtThisTurnWrapper.Set(company);
            hasActed.Set(true);
            SetPriority();

            // Check if presidency has changed
            company.CheckPresidencyOnBuy(currentPlayer);

            // Check if the company has floated
            if (!company.HasFloated()) CheckFlotation(company);

            // Check for any game-specific consequences
            // (such as making another company available in the IPO)
            GameSpecificChecks(from, company);

            return true;
        }

        /** Stub, may be overridden in subclasses */
        // called by:
        // StockRound: buyShares, startCompany
        // StockRound 1880: (gameSpecificChecks)

        // overridden by:
        // StockRound 1825, 1835, 1856, 1880

        virtual protected void GameSpecificChecks(PortfolioModel boughtFrom,
                PublicCompany company)
        {

        }

        /** Allow different price setting in subclasses (i.e. 1835 Nationalization) */
        // called by:
        // StockRound: buyShares

        // overridden by:
        // StockRound 1835

        virtual protected int GetBuyPrice(BuyCertificate action, StockSpace currentSpace)
        {
            return currentSpace.Price;
        }

        /**
         * Who receives the cash when a certificate is bought.
         * With incremental capitalization, this can be the company treasure.
         * This method must be called <i>before</i> transferring the certificate.
         * @param cert
         * @return
         */
        // called by:
        // StockRound: buyShares, startCompany

        // overridden by:
        // StockRound 1856
        virtual protected IMoneyOwner GetSharePriceRecipient(PublicCompany comp,
                IOwner from, int price)
        {

            IMoneyOwner recipient;
            if (comp.HasFloated()
                    && from == ipo.Parent
                    && comp.Capitalization == PublicCompany.CAPITALIZE_INCREMENTAL)
            {
                recipient = comp;
            }
            else if (from is BankPortfolio)
            {
                recipient = bank;
            }
            else
            {
                recipient = (IMoneyOwner)from;
            }
            return recipient;
        }

        /** Make the certificates of one company available for buying
         * by putting these in the IPO.
         * @param company The company to be released.
         */
        // called by:
        // StockRound 1825, 1835: gameSpecificChecks

        // not overridden
        virtual protected void ReleaseCompanyShares(PublicCompany company)
        {
            Portfolio.MoveAll(unavailable.GetCertificates(company), ipo.Parent);
        }

        // called by:
        // StockRound: process
        // StockRound 1880: (sellsShares)

        // overridden by:
        // ShareSellingRound
        // TreasuryShareRound
        // StockRound 1880
        // ShareSellingRound 1880
        virtual public bool DoSellShares(SellShares action)
        // NOTE: Don't forget to keep ShareSellingRound.sellShares() in sync
        {

            PortfolioModel portfolio = currentPlayer.PortfolioModel;
            string playerName = currentPlayer.Id;
            string errMsg = null;
            string companyName = action.CompanyName;
            PublicCompany company = companyManager.GetPublicCompany(action.CompanyName);
            PublicCertificate presCert = null;
            List<PublicCertificate> certsToSell = new List<PublicCertificate>();
            Player dumpedPlayer = null;
            int presidentShareNumbersToSell = 0;
            int numberToSell = action.Number;
            int shareUnits = action.ShareUnits;

            // Dummy loop to allow a quick jump out
            while (true)
            {

                // Check everything
                if (CheckFirstRoundSellRestriction())
                {
                    errMsg = LocalText.GetText("FirstSRNoSell");
                    break;
                }
                if (numberToSell <= 0)
                {
                    errMsg = LocalText.GetText("NoSellZero");
                    break;
                }

                // May not sell in certain cases
                if (!MayCurrentPlayerSellAnything())
                {
                    errMsg = LocalText.GetText("SoldEnough");
                    break;
                }

                // Check company
                if (company == null)
                {
                    errMsg = LocalText.GetText("NoCompany");
                    break;
                }

                // May player sell this company
                if (!MayPlayerSellShareOfCompany(company))
                {
                    errMsg = LocalText.GetText("SaleNotAllowed", companyName);
                    break;
                }

                // The player must have the share(s)
                if (portfolio.GetShare(company) < numberToSell)
                {
                    errMsg = LocalText.GetText("NoShareOwned");
                    break;
                }

                // The pool may not get over its limit.
                if (pool.GetShare(company) + numberToSell * company.GetShareUnit()
                        > GameDef.GetGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT))
                {
                    errMsg = LocalText.GetText("PoolOverHoldLimit");
                    break;
                }

                // Find the certificates to sell

                // ... check if there is a dump required
                // Player is president => dump is possible
                if (currentPlayer == company.GetPresident() && shareUnits == 1)
                {
                    dumpedPlayer = company.FindPlayerToDump();
                    if (dumpedPlayer != null)
                    {
                        presidentShareNumbersToSell = PlayerShareUtils.PresidentShareNumberToSell(
                                company, currentPlayer, dumpedPlayer, numberToSell);
                        // reduce the numberToSell by the president (partial) sold certificate
                        numberToSell -= presidentShareNumbersToSell;
                    }
                }

                certsToSell = PlayerShareUtils.FindCertificatesToSell(company, currentPlayer, numberToSell, shareUnits);

                // reduce numberToSell to double check
                foreach (PublicCertificate c in certsToSell)
                {
                    numberToSell -= c.GetShares();
                }

                // Check if we could sell them all
                if (numberToSell > 0)
                {
                    if (presCert != null)
                    {
                        errMsg = LocalText.GetText("NoDumping");
                    }
                    else
                    {
                        errMsg = LocalText.GetText("NotEnoughShares");
                    }
                    break;
                }

                break;
            }

            int numberSold = action.Number;
            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CantSell",
                        playerName,
                        numberSold,
                        companyName,
                        errMsg));
                return false;
            }

            // All seems OK, now do the selling.

            // Selling price
            int price = GetCurrentSellPrice(company);
            int cashAmount = numberSold * price * shareUnits;

            // Save original price as it may be reused in subsequent sale actions in the same turn
            bool soldBefore = sellPrices.ContainsKey(company);
            if (!soldBefore)
            {
                sellPrices.Put(company, company.GetCurrentSpace());
            }



            string cashText = Currency.FromBank(cashAmount, currentPlayer);
            if (numberSold == 1)
            {
                ReportBuffer.Add(this, LocalText.GetText("SELL_SHARE_LOG",
                        playerName,
                        company.GetShareUnit() * shareUnits,
                        companyName,
                        cashText));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("SELL_SHARES_LOG",
                        playerName,
                        numberSold,
                        company.GetShareUnit() * shareUnits,
                        numberSold * company.GetShareUnit() * shareUnits,
                        companyName,
                        cashText));
            }

            AdjustSharePrice(company, numberSold, soldBefore);

            if (!company.IsClosed())
            {

                ExecuteShareTransfer(company, certsToSell,
                        dumpedPlayer, presidentShareNumbersToSell);
            }

            // Remember that the player has sold this company this round.
            currentPlayer.SetSoldThisRound(company);

            if (companyBoughtThisTurnWrapper.Value == null)
                hasSoldThisTurnBeforeBuying.Set(true);
            hasActed.Set(true);
            SetPriority();

            return true;
        }

        // FIXME: Rails 2.x This has to be rewritten to give the new presidency a choice which shares to swap (if he has multiple share certificates)
        // called by:
        // StockRound: executeShareTransfer
        // StockRound 1880: executeShareTransfer
        // ShareSellingRound 1880: executeShareTransfer

        // not overridden
        protected void ExecuteShareTransferTo(PublicCompany company,
                List<PublicCertificate> certsToSell, Player dumpedPlayer, int presSharesToSell,
                BankPortfolio bankTo)
        {

            // Check if the presidency has changed
            if (dumpedPlayer != null && presSharesToSell > 0)
            {

                PlayerShareUtils.ExecutePresidentTransferAfterDump(company, dumpedPlayer, bankTo, presSharesToSell);

                ReportBuffer.Add(this, LocalText.GetText("IS_NOW_PRES_OF",
                        dumpedPlayer.Id,
                        company.Id));

            }

            // Transfer the sold certificates
            Portfolio.MoveAll(certsToSell, bankTo);

        }

        // called by:
        // StockRound: sellShares
        // ShareSellingRound. sellShares

        // overridden by
        // StockRound 1880
        // ShareSellingRound 1880
        virtual protected void ExecuteShareTransfer(PublicCompany company,
                List<PublicCertificate> certsToSell,
                Player dumpedPlayer, int presSharesToSell)
        {

            ExecuteShareTransferTo(company, certsToSell, dumpedPlayer, presSharesToSell, (BankPortfolio)pool.Parent);
        }

        // called by:
        // StockRound: sellShares, setSellableShares

        // overridden by:
        // StockRound 1835

        virtual protected int GetCurrentSellPrice(PublicCompany company)
        {

            int price;

            if (sellPrices.ContainsKey(company)
                    && GameOption.GetAsBoolean(this, "SeparateSalesAtSamePrice"))
            {
                price = (sellPrices.Get(company)).Price;
            }
            else
            {
                price = company.GetCurrentSpace().Price;
            }
            // stored price is the previous unadjusted price
            price = price / company.ShareUnitsForSharePrice;
            return price;
        }

        // called by:
        // StockRound: sellShares
        // StockRound 1835, 1856, 1880: (adjustSharePrice)
        // ShareSellingRound 1856: (adjustSharePrice)
        // ShareSellingRound: sellShares
        // ShareSellingRound 1880: sellShares

        // overridden by:
        // StockRound 1825, 1835, 1856, 1880
        // ShareSellingRound 1856
        virtual protected void AdjustSharePrice(PublicCompany company, int numberSold, bool soldBefore)
        {
            if (!company.CanSharePriceVary()) return;

            stockMarket.Sell(company, numberSold);

            StockSpace newSpace = company.GetCurrentSpace();

            if (newSpace.ClosesCompany && company.CanClose)
            {
                company.SetClosed();
                ReportBuffer.Add(this, LocalText.GetText("CompanyClosesAt", company.Id, newSpace.Id));
                return;
            }

            // Company is still open

        }

        // called by:
        // StockRound: process

        // not overridden
        virtual public bool DoUseSpecialProperty(UseSpecialProperty action)
        {
            SpecialProperty sp = action.SpecialProperty;

            // TODO This should work for all subclasses, but not all have execute()
            // yet.
            if (sp is ExchangeForShare)
            {

                bool result = ExecuteExchangeForShare(action, (ExchangeForShare)sp);
                if (result) hasActed.Set(true);
                return result;

            }
            else
            {
                return false;
            }
        }

        // TODO: Check if this still does work, there is a cast involved now

        // called by:
        // StockRound: useSpecialProperty

        // not overridden
        virtual public bool ExecuteExchangeForShare(UseSpecialProperty action, ExchangeForShare sp)
        {

            PublicCompany publicCompany =
                companyManager.GetPublicCompany(sp.PublicCompanyName);
            PrivateCompany privateCompany = (PrivateCompany)sp.OriginalCompany;
            IOwner owner = privateCompany.Owner;
            Player player = null;
            string errMsg = null;
            bool ipoHasShare = ipo.GetShare(publicCompany) >= sp.Share;
            bool poolHasShare = pool.GetShare(publicCompany) >= sp.Share;

            while (true)
            {

                /* Check if the private is owned by a player */
                if (!(owner is Player))
                {
                    errMsg = LocalText.GetText("PrivateIsNotOwnedByAPlayer", privateCompany.Id);
                    break;
                }

                player = (Player)owner;

                /* Check if a share is available */
                if (!ipoHasShare && !poolHasShare)
                {
                    errMsg = LocalText.GetText("NoSharesAvailable", publicCompany.Id);
                    break;
                }
                /* Check if the player has room for a share of this company */
                if (!CheckAgainstHoldLimit(player, publicCompany, 1))
                {
                    // TODO: Not nice to use '1' here, should be percentage.
                    errMsg = LocalText.GetText("WouldExceedHoldLimit",
                                GameDef.GetGameParameterAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT).ToString());
                    break;
                }
                break;
            }
            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText(
                        "CannotSwapPrivateForCertificate",
                        player.Id,
                        privateCompany.Id,
                        sp.Share,
                        publicCompany.Id,
                        errMsg));
                return false;
            }



            ICertificate cert =
                ipoHasShare ? ipo.FindCertificate(publicCompany,
                        false) : pool.FindCertificate(publicCompany,
                                false);
            cert.MoveTo(player);
            ReportBuffer.Add(this, LocalText.GetText("SwapsPrivateForCertificate",
                    player.Id,
                    privateCompany.Id,
                    sp.Share,
                    publicCompany.Id));
            sp.SetExercised();
            privateCompany.SetClosed();

            // Check if the company has floated
            if (!publicCompany.HasFloated()) CheckFlotation(publicCompany);

            return true;
        }

        /**
         * The current Player passes or is done.
         * @param action TODO
         * @param player Name of the passing player.
         *
         * @return False if an error is found.
         */
        // called by
        // StockRound: finishTurn, process

        // overridden by
        // StockRound 1837, 18EU
        // TreasuryShareRound
        virtual public bool Done(NullAction action, string playerName, bool hasAutopassed)
        {

            //currentPlayer = getCurrentPlayer();

            if (!playerName.Equals(currentPlayer.Id))
            {
                DisplayBuffer.Add(this, LocalText.GetText("WrongPlayer", playerName, currentPlayer.Id));
                return false;
            }

            if (hasActed.Value)
            {
                numPasses.Set(0);
            }
            else
            {
                numPasses.Add(1);
                if (hasAutopassed)
                {
                    if (!HasAutopassed(currentPlayer))
                    {
                        SetAutopass(currentPlayer, true);
                        SetCanRequestTurn(currentPlayer, true);
                    }
                    ReportBuffer.Add(this, LocalText.GetText("Autopasses",
                            currentPlayer.Id));
                }
                else
                {
                    ReportBuffer.Add(this, LocalText.GetText("PASSES",
                            currentPlayer.Id));
                }
            }

            if (numPasses.Value >= PlayerManager.GetNumberOfActivePlayers(this))
            {

                FinishRound();

            }
            else
            {

                FinishTurn();

            }
            return true;
        }

        // called by:
        // StockRound: done
        // StockRound 18367, 18EU: (finishRound)

        // overridden by:
        // StockRound 1837, 1880, 18EU
        // NationalFormationRound, PrussianFormationRound

        override protected void FinishRound()
        {
            ReportBuffer.Add(this, " ");
            ReportBuffer.Add(this, LocalText.GetText("END_SR", StockRoundNumber.ToString()));

            if (raiseIfSoldOut)
            {
                /* Check if any companies are sold out. */
                foreach (PublicCompany company in gameManager.GetCompaniesInRunningOrder())
                {
                    if (company.HasStockPrice && company.IsSoldOut())
                    {
                        StockSpace oldSpace = company.GetCurrentSpace();
                        stockMarket.SoldOut(company);
                        StockSpace newSpace = company.GetCurrentSpace();
                        if (newSpace != oldSpace)
                        {
                            ReportBuffer.Add(this, LocalText.GetText("SoldOut",
                                company.Id,
                                Bank.Format(this, oldSpace.Price),
                                oldSpace.Id,
                                Bank.Format(this, newSpace.Price),
                                newSpace.Id));
                        }
                        else
                        {
                            ReportBuffer.Add(this, LocalText.GetText("SoldOutNoRaise",
                                company.Id,
                                Bank.Format(this, newSpace.Price),
                                newSpace.Id));
                        }
                    }
                }
            }

            // reset soldThisRound
            foreach (Player player in playerManager.Players)
            {
                player.ResetSoldThisRound();
            }


            base.FinishRound();
        }

        // called by:
        // StockRound: process

        // not overridden
        protected bool DoRequestTurn(RequestTurn action)
        {

            Player requestingPlayer = playerManager.GetPlayerByName(action.RequestingPlayerName);

            bool result = CanRequestTurn(requestingPlayer);

            if (!result)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CannotRequestTurn",
                        requestingPlayer.Id));
                return false;
            }


            if (HasAutopassed(requestingPlayer))
            {
                SetAutopass(requestingPlayer, false);
            }
            else
            {
                DoRequestTurn(requestingPlayer); // TODO: Check if this still works, replaces requestTurn.add(..)
            }

            return true;
        }


        // called by:
        // StockRound: done
        // StockRound 1837, 18EU: (finishTurn)

        // overridden by:
        // StockRound 1837, 18EU

        virtual protected void FinishTurn()
        {

            SetNextPlayer();
            sellPrices.Clear();
            if (HasAutopassed(currentPlayer))
            {
                if (IsPlayerOverLimits(currentPlayer))
                {
                    // Being over a share/certificate limit undoes an Autopass setting
                    SetAutopass(currentPlayer, false);
                }
                else
                {
                    // Process a pass for a player that has set Autopass
                    Done(null, currentPlayer.Id, true);
                }
            }
        }

        /**
         * Internal method: pass the turn to the next player.
         */

        // called by
        // StockRound: finishTurn
        // NationalFormationRound, 1835 PrussianFormationRound: findNextMergingPlayer
        // 1837FinalCoalExchangeRound: setMinorMergeActions
        // 18EUFinalMInorExchangeRound: setMinorMergeActions

        // not overridden
        protected void SetNextPlayer()
        {

            GetRoot.PlayerManager.SetCurrentToNextPlayer();
            InitPlayer();
        }

        // called by
        // StockRound: setNextPlayer, start
        // StockRound 1856: (initPlayer)

        // overridden by:
        // StockRound 1837, 1856, 18EU
        // FinalCoalExchangeRound
        // FinalMinorExchangeRound
        virtual protected void InitPlayer()
        {

            currentPlayer = playerManager.CurrentPlayer;
            companyBoughtThisTurnWrapper.Set(null);
            hasSoldThisTurnBeforeBuying.Set(false);
            hasActed.Set(false);
            if (currentPlayer == startingPlayer) ReportBuffer.Add(this, "");
        }

        /**
         * Remember the player that has the Priority Deal. <b>Must be called BEFORE
         * setNextPlayer()!</b>
         */
        // called by
        // StockRound: buyShares, sellShares, startCompany
        // StockRound 18EU: mergeCompanies, startCompany
        // StockRound 1837: mergeCompanies

        // not overridden
        protected void SetPriority()
        {
            GetRoot.PlayerManager.SetPriorityPlayerToNext();
        }

        // called by
        // Stockround 1837, 18EU: finishRound, finishTurn
        // NationalFormationRound, 1835PrussianFormationRound: setPossibleActions, start

        // not overridden
        [Obsolete]
        public void SetCurrentPlayer(Player player)
        {
            GetRoot.PlayerManager.SetCurrentPlayer(player);
            currentPlayer = player;
        }

        /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

        /**
         * @return The index of the player that has the turn.
         */
        // called by
        // ShareSellingRound 1880: sellShares

        // not overridden
        public int CurrentPlayerIndex
        {
            get
            {
                return currentPlayer.Index;
            }
        }

        /**
         * @return true if first round sell restriction is active
         */
        // called by
        // StockRound: mayCurrentPlayerSellAnything, sellShares

        // not overridden
        private bool CheckFirstRoundSellRestriction()
        {
            if (NoSaleInFirstSR && StockRoundNumber == 1)
            {
                // depending on GameOption restriction is either valid during the first (true) Stock Round or the first Round 
                if (GameOption.GetValue(this, "FirstRoundSellRestriction").Equals("First Stock Round"))
                {
                    return true;
                }
                else if (GameOption.GetValue(this, "FirstRoundSellRestriction").Equals("First Round"))
                {
                    // if all players have passed it is not the first round
                    return !gameManager.GetFirstAllPlayersPassed();
                }
            }
            return false;
        }

        /**
         * Can the current player do any selling?
         *
         * @return True if any selling is allowed.
         */
        // called by
        // StockRound: sellShares, sellSellableShares

        // overridden by
        // ShareSellingRound
        // TreasuryShareRound
        virtual public bool MayCurrentPlayerSellAnything()
        {

            if (CheckFirstRoundSellRestriction())
            {
                return false;
            }

            if (companyBoughtThisTurnWrapper.Value != null
                    && (sequenceRule == SELL_BUY_OR_BUY_SELL
                            && hasSoldThisTurnBeforeBuying.Value || sequenceRule == SELL_BUY))
            {
                return false;
            }
            return true;
        }


        // called by 
        // StockRound: sellShares, setSellableShares
        // StockRound 1880: (mayPlayerSellShareOfCompany), sellShares
        // ShareSellingRound: getSellableShares

        // overridden by
        // StockRound 1880
        public bool MayPlayerSellShareOfCompany(PublicCompany company)
        {

            // Can't sell shares that have no price
            if (!company.HasStarted() || !company.HasStockPrice) return false;

            // In some games, can't sell shares if not operated
            if (NoSaleIfNotOperated
                    && !company.HasOperated()) return false;

            return true;
        }


        /**
         * Can the current player do any buying?
         * <p>Note: requires sellable shares to be checked BEFORE buyable shares
         *
         * @return True if any buying is allowed.
         */
        // called by 
        // StockRound: setBuyableCerts
        // StockRound 1880, 18EU: setBuyableCerts
        // StockRound 1837, 18EU: setGameSpecificActions

        // overridden by
        // ShareSellingRound
        // TreasuryShareRound
        virtual public bool MayCurrentPlayerBuyAnything()
        {
            return !isOverLimits && companyBoughtThisTurnWrapper.Value == null;
        }

        // Only used now to check if Autopass must be reset.
        // called by
        // StockRound: finishTurn

        // not overridden
        protected bool IsPlayerOverLimits(Player player)
        {

            // Over the total certificate hold Limit?
            if (player.PortfolioModel.GetCertificateCount() > gameManager.GetPlayerCertificateLimit(player))
            {
                return true;
            }

            // Over the hold limit of any company?
            foreach (PublicCompany company in companyManager.GetAllPublicCompanies())
            {
                if (company.HasStarted() && company.HasStockPrice
                        && !CheckAgainstHoldLimit(player, company, 0))
                {
                    return true;
                }
            }

            return false;
        }

        /**
         * Check if a player may buy the given number of certificates.
         *
         * @param number Number of certificates to buy (usually 1 but not always
         * so).
         * @return True if it is allowed.
         */
        // called by 
        // StockRound: buyShares, setBuyableCerts, startCompany
        // StockRound 1835, 1880, 18EU: setBuyableCerts
        // StockRound 18EU: startCompany

        // not overridden
        public bool MayPlayerBuyCertificate(Player player, PublicCompany comp, float number)
        {
            if (comp.HasFloated() && comp.GetCurrentSpace().IsNoCertLimit)
                return true;
            if (player.PortfolioModel.GetCertificateCount() + number > gameManager.GetPlayerCertificateLimit(player))
                return false;
            return true;
        }

        /**
         * Check if a player may buy the given number of shares from a given
         * company, given the "hold limit" per company, that is the percentage of
         * shares of one company that a player may hold (typically 60%).
         *
         * @param player the buying player
         * @param company The company from which to buy
         * @param number The number of shares (usually 1 but not always so)
         * @return True if it is allowed.
         */
        // called by 
        // StockRound: buyShares, executeExchangeForShare, isPlayerOverLimits, setBuyableCerts, setSellableShares
        // StockRound 18EU, 1880: setBuyableCerts

        // overridden by:
        // StockRound 1835
        virtual public bool CheckAgainstHoldLimit(Player player, PublicCompany company, int number)
        {
            // Check for per-company share limit
            if (player.PortfolioModel.GetShare(company)
                    + number * company.GetShareUnit()
                    > GameDef.GetGameParameterAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)
                    && !company.GetCurrentSpace().IsNoHoldLimit
                    && !IsSellObligationLifted(company)) return false;
            return true;
        }

        /**
         * Return the number of <i>additional</i> shares of a certain company and
         * of a certain size that a player may buy, given the share "hold limit" per
         * company, that is the percentage of shares of one company that a player
         * may hold (typically 60%). <p>If no hold limit applies, it is taken to be
         * 100%.
         *
         * @param company The company from which to buy
         * @param number The share unit (typically 10%).
         * @return The maximum number of such shares that would not let the player
         * overrun the per-company share hold limit.
         */
        // called by 
        // StockRound setBuyableCerts
        // StockRound 1880, 18EU: setBuyableCerts

        // not overridden
        public int MaxAllowedNumberOfSharesToBuy(Player player, PublicCompany company, int shareSize)
        {
            int limit;
            int playerShareLimit = GameDef.GetGameParameterAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT);
            if (!company.HasStarted())
            {
                limit = playerShareLimit;
            }
            else
            {
                limit =
                    company.GetCurrentSpace().IsNoHoldLimit ? 100
                            : playerShareLimit;
            }
            int maxAllowed = (limit - player.PortfolioModel.GetShare(company)) / shareSize;
            //        log.debug("MaxAllowedNumberOfSharesToBuy = " + maxAllowed + " for company =  " + company + " shareSize " + shareSize);
            return maxAllowed;
        }


        // called by 
        // StockRound: checkFirstRoundSellRestriction

        // not overridden
        protected bool NoSaleInFirstSR
        {
            get
            {
                return (bool)gameManager.GetGameParameter(GameDef.Parm.NO_SALE_IN_FIRST_SR);
            }
        }


        // called by 
        // StockRound: mayPlayerSellShareOfCompany

        // not overridden
        protected bool NoSaleIfNotOperated
        {
            get
            {
                return (bool)gameManager.GetGameParameter(GameDef.Parm.NO_SALE_IF_NOT_OPERATED);
            }
        }

        // called by
        // 1835PrussianFormationRound: finishRound
        // GameManager: processOnReload 
        // GameUIManager: initSaveSettings, saveGame 

        // not overridden
        override public string RoundName
        {
            get
            {
                return "StockRound " + StockRoundNumber;
            }
        }

        // Called by 
        // StockRound: checkAgainstHoldLimit

        // not overridden
        public bool IsSellObligationLifted(PublicCompany company)
        {
            return sellObligationLifted != null
            && sellObligationLifted.Contains(company);
        }

        // Called by 
        // 18EU, 1837 StockRound: mergeCompanies

        // not overridden
        public void SetSellObligationLifted(PublicCompany company)
        {
            if (sellObligationLifted == null)
            {
                sellObligationLifted = HashSetState<PublicCompany>.Create(this, "sellObligationLifted");
            }
            sellObligationLifted.Add(company);
        }

        public bool DoRequestTurn(Player player)
        {
            if (CanRequestTurn(player))
            {
                if (!hasRequestedTurn.Contains(player)) hasRequestedTurn.Add(player);
                return true;
            }
            return false;
        }

        public bool CanRequestTurn(Player player)
        {
            return canRequestTurn.Contains(player);
        }

        public void SetCanRequestTurn(Player player, bool value)
        {
            if (value && !canRequestTurn.Contains(player))
            {
                canRequestTurn.Add(player);
            }
            else if (!value && canRequestTurn.Contains(player))
            {
                canRequestTurn.Remove(player);
            }
        }

        public void SetAutopass(Player player, bool value)
        {
            if (value && !autopasses.Contains(player))
            {
                autopasses.Add(player);
            }
            else if (!value && autopasses.Contains(player))
            {
                autopasses.Remove(player);
            }
        }

        public bool HasAutopassed(Player player)
        {
            return autopasses.Contains(player);
        }

        public IReadOnlyCollection<Player> GetAutopasses()
        {
            return autopasses.View();
        }

    }
}
