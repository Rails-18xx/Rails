using GameLib.Net.Common;
using GameLib.Rails.Game.Action;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.GameRound;

namespace GameLib.Net.Game
{
    abstract public class Round : RailsAbstractItem, IRoundFacade
    {
        protected static Logger<Round> log = new Logger<Round>();

        protected readonly PossibleActions possibleActions;
        protected readonly GuiHints guiHints;

        protected readonly GameManager gameManager;
        protected readonly CompanyManager companyManager;
        protected readonly PlayerManager playerManager;
        protected readonly Bank bank;
        protected readonly PortfolioModel ipo;
        protected readonly PortfolioModel pool;
        protected readonly PortfolioModel unavailable;
        protected readonly PortfolioModel scrapHeap;
        protected readonly StockMarket stockMarket;
        protected readonly MapManager mapManager;

        protected readonly BooleanState wasInterrupted;// = BooleanState.create(this, "wasInterrupted");


        protected Round(GameManager parent, string id) : base(parent, id)
        {
            this.gameManager = parent;
            this.possibleActions = gameManager.GetPossibleActions();

            wasInterrupted = BooleanState.Create(this, "wasInterrupted");

            companyManager = GetRoot.CompanyManager;
            playerManager = GetRoot.PlayerManager;
            bank = GetRoot.Bank;
            // TODO: It would be good to work with BankPortfolio and Owner instead of PortfolioModels
            // However this requires a lot of work inside the Round classes
            ipo = bank.Ipo.PortfolioModel;
            pool = bank.Pool.PortfolioModel;
            unavailable = bank.Unavailable.PortfolioModel;
            scrapHeap = bank.ScrapHeap.PortfolioModel;
            stockMarket = GetRoot.StockMarket;
            mapManager = GetRoot.MapManager;

            guiHints = gameManager.UIHints;
            guiHints.CurrentRoundType = GetType();
        }

        // called from GameManager
        virtual public bool Process(PossibleAction action)
        {
            return true;
        }

        /**
         * Default version, does nothing. Subclasses should override this method
         * with a real version.
         */
        virtual public bool SetPossibleActions()
        {
            return false;
        }

        /** Generic stub to resume an interrupted round.
         * Only valid if implemented in a subclass.
         *
         */
        // called from GameManager
        virtual public void Resume()
        {
            log.Error("Calling Round.resume() is invalid");
        }

        // called from GameManager and GameUIManager
        virtual public string RoundName
        {
            get
            {
                return this.GetType().Name;
            }
        }

        /** A stub for processing actions triggered by a phase change.
         * Must be overridden by subclasses that need to process such actions.
         * @param name (required) The name of the action to be executed
         * @param value (optional) The value of the action to be executed, if applicable
         */
        // can this be moved to GameManager, not yet as there are internal dependencies
        // called from GameManager
        virtual public void ProcessPhaseAction(string name, string value)
        {

        }

        /** Set the operating companies in their current acting order */
        // What is the reason of that to have that here? => move to OR?
        // called only internally
        public List<PublicCompany> SetOperatingCompanies()
        {
            return SetOperatingCompanies(null, null);
        }

        // What is the reason of that to have that here => move to OR?
        // this is still required for 18EU StockRound as due to the merger there are companies that have to discard trains
        // called only internally
        public List<PublicCompany> SetOperatingCompanies(List<PublicCompany> oldOperatingCompanies,
                PublicCompany lastOperatingCompany)
        {
            SortedDictionary<int, PublicCompany> operatingCompanies =
                new SortedDictionary<int, PublicCompany>();
            List<PublicCompany> newOperatingCompanies;
            StockSpace space;
            int key;
            int minorNo = 0;
            bool reorder = gameManager.IsDynamicOperatingOrder
            && oldOperatingCompanies != null && lastOperatingCompany != null;

            int lastOperatingCompanyndex;
            if (reorder)
            {
                newOperatingCompanies = oldOperatingCompanies;
                lastOperatingCompanyndex = oldOperatingCompanies.IndexOf(lastOperatingCompany);
            }
            else
            {
                newOperatingCompanies = companyManager.GetAllPublicCompanies();
                lastOperatingCompanyndex = -1;
            }

            foreach (PublicCompany company in newOperatingCompanies)
            {
                if (!reorder && !CanCompanyOperateThisRound(company)) continue;

                if (reorder
                        && oldOperatingCompanies.IndexOf(company) <= lastOperatingCompanyndex)
                {
                    // Companies that have operated this round get lowest keys
                    key = oldOperatingCompanies.IndexOf(company);
                }
                else if (company.HasStockPrice)
                {
                    // Key must put companies in reverse operating order, because sort
                    // is ascending.
                    space = company.GetCurrentSpace();
                    key = 1000000 * (999 - space.Price)
                    + 10000 * (99 - space.Column)
                    + 100 * (space.Row + 1)
                    + space.GetStackPosition(company);
                }
                else
                {
                    key = 50 + ++minorNo;
                }
                operatingCompanies[key] = company;
            }

            return new List<PublicCompany>(operatingCompanies.Values);
        }

        /** Can a public company operate? (Default version) */
        // What is the reason of that to have that here? => move to OR?
        // is called by setOperatingCompanies above
        // called only internally
        protected bool CanCompanyOperateThisRound(PublicCompany company)
        {
            return company.HasFloated() && !company.IsClosed();
        }

        /**
         * Check if a company must be floated, and if so, do it. <p>This method is
         * included here because it is used in various types of Round.
         *
         * @param company
         */
        // What is the reason of that to have that here? => best to move it to PublicCompany in the long-run
        // is called by StartRound as well
        // called only internally
        protected void CheckFlotation(PublicCompany company)
        {

            if (!company.HasStarted() || company.HasFloated()) return;

            if (company.GetSoldPercentage() >= company.FloatPercentage)
            {
                // Company floats
                FloatCompany(company);
            }
        }

        /**
         * Float a company, including a default implementation of moving cash and
         * shares as a result of flotation. <p>Full capitalisation is implemented
         * as in 1830. Partial capitalisation is implemented as in 1851. Other ways
         * to process the consequences of company flotation must be handled in
         * game-specific subclasses.
         */
        // What is the reason of that to have that here? => move to SR?
        // called by checkFloatation above
        // move it to PublicCompany in the long-run
        // called only internally
        protected void FloatCompany(PublicCompany company)
        {

            // Move cash and shares where required
            int soldPercentage = company.GetSoldPercentage();
            int cash = 0;
            int capitalizationMode = company.Capitalization;
            if (company.HasStockPrice)
            {
                int capFactor = 0;
                int shareUnit = company.GetShareUnit();
                if (capitalizationMode == PublicCompany.CAPITALIZE_FULL)
                {
                    // Full capitalization as in 1830
                    capFactor = 100 / shareUnit;
                }
                else if (capitalizationMode == PublicCompany.CAPITALIZE_INCREMENTAL)
                {
                    // Incremental capitalization as in 1851
                    capFactor = soldPercentage / shareUnit;
                }
                else if (capitalizationMode == PublicCompany.CAPITALIZE_WHEN_BOUGHT)
                {
                    // Cash goes directly to treasury at each buy (as in 1856 before phase 6)
                    capFactor = 0;
                }
                int price = company.GetIPOPrice();
                cash = capFactor * price;
            }
            else
            {
                cash = company.FixedPrice;
            }

            // Subtract initial token cost (e.g. 1851, 18EU)
            cash -= company.BaseTokensBuyCost;

            company.SetFloated(); // After calculating cash (for 1851: price goes
                                  // up)

            if (cash > 0)
            {
                string cashText = Currency.FromBank(cash, company);
                ReportBuffer.Add(this, LocalText.GetText("FloatsWithCash", company.Id, cashText));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("Floats", company.Id));
            }

            if (capitalizationMode == PublicCompany.CAPITALIZE_INCREMENTAL
                    && company.CanHoldOwnShares)
            {
                // move all shares from ipo to the company portfolio
                // FIXME: Does this work correctly?
                Portfolio.MoveAll(ipo.GetCertificates(company), company);
            }
        }

        // Could be moved somewhere else (RoundUtils?)
        // called only internally
        virtual protected void FinishRound()
        {
            // Report financials
            ReportBuffer.Add(this, "");
            foreach (PublicCompany c in companyManager.GetAllPublicCompanies())
            {
                if (c.HasFloated() && !c.IsClosed())
                {
                    ReportBuffer.Add(this, LocalText.GetText("Has", c.Id,
                            Bank.Format(this, c.Cash)));
                }
            }
            foreach (Player p in playerManager.Players)
            {
                ReportBuffer.Add(this, LocalText.GetText("Has", p.Id,
                        Bank.Format(this, p.CashValue)));
            }
            // Inform GameManager
            gameManager.NextRound(this);
        }

        // called only from 1835 Operating Round?
        public bool WasInterrupted()
        {
            return wasInterrupted.Value;
        }

    }
}
