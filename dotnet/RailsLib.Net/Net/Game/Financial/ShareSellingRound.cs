using GameLib.Net.Common;
using GameLib.Net.Game.GameRound;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Text;

// TODO: Check if un-initialized states cause undo problems

namespace GameLib.Net.Game.Financial
{
    public class ShareSellingRound : StockRound
    {
        protected IRoundFacade parentRound;
        protected Player sellingPlayer;
        protected IntegerState cashToRaise; // initialized later
        protected PublicCompany cashNeedingCompany;
        protected bool dumpOtherCompaniesAllowed;

        protected List<SellShares> sellableShares;

        /**
         * Created using Configure
         */
        // change: ShareSellingRound is not really a (full) Round, only a single player acting
        // requires: make an independent Round for EnforcedSelling that uses the selling shares activity
        public ShareSellingRound(GameManager parent, string id) : base(parent, id)
        {
            guiHints.ActivePanel = GuiDef.Panel.STATUS;
        }

        public void Start(IRoundFacade parentRound, Player sellingPlayer, int cashToRaise,
                PublicCompany cashNeedingCompany, bool dumpOtherCompaniesAllowed)
        {
            log.Info("Share selling round started, player="
                    + sellingPlayer.Id + " cash=" + cashToRaise);
            ReportBuffer.Add(this, LocalText.GetText("PlayerMustSellShares",
                    sellingPlayer.Id,
                    Bank.Format(this, cashToRaise)));
            this.parentRound = parentRound;
            currentPlayer = this.sellingPlayer = sellingPlayer;
            this.cashNeedingCompany = cashNeedingCompany;
            this.cashToRaise = IntegerState.Create(this, "CashToRaise", cashToRaise);

            this.dumpOtherCompaniesAllowed = dumpOtherCompaniesAllowed;
            log.Debug("Forced selling, dumpOtherCompaniesAllowed = " + dumpOtherCompaniesAllowed);
            GetRoot.PlayerManager.SetCurrentPlayer(sellingPlayer);
            if (GetSellableShares().Count == 0)
            {
                ReportBuffer.Add(this, LocalText.GetText("YouMustRaiseCashButCannot",
                        Bank.Format(this, this.cashToRaise.Value)));
                DisplayBuffer.Add(this, LocalText.GetText("YouMustRaiseCashButCannot",
                        Bank.Format(this, this.cashToRaise.Value)));
                currentPlayer.SetBankrupt();
                gameManager.RegisterBankruptcy();
            }
        }

    override public bool MayCurrentPlayerSellAnything()
        {
            return true;
        }

        
    override public bool MayCurrentPlayerBuyAnything()
        {
            return false;
        }

        override public bool SetPossibleActions()
        {

            possibleActions.Clear();

            SetSellableShares();

            foreach (PossibleAction pa in possibleActions.GetList())
            {
                log.Debug(currentPlayer.Id + " may: " + pa.ToString());
            }

            return true;
        }

    override public void SetSellableShares()
        {
            possibleActions.AddAll(sellableShares);
        }

        /**
         * Create a list of certificates that a player may sell in an emergency
         * share selling round, taking all rules taken into account.
         * 
         * FIXME: Rails 2.x Adopt the new code from StockRound
         */
        protected List<SellShares> GetSellableShares()
        {

            sellableShares = new List<SellShares>();

            int price;
            int number;
            int share, maxShareToSell;
            PortfolioModel playerPortfolio = currentPlayer.PortfolioModel;

            /*
             * First check of which companies the player owns stock, and what
             * maximum percentage he is allowed to sell.
             */
            foreach (PublicCompany company in companyManager.GetAllPublicCompanies())
            {

                // Check if shares of this company can be sold at all
                if (!MayPlayerSellShareOfCompany(company)) continue;

                share = maxShareToSell = playerPortfolio.GetShare(company);
                if (maxShareToSell == 0) continue;

                /* May not sell more than the Pool can accept */
                maxShareToSell =
                    Math.Min(maxShareToSell,
                            GameDef.GetGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)
                            - pool.GetShare(company));
                if (maxShareToSell == 0) continue;

                /*
                 * If the current Player is president, check if he can dump the
                 * presidency onto someone else
                 *
                 * Two reasons for the check:
                 * A) President not allowed to sell that company
                 * Thus keep enough shares to stay president
                 *
                 * Example here
                 * share = 60%, other player holds 40%, maxShareToSell > 30%
                 * => requires selling of president

                 * B) President allowed to sell that company
                 * In that case the president share can be sold
                 *
                 * Example here
                 * share = 60%, , president share = 20%, maxShareToSell > 40%
                 * => requires selling of president
                 */
                if (company.GetPresident() == currentPlayer)
                {
                    int presidentShare =
                        company.GetCertificates()[0].Share;
                    bool dumpPossible;
                    log.Debug("Forced selling check: company = " + company +
                            ", share = " + share + ", maxShareToSell = " + maxShareToSell);
                    if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed)
                    {
                        // case A: selling of president not allowed (either company triggered share selling or no dump of others)
                        int maxOtherShares = 0;
                        foreach (Player player in GetRoot.PlayerManager.Players)
                        {
                            if (player == currentPlayer) continue;
                            maxOtherShares = Math.Max(maxOtherShares, player.PortfolioModel.GetShare(company));
                        }
                        // limit shares to sell to difference between president and second largest ownership
                        maxShareToSell = Math.Min(maxShareToSell, share - maxOtherShares);
                        dumpPossible = false; // and no dump is possible by definition
                    }
                    else
                    {
                        // case B: potential sale of president certificate possible
                        if (share - maxShareToSell < presidentShare)
                        {
                            // dump necessary
                            dumpPossible = false;
                            foreach (Player player in GetRoot.PlayerManager.Players)
                            {
                                if (player == currentPlayer) continue;
                                // there is a player with holding exceeding the president share
                                if (player.PortfolioModel.GetShare(company) >= presidentShare)
                                {
                                    dumpPossible = true;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            dumpPossible = false; // no dump necessary
                        }
                    }
                    if (!dumpPossible)
                    {
                        // keep presidentShare at minimum
                        maxShareToSell = Math.Min(maxShareToSell, share - presidentShare);
                    }
                }

                /*
                 * Check what share units the player actually owns. In some games
                 * (e.g. 1835) companies may have different ordinary shares: 5% and
                 * 10%, or 10% and 20%. The president's share counts as a multiple
                 * of the lowest ordinary share unit type.
                 */
                // Take care for max. 4 share units per share
                int[] shareCountPerUnit = new int[5];
                foreach (PublicCertificate c in playerPortfolio.GetCertificates(company))
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

                    // May not sell more than is needed to buy the train
                    while (number > 0 && ((number - 1) * price) > cashToRaise.Value)
                    {
                        number--;
                    }

                    if (number > 0)
                    {
                        for (int i = 1; i <= number; i++)
                        {
                            sellableShares.Add(new SellShares(company, shareSize, i, price));
                        }
                    }
                }
            }
            return sellableShares;
        }

    override public bool DoSellShares(SellShares action)
        {
            PortfolioModel portfolio = currentPlayer.PortfolioModel;
            string playerName = currentPlayer.Id;
            string errMsg = null;
            string companyName = action.CompanyName;
            PublicCompany company = companyManager.GetPublicCompany(action.CompanyName);
            PublicCertificate cert = null;
            PublicCertificate presCert = null;
            List<PublicCertificate> certsToSell = new List<PublicCertificate>();
            Player dumpedPlayer = null;
            int presSharesToSell = 0;
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
                IEnumerator<PublicCertificate> it =
                        portfolio.GetCertificates(company).GetEnumerator();
                while (numberToSell > 0 && it.MoveNext())
                {
                    cert = it.Current;
                    if (cert.IsPresidentShare)
                    {
                        // Remember the president's certificate in case we need it
                        if (cert.IsPresidentShare) presCert = cert;
                        continue;
                    }
                    else if (shareUnits != cert.GetShares())
                    {
                        // Wrong number of share units
                        continue;
                    }
                    // OK, we will sell this one
                    certsToSell.Add(cert);
                    numberToSell--;
                }
                if (numberToSell == 0) presCert = null;

                if (numberToSell > 0 && presCert != null
                        && numberToSell <= presCert.GetShares())
                {
                    // Not allowed to dump the company that needs the train
                    if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed)
                    {
                        errMsg =
                            LocalText.GetText("CannotDumpTrainBuyingPresidency");
                        break;
                    }
                    // More to sell and we are President: see if we can dump it.
                    Player otherPlayer, previousPlayer;
                    previousPlayer = GetRoot.PlayerManager.CurrentPlayer;
                    for (int i = 0; i <= numberOfPlayers; i++)
                    {
                        otherPlayer = GetRoot.PlayerManager.GetNextPlayerAfter(previousPlayer);
                        if (otherPlayer.PortfolioModel.GetShare(company) >= presCert.Share)
                        {
                            // Check if he has the right kind of share
                            if (numberToSell > 1
                                || otherPlayer.PortfolioModel.OwnsCertificates(
                                            company, 1, false) >= 1)
                            {
                                // The poor sod.
                                dumpedPlayer = otherPlayer;
                                presSharesToSell = numberToSell;
                                numberToSell = 0;
                                break;
                            }
                        }
                        previousPlayer = otherPlayer;
                    }
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
            StockSpace sellPrice;
            int price;

            // Get the sell price (does not change within a turn)
            if (sellPrices.ContainsKey(company)
                    && GameOption.GetAsBoolean(this, "SeparateSalesAtSamePrice"))
            {
                price = (sellPrices.Get(company).Price);
            }
            else
            {
                sellPrice = company.GetCurrentSpace();
                price = sellPrice.Price;
                sellPrices.Put(company, sellPrice);
            }
            int cashAmount = numberSold * price * shareUnits;


            // FIXME: changeStack.linkToPreviousMoveSet();

            string cashText = Currency.FromBank(cashAmount, currentPlayer);
            ReportBuffer.Add(this, LocalText.GetText("SELL_SHARES_LOG",
                    playerName,
                    numberSold,
                    company.GetShareUnit(),
                    numberSold * company.GetShareUnit(),
                    companyName,
                    cashText));

            bool soldBefore = sellPrices.ContainsKey(company);

            AdjustSharePrice(company, numberSold, soldBefore);

            if (!company.IsClosed())
            {

                ExecuteShareTransfer(company, certsToSell,
                        dumpedPlayer, presSharesToSell);
            }

            cashToRaise.Add(-numberSold * price);

            if (cashToRaise.Value <= 0)
            {
                gameManager.FinishShareSellingRound();
            }
            else if (GetSellableShares().Count == 0)
            {
                DisplayBuffer.Add(this, LocalText.GetText("YouMustRaiseCashButCannot",
                        Bank.Format(this, cashToRaise.Value)));
                currentPlayer.SetBankrupt();
                gameManager.RegisterBankruptcy();
            }

            return true;
        }

        public int GetRemainingCashToRaise()
        {
            return cashToRaise.Value;
        }

        public PublicCompany GetCompanyNeedingCash()
        {
            return cashNeedingCompany;
        }

    override public string ToString()
        {
            return "ShareSellingRound";
        }

    }
}
