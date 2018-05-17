using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace GameLib.Net.Game.Financial
{
    public class Bank : RailsManager, ICurrencyOwner, IRailsMoneyOwner, IConfigurable, ITriggerable
    {
        public const string ID = "BANK";

        /** Specific portfolio names */
        public const string IPO_NAME = "IPO";
        public const string POOL_NAME = "Pool";
        public const string SCRAPHEAP_NAME = "ScrapHeap";
        public const string UNAVAILABLE_NAME = "Unavailable";

        /** Default limit of shares in the bank pool */
        private const int DEFAULT_BANK_AMOUNT = 12000;
        private const string DEFAULT_MONEY_FORMAT = "$@";

        /** The Bank currency */
        private Currency currency;

        /** The Bank's amount of cash */
        private PurseMoneyModel cash;

        /** The IPO */
        private BankPortfolio ipo;
        /** The Bank Pool */
        private BankPortfolio pool;
        /** Collection of items that will (may) become available in the future */
        private BankPortfolio unavailable;
        /** Collection of items that have been discarded (but are kept to allow Undo) */
        private BankPortfolio scrapHeap;

        /** Is the bank broken */
        private BooleanState broken;

        //// Instance initializer to create a BankBroken model
        //    {
        //        new Triggerable() {
        //        {// instance initializer
        //            cash.addTrigger(this);
        //    }
        //        public void triggered(Observable obs, Change change)
        //    {
        //        if (cash.value() <= 0 && !broken.value())
        //        {
        //            broken.set(true);
        //            cash.setText(LocalText.getText("BROKEN"));
        //            getRoot().getGameManager().registerBrokenBank();
        //        }
        //    }
        //};
        //}

        /**
         * Used by Configure (via reflection) only
         */
        public Bank(RailsRoot parent, string id) : base(parent, ID)
        {
            // FIXME: This is a workaround to keep id to large caps
            //super(parent, ID);
            currency = Currency.Create(this, "currency");
            cash = PurseMoneyModel.Create(this, "cash", false, currency);
            ipo = BankPortfolio.Create(this, IPO_NAME);
            pool = BankPortfolio.Create(this, POOL_NAME);
            unavailable = BankPortfolio.Create(this, UNAVAILABLE_NAME);
            scrapHeap = BankPortfolio.Create(this, SCRAPHEAP_NAME);
            broken = BooleanState.Create(this, "broken");

            cash.AddTrigger(this);
        }

        public void Triggered(Observable obs, Change change)
        {
            if (cash.Value <= 0 && !broken.Value)
            {
                broken.Set(true);
                cash.SetText(LocalText.GetText("BROKEN"));
                GetRoot.GameManager.RegisterBrokenBank();
            }
        }
        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {

            // Parse the Bank element

            /* First set the money format */
            string moneyFormat = null;
            string configFormat = Config.Get("money_format");
            if (!string.IsNullOrEmpty(configFormat) && Regex.IsMatch(configFormat, ".*@.*"))
            {
                moneyFormat = configFormat;
            }
            else
            {
                /*
                 * Only use the rails.game-specific format if it has not been
                 * overridden in the configuration file (see if statement above)
                 */
                Tag moneyTag = tag.GetChild("Money");
                if (moneyTag != null)
                {
                    moneyFormat = moneyTag.GetAttributeAsString("format");
                }
            }
            /* Make sure that we have a format */
            if (string.IsNullOrEmpty(moneyFormat)) moneyFormat = DEFAULT_MONEY_FORMAT;
            currency.SetFormat(moneyFormat);

            Tag bankTag = tag.GetChild("Bank");
            if (bankTag != null)
            {
                // initialize bank from unknown owner
                UnknownOwner unknown = GetRoot.StateManager.WalletManager.UnknownOwner;
                int amount = bankTag.GetAttributeAsInteger("amount", DEFAULT_BANK_AMOUNT);
                currency.Move(unknown, amount, this);
            }

        }

        public void FinishConfiguration(RailsRoot root)
        {

            ReportBuffer.Add(this, LocalText.GetText("BankSizeIs", currency.Format(cash.Value)));

            // finish configuration of BankPortfolios
            ipo.FinishConfiguration();
            pool.FinishConfiguration();
            unavailable.FinishConfiguration();
            scrapHeap.FinishConfiguration();

            // Add privates
            List<PrivateCompany> privates = root.CompanyManager.GetAllPrivateCompanies();
            foreach (PrivateCompany priv in privates)
            {
                ipo.PortfolioModel.AddPrivateCompany(priv);
            }

            // Add public companies
            List<PublicCompany> companies = root.CompanyManager.GetAllPublicCompanies();
            foreach (PublicCompany comp in companies)
            {
                foreach (PublicCertificate cert in comp.GetCertificates())
                {
                    if (cert.IsInitiallyAvailable)
                    {
                        cert.MoveTo(ipo);
                    }
                    else
                    {
                        cert.MoveTo(unavailable);
                    }
                }
            }
        }

        /**
         * @return IPO Portfolio
         */
        public BankPortfolio Ipo
        {
            get
            {
                return ipo;
            }
        }

        public BankPortfolio ScrapHeap
        {
            get
            {
                return scrapHeap;
            }
        }

        /**
         * @return Portfolio of stock in Bank Pool
         */
        public BankPortfolio Pool
        {
            get
            {
                return pool;
            }
        }

        /**
         * @return Portfolio of unavailable shares
         */
        public BankPortfolio Unavailable
        {
            get
            {
                return unavailable;
            }
        }

        override public string ToText()
        {
            return LocalText.GetText("BANK");
        }

        // CurrencyOwner interface
        public Currency Currency
        {
            get
            {
                return currency;
            }
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

        public static string Format(IRailsItem item, IEnumerable<int> amount)
        {
            Currency currency = item.GetRoot.Bank.Currency;
            return currency.Format(amount);
        }

        public static string Format(IRailsItem item, int amount)
        {
            Currency currency = item.GetRoot.Bank.Currency;
            return currency.Format(amount);
        }

        public static Bank Get(IRailsItem item)
        {
            return item.GetRoot.Bank;
        }

        public static BankPortfolio GetIpo(IRailsItem item)
        {
            return Get(item).ipo;
        }

        public static BankPortfolio GetPool(IRailsItem item)
        {
            return Get(item).pool;
        }

        public static BankPortfolio GetScrapHeap(IRailsItem item)
        {
            return Get(item).scrapHeap;
        }

        public static BankPortfolio GetUnavailable(IRailsItem item)
        {
            return Get(item).unavailable;
        }
    }
}
