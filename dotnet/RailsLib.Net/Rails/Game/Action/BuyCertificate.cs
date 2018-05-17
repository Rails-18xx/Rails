using GameLib.Net.Game;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Model;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class BuyCertificate : PossibleAction
    {
        // Server-side settings

        /* Some obsolete properties, which are only retained for backwards compatibility
         * (i.e. to remain able to load older saved files).
         * The certificate was in fact only used to find the below replacement
         * attributes. It was NOT actually used to select the bought certificate!
         */
        // #transient
        [JsonIgnore]
        protected PublicCertificate certificate = null;
        protected string certUniqueId = null;

        /* Replacement for the above.*/
        // #transient
        [JsonIgnore]
        protected PublicCompany company;
        protected string companyName;
        protected int sharePerCert; // Share % per buyable certificate.

        // FIXME: We have to recreate the portfolio name
        // #transient
        [JsonIgnore]
        protected PortfolioModel from;
        protected string fromName; // Old: portfolio name. New: portfolio unique name.
        protected int price;
        protected int maximumNumber;

        // Client-side settings
        protected int numberBought = 0;

        new public const long serialVersionUID = 1L;

        public BuyCertificate(PublicCompany company, int sharePerCert,
                IPortfolioOwner from,
                int price, int maximumNumber) : base(null)
        {
            //super(null); // not defined by an activity yet
            this.company = company;
            this.sharePerCert = sharePerCert;
            this.from = from.PortfolioModel;
            this.fromName = this.from.UniqueName;
            this.price = price;
            this.maximumNumber = maximumNumber;

            companyName = company.Id;
        }

        /** Buy a certificate from some owner at a given price */
        public BuyCertificate(PublicCompany company, int sharePerCert, IPortfolioOwner from, int price) :
            this(company, sharePerCert, from, price, 1)
        {

        }

        /** Required for deserialization */
        public BuyCertificate() : base(null)
        {
            //super(null); // not defined by an activity yet
        }


        public PortfolioModel FromPortfolio
        {
            get
            {
                return from;
            }
        }

        /**
         * @return Returns the maximumNumber.
         */
        public int MaximumNumber
        {
            get
            {
                return maximumNumber;
            }
        }

        /**
         * @return Returns the price.
         */
        public int Price
        {
            get
            {
                return price;
            }
        }

        public PublicCompany Company
        {
            get
            {
                return company;
            }
        }

        public string CompanyName
        {
            get
            {
                return companyName;
            }
        }

        public int SharePerCertificate
        {
            get
            {
                return sharePerCert;
            }
        }

        public int SharesPerCertificate
        {
            get
            {
                return sharePerCert / company.GetShareUnit();
            }
        }

        public int NumberBought
        {
            get
            {
                return numberBought;
            }
            set
            {
                numberBought = value;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            BuyCertificate action = (BuyCertificate)pa;
            bool options =
                    // TODO: This is commented out as the certificate is not required anymore
                    // Objects.equal(this.certificate, action.certificate)
                    this.company.Equals(action.company)
                    // In StartCompany_1880 the sharePerCert can differ #FIXME_1880
                    && (this.sharePerCert.Equals(action.sharePerCert) /*|| (this is StartCompany_1880) */)
                && this.from.Equals(action.from)
                // In StartCompany the price can differ
                && (this.price.Equals(action.price) || (this is StartCompany))
                && this.maximumNumber.Equals(action.maximumNumber);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && this.numberBought.Equals(action.numberBought);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("certificate", certificate)
                        .AddToString("company", company)
                        .AddToString("sharePerCert", sharePerCert)
                        .AddToString("fromName", fromName)
                        .AddToString("from", from)
                        .AddToString("price", price)
                        .AddToString("maximumNumber", maximumNumber)
                        .AddToStringOnlyActed("numberBought", numberBought)
                        .ToString();
        }

        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            RailsRoot root = RailsRoot.Instance;

            /* Check for aliases (old company names) */
            CompanyManager companyManager = root.CompanyManager;
            companyName = companyManager.CheckAlias(companyName);

            if (certUniqueId != null)
            {
                // Old style
                certUniqueId = companyManager.CheckAliasInCertId(certUniqueId);
                certificate = PublicCertificate.GetByUniqueId(certUniqueId);
                // TODO: This function needs a compatible replacement 
                from = GameManager.GetPortfolioByName(fromName);
                company = certificate.Company;
                companyName = company.Id;
                sharePerCert = certificate.Share;
                throw new InvalidOperationException(); // see if we can delete this
            }
            else if (companyName != null)
            {
                // New style (since Rails.1.3.1)
                company = root.CompanyManager.GetPublicCompany(companyName);
                // TODO: This function needs a compatible replacement 
                from = GameManager.GetPortfolioByUniqueName(fromName);
                // We don't need the certificate anymore.
            }
        }
    }
}
