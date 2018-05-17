using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Game
{
    public class Player : RailsAbstractItem, IRailsMoneyOwner, IPortfolioOwner, IChangeActionOwner, IComparable<Player>
    {
        // FIXME: Rails 2.0 Do we need the index number?

        // dynamic data (states and models)
        private IntegerState index;
        private PortfolioModel portfolio;
        private CertificateCountModel certCount;

        private PurseMoneyModel cash;
        private CalculatedMoneyModel freeCash;
        private CountingMoneyModel blockedCash;
        private CalculatedMoneyModel worth;
        private CountingMoneyModel lastORWorthIncrease;

        private BooleanState bankrupt;
        private IntegerState worthAtORStart;
        private Dictionary<PublicCompany, SoldThisRoundModel> soldThisRound = new Dictionary<PublicCompany, SoldThisRoundModel>();
        private PlayerNameModel playerNameModel;

        class FreeCashMethod : CalculationMethodBase
        {
            Player player;
            public FreeCashMethod(Player p)
            {
                player = p;
            }

            override public int Calculate()
            {
                return player.cash.Value - player.blockedCash.Value;
            }

            override public bool Initialized
            {
                get
                {
                    return player.cash.Initialized && player.blockedCash.Initialized;
                }
            }
        }

        class WorthMethod : CalculationMethodBase
        {
            Player player;
            public WorthMethod(Player p)
            {
                player = p;
            }

            override public int Calculate()
            {
                // if player is bankrupt cash is not counted
                // as this was generated during forced selling
                int worth;
                if (player.bankrupt.Value)
                {
                    worth = 0;
                }
                else
                {
                    worth = player.cash.Value;
                }

                foreach (PublicCertificate cert in player.PortfolioModel.Certificates)
                {
                    worth += cert.Company.GetGameEndPrice() * cert.GetShares();
                }
                foreach (PrivateCompany priv in player.PortfolioModel.PrivateCompanies)
                {
                    worth += priv.BasePrice;
                }
                return worth;
            }

            override public bool Initialized
            {
                get
                {
                    return player.cash.Initialized;
                }
            }
        }

        private Player(PlayerManager parent, string id, int index) : base(parent, id)
        {
            this.index = IntegerState.Create(this, "index");
            portfolio = PortfolioModel.Create(this);
            certCount = CertificateCountModel.Create(portfolio);
            cash = PurseMoneyModel.Create(this, "cash", false);
            blockedCash = CountingMoneyModel.Create(this, "blockedCash", false);
            lastORWorthIncrease = CountingMoneyModel.Create(this, "lastORIncome", false);
            bankrupt = BooleanState.Create(this, "isBankrupt");
            worthAtORStart = IntegerState.Create(this, "worthAtORStart");
            playerNameModel = PlayerNameModel.Create(this);

            this.index.Set(index);

            blockedCash.SetSuppressZero(true);
            lastORWorthIncrease.SetDisplayNegative(true);

            freeCash = CalculatedMoneyModel.Create(this, "freeCash", new FreeCashMethod(this));
            cash.AddModel(freeCash);
            blockedCash.AddModel(freeCash);

            worth = CalculatedMoneyModel.Create(this, "worth", new WorthMethod(this));
            portfolio.AddModel(worth);
            cash.AddModel(worth);
        }

        public static Player Create(PlayerManager parent, string id, int index)
        {
            return new Player(parent, id, index);
        }

        new public PlayerManager Parent
        {
            get
            {
                return (PlayerManager)base.Parent;
            }
        }

        public void FinishConfiguration(RailsRoot root)
        {
            portfolio.FinishConfiguration();

            // create soldThisRound states
            foreach (PublicCompany company in root.CompanyManager.GetAllPublicCompanies())
            {
                soldThisRound[company] = SoldThisRoundModel.Create(this, company);
            }
            // make worth aware of market model
            root.StockMarket.MarketModel.AddModel(worth);
        }

        public string GetNameAndPriority()
        {
            return Id + (Parent.PriorityPlayer == this ? " PD" : "");
        }

        public PlayerNameModel PlayerNameModel
        {
            get
            {
                return playerNameModel;
            }
        }

        /**
         * Get the player's total worth.
         *
         * @return Total worth
         */
        public int GetWorth()
        {
            return worth.Value;
        }

        public CalculatedMoneyModel WorthModel
        {
            get
            {
                return worth;
            }
        }

        public MoneyModel LastORWorthIncrease
        {
            get
            {
                return lastORWorthIncrease;
            }
        }

        public void SetWorthAtORStart()
        {
            worthAtORStart.Set(GetWorth());
        }

        public void SetLastORWorthIncrease()
        {
            lastORWorthIncrease.Set(GetWorth() - worthAtORStart.Value);
        }

        public int CashValue
        {
            get
            {
                return cash.Value;
            }
        }

        public void UpdateWorth()
        {
            // FIXME: Is this method still required
            // worth.update();
        }

        public CertificateCountModel CertCountModel
        {
            get
            {
                return certCount;
            }
        }

        public CalculatedMoneyModel FreeCashModel
        {
            get
            {
                return freeCash;
            }
        }

        public MoneyModel BlockedCashModel
        {
            get
            {
                return blockedCash;
            }
        }

        /**
         * Block cash allocated by a bid.
         *
         * @param amount Amount of cash to be blocked.
         * @return false if the amount was not available.
         */
        public bool BlockCash(int amount)
        {
            if (amount > cash.Value - blockedCash.Value)
            {
                return false;
            }
            else
            {
                blockedCash.Change(amount);
                // TODO: is this still required?
                // freeCash.update();
                return true;
            }
        }

        /**
         * Unblock cash.
         *
         * @param amount Amount to be unblocked.
         * @return false if the given amount was not blocked.
         */
        public bool UnblockCash(int amount)
        {
            if (amount > blockedCash.Value)
            {
                return false;
            }
            else
            {
                blockedCash.Change(-amount);
                // TODO: is this still required?
                // freeCash.update();
                return true;
            }
        }

        /**
         * @return the unblocked cash (available for bidding)
         */
        public int FreeCash
        {
            get
            {
                return freeCash.Value;
            }
        }

        public int BlockedCash
        {
            get
            {
                return blockedCash.Value;
            }
        }

        public int Index
        {
            get
            {
                return index.Value;
            }
        }

        public void SetIndex(int index)
        {
            this.index.Set(index);
        }

        public void SetBankrupt()
        {
            bankrupt.Set(true);
        }

        public bool IsBankrupt
        {
            get
            {
                return bankrupt.Value;
            }
        }

        public void ResetSoldThisRound()
        {
            foreach (SoldThisRoundModel state in soldThisRound.Values)
            {
                state.Set(false);
            }
        }

        public bool HasSoldThisRound(PublicCompany company)
        {
            return soldThisRound[company].Value;
        }

        public void SetSoldThisRound(PublicCompany company)
        {
            soldThisRound[company].Set(true);
        }

        public SoldThisRoundModel GetSoldThisRoundModel(PublicCompany company)
        {
            return soldThisRound[company];
        }

        // MoneyOwner interface
        public Purse Purse
        {
            get
            {
                return cash.Purse;
            }
        }

        public int Cash
        {
            get
            {
                return cash.Purse.Value();
            }
        }

        // Owner interface
        public PortfolioModel PortfolioModel
        {
            get
            {
                return portfolio;
            }
        }

        /**
         * Compare Players by their total worth, in descending order. This method
         * implements the Comparable interface.
         * second level decision is by name
         */
        public int CompareTo(Player p)
        {
            // first by wealth
            int result = -GetWorth().CompareTo(p.GetWorth());
            // then by name
            if (result == 0)
                result = Id.CompareTo(p.Id);
            return result;
        }

        public PurseMoneyModel Wallet
        {
            get
            {
                return cash;
            }
        }
    }
}
