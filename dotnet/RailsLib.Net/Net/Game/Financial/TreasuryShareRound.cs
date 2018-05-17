using GameLib.Net.Common;
using GameLib.Net.Game.GameRound;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

// Change: TreasuryShareRound is a workaround as StockRound
// It is a single Activity to allow companies buying or selling shares

namespace GameLib.Net.Game.Financial
{
    public class TreasuryShareRound : StockRound, ICreatable
    {
        protected Player sellingPlayer;
        protected PublicCompany operatingCompany;
        private BooleanState hasBought;
        private BooleanState hasSold;

        /**
         * Created via Configure
         */
        public TreasuryShareRound(GameManager parent, string id) : base(parent, id)
        {
            hasBought = BooleanState.Create(this, "hasBought");
            hasSold = BooleanState.Create(this, "hasSold");

            guiHints.ActivePanel = GuiDef.Panel.STATUS;
        }

        // TODO: Check if this still works, as the initialization was moved back to here
        public void Start(IRoundFacade parentRound)
        {
            log.Info("Treasury share trading round started");
            operatingCompany = ((OperatingRound)parentRound).GetOperatingCompany();
            sellingPlayer = operatingCompany.GetPresident();
            GetRoot.PlayerManager.SetCurrentPlayer(sellingPlayer);
            currentPlayer = sellingPlayer;
        }

        override public bool MayCurrentPlayerSellAnything()
        {
            return false;
        }

        override public bool MayCurrentPlayerBuyAnything()
        {
            return false;
        }

        override public bool SetPossibleActions()
        {

            possibleActions.Clear();

            if (operatingCompany.MustHaveOperatedToTradeShares
                    && !operatingCompany.HasOperated()) return true;

            if (!hasSold.Value) SetBuyableCerts();
            if (!hasBought.Value) SetSellableCerts();

            if (possibleActions.IsEmpty)
            {
                // TODO Finish the round before it started...
            }

            possibleActions.Add(new NullAction(NullAction.Modes.DONE));

            foreach (PossibleAction pa in possibleActions.GetList())
            {
                log.Debug(operatingCompany.Id + " may: " + pa.ToString());
            }

            return true;
        }

        /**
         * Create a list of certificates that a player may buy in a Stock Round,
         * taking all rules into account.
         *
         * @return List of buyable certificates.
         */
        override public void SetBuyableCerts()
        {
            ICollection<PublicCertificate> certs;
            PublicCertificate cert;
            PortfolioModel from;
            int price;
            int number;

            int cash = operatingCompany.Cash;

            /* Get the unique Pool certificates and check which ones can be bought */
            from = pool;
            MultiDictionaryBase<PublicCompany, PublicCertificate> map = from.GetCertsPerCompanyMap();

            foreach (PublicCompany comp in map.Keys)
            {
                certs = map[comp];
                // if (certs.isEmpty()) continue; // TODO: Check if removal is correct 
                var it = certs.GetEnumerator();
                it.MoveNext();
                cert = it.Current; //Iterables.get(certs, 0);

                // TODO For now, only consider own certificates.
                // This will have to be revisited with 1841.
                if (comp != operatingCompany) continue;

                // Shares already owned
                int ownedShare = operatingCompany.PortfolioModel.GetShare(operatingCompany);
                // Max share that may be owned
                int maxShare = GameDef.GetGameParameterAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT);
                // Max number of shares to add
                int maxBuyable = (maxShare - ownedShare) / operatingCompany.GetShareUnit();
                // Max number of shares to buy
                number = Math.Min(certs.Count, maxBuyable);
                if (number == 0) continue;

                price = comp.GetMarketPrice();

                // Does the company have enough cash?
                while (number > 0 && cash < number * price)
                    number--;

                if (number > 0)
                {
                    possibleActions.Add(new BuyCertificate(comp, cert.Share, from.Parent, price, number));
                }
            }
        }

        /**
         * Create a list of certificates that the company may sell, taking all rules
         * taken into account. <br>Note: old code that provides for ownership of
         * presidencies of other companies has been retained, but not tested. This
         * code will be needed for 1841.
         *
         * @return List of sellable certificates.
         */
        public void SetSellableCerts()
        {
            int price;
            int number;
            int maxShareToSell;

            PortfolioModel companyPortfolio = operatingCompany.PortfolioModel;

            /*
             * First check of which companies the player owns stock, and what
             * maximum percentage he is allowed to sell.
             */
            foreach (PublicCompany company in companyManager.GetAllPublicCompanies())
            {
                // Can't sell shares that have no price
                if (!company.HasStarted()) continue;

                maxShareToSell = companyPortfolio.GetShare(company);
                if (maxShareToSell == 0) continue;

                /* May not sell more than the Pool can accept */
                maxShareToSell =
                    Math.Min(maxShareToSell,
                            GameDef.GetGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)
                            - pool.GetShare(company));
                if (maxShareToSell == 0) continue;

                /*
                 * Check what share units the player actually owns. In some games
                 * (e.g. 1835) companies may have different ordinary shares: 5% and
                 * 10%, or 10% and 20%. The president's share counts as a multiple
                 * of the lowest ordinary share unit type.
                 */
                // Take care for max. 4 share units per share
                int[] shareCountPerUnit = new int[5];
                foreach (PublicCertificate c in companyPortfolio.GetCertificates(company))
                {
                    if (c.IsPresidentShare)
                    {
                        shareCountPerUnit[1] += c.GetShares();
                    }
                    else
                    {
                        ++shareCountPerUnit[c.GetShares()];
                    }
                }
                // TODO The above ignores that a dumped player must be
                // able to exchange the president's share.

                /*
                 * Check the price. If a cert was sold before this turn, the
                 * original price is still valid
                 */
                if (sellPrices.ContainsKey(company))
                {
                    price = (sellPrices.Get(company)).Price;
                }
                else
                {
                    price = company.GetMarketPrice();
                }

                for (int shareSize = 1; shareSize <= 4; shareSize++)
                {
                    number = shareCountPerUnit[shareSize];
                    if (number == 0) continue;
                    number =
                        Math.Min(number, maxShareToSell
                                / (shareSize * company.GetShareUnit()));
                    if (number == 0) continue;

                    for (int i = 1; i <= number; i++)
                    {
                        possibleActions.Add(new SellShares(company, shareSize, i, price));
                    }
                }
            }
        }

        /**
         * Buying one or more single or double-share certificates (more is sometimes
         * possible)
         *
         * @param player The player that wants to buy shares.
         * @param action The executed action
         * @return True if the certificates could be bought. False indicates an
         * error.
         */
        override public bool DoBuyShares(string playerName, BuyCertificate action)
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
            // TODO: Might not be needed anymore, replaced by company
            PortfolioModel portfolio = null;

            currentPlayer = playerManager.CurrentPlayer;

            // Dummy loop to allow a quick jump out
            while (true)
            {

                // Check everything
                // Only the player that has the turn may act
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
                if (company != operatingCompany)
                {
                    errMsg =
                        LocalText.GetText("WrongCompany",
                                companyName,
                                    operatingCompany.Id);

                }

                // The company must have floated
                if (!company.HasFloated())
                {
                    errMsg = LocalText.GetText("NotYetFloated", companyName);
                    break;
                }
                if (company.MustHaveOperatedToTradeShares
                        && !company.HasOperated())
                {
                    errMsg = LocalText.GetText("NotYetOperated", companyName);
                    break;
                }

                // Company may not buy after sell
                if (hasSold.Value)
                {
                    errMsg = LocalText.GetText("MayNotBuyAndSell", companyName);
                    break;
                }

                // Check if that many shares are available
                if (share > from.GetShare(company))
                {
                    errMsg = LocalText.GetText("NotAvailable", companyName, from.Id);
                    break;
                }

                portfolio = operatingCompany.PortfolioModel;

                // Check if company would exceed the per-company share limit
                int treasuryShareLimit = GameDef.GetGameParameterAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT);
                if (portfolio.GetShare(company) + share > treasuryShareLimit)
                {
                    errMsg = LocalText.GetText("TreasuryOverHoldLimit", treasuryShareLimit.ToString());
                    break;
                }

                price = company.GetMarketPrice();

                // Check if the Player has the money.
                if (operatingCompany.Cash < shares * price)
                {
                    errMsg = LocalText.GetText("NoMoney");
                    break;
                }

                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CantBuy",
                        companyName,
                        shares,
                        companyName,
                        from.Id,
                        errMsg));
                return false;
            }

            // All seems OK, now buy the shares.


            int cashAmount = shares * price;
            string cashText = Currency.ToBank(company, cashAmount);
            if (number == 1)
            {
                ReportBuffer.Add(this, LocalText.GetText("BUY_SHARE_LOG",
                        companyName,
                        shareUnit,
                        companyName,
                        from.Name,
                        cashText));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("BUY_SHARES_LOG",
                        companyName,
                        number,
                        shareUnit,
                        number * shareUnit,
                        companyName,
                        from.Name,
                        cashText));
            }

            PublicCertificate cert2;
            for (int i = 0; i < number; i++)
            {
                cert2 = from.FindCertificate(company, sharePerCert / shareUnit, false);
                cert2.MoveTo(company);
            }

            hasBought.Set(true);

            return true;
        }

        override public bool DoSellShares(SellShares action)
        {
            PortfolioModel portfolio = operatingCompany.PortfolioModel;
            string errMsg = null;
            string companyName = action.CompanyName;
            PublicCompany company = companyManager.GetPublicCompany(companyName);
            PublicCertificate cert = null;
            List<PublicCertificate> certsToSell = new List<PublicCertificate>();
            int numberToSell = action.Number;
            int shareUnits = action.ShareUnits;

            // Dummy loop to allow a quick jump out
            while (true)
            {

                // Check everything
                if (numberToSell <= 0)
                {
                    errMsg = LocalText.GetText("NoSellZero");
                    break;
                }

                // Check company
                if (company == null)
                {
                    errMsg = LocalText.GetText("NoCompany");
                    break;
                }
                if (company != operatingCompany)
                {
                    errMsg =
                        LocalText.GetText("WrongCompany",
                                companyName, operatingCompany.Id);
                    break;
                }

                // The company must have floated
                if (!company.HasFloated())
                {
                    errMsg = LocalText.GetText("NotYetFloated", companyName);
                    break;
                }
                if (company.MustHaveOperatedToTradeShares
                        && !company.HasOperated())
                {
                    errMsg = LocalText.GetText("NotYetOperated", companyName);
                    break;
                }

                // Company may not sell after buying
                if (hasBought.Value)
                {
                    errMsg = LocalText.GetText("MayNotBuyAndSell", companyName);
                    break;
                }

                // The company must have the share(s)
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
                IEnumerator<PublicCertificate> it = portfolio.GetCertificates(company).GetEnumerator();
                while (numberToSell > 0 && it.MoveNext())
                {
                    cert = it.Current;
                    if (shareUnits != cert.GetShares())
                    {
                        // Wrong number of share units
                        continue;
                    }
                    // OK, we will sell this one
                    certsToSell.Add(cert);
                    numberToSell--;
                }

                // Check if we could sell them all
                if (numberToSell > 0)
                {
                    errMsg = LocalText.GetText("NotEnoughShares");
                    break;
                }

                break;
            }

            int numberSold = action.Number;
            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CantSell",
                        companyName,
                        numberSold,
                        companyName,
                        errMsg));
                return false;
            }

            // All seems OK, now do the selling.
            StockSpace sellPrice;
            int price;

            // Get the sell price (does not change within a turn)
            if (sellPrices.ContainsKey(company))
            {
                price = (sellPrices.Get(company)).Price;
            }
            else
            {
                sellPrice = company.GetCurrentSpace();
                price = sellPrice.Price;
                sellPrices.Put(company, sellPrice);
            }

            int cashAmount = numberSold * price;
            string cashText = Currency.FromBank(cashAmount, company);
            ReportBuffer.Add(this, LocalText.GetText("SELL_SHARES_LOG",
                    companyName,
                    numberSold,
                    company.GetShareUnit(),
                    (numberSold * company.GetShareUnit()),
                    companyName,
                    cashText));

            // Transfer the sold certificates
            Portfolio.MoveAll(certsToSell, pool.Parent);
            /*
            for (PublicCertificate cert2 : certsToSell) {
                if (cert2 != null) {
                     transferCertificate (cert2, pool, cert2.getShares() * price);
                }
            }
             */
            stockMarket.Sell(company, numberSold);

            hasSold.Set(true);

            return true;
        }

        /**
         * The current Player passes or is done.
         * @param player Name of the passing player.
         *
         * @return False if an error is found.
         */
        // Autopassing does not apply here
        override public bool Done(NullAction action, string playerName, bool hasAutopassed)
        {

            currentPlayer = playerManager.CurrentPlayer;

            if (!playerName.Equals(currentPlayer.Id))
            {
                DisplayBuffer.Add(this, LocalText.GetText("WrongPlayer", playerName, currentPlayer.Id));
                return false;
            }



            // Inform GameManager
            gameManager.FinishTreasuryShareRound();

            return true;
        }

        public PublicCompany OperatingCompany
        {
            get
            {
                return this.operatingCompany;
            }
        }

        override public string ToString()
        {
            return "TreasuryShareRound";
        }
    }
}
