using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class SellShares : PossibleAction
    {
        // Server-side settings
        private string companyName;
        /*transient*/
        [JsonIgnore]
        private PublicCompany company;
        private int shareUnit;
        private int shareUnits;
        private int share;
        private int price;
        private int number;
        /** Dump flag, indicates to which type of certificates the president's share must be exchanged.<br>
         * 0 = no dump, or dump that does not require any choice of exchange certificates;<br>
         * 1 = exchange against 1-share certificates (usually 10%);<br>
         * 2 = exchange against a 2-share certificate (as can occur in 1835);<br>
         * etc.
         */
        private int presidentExchange = 0;

        // For backwards compatibility only
#pragma warning disable 414
        private int numberSold = 0;
#pragma warning restore 414

        new public const long serialVersionUID = 1L;

        public SellShares(PublicCompany company, int shareUnits, int number, int price) :
            this(company, shareUnits, number, price, 0)
        {

        }

        public SellShares(PublicCompany company, int shareUnits, int number, int price, int presidentExchange) : base(null)
        {
            //super(null); // not defined by an activity yet
            this.company = company;
            this.shareUnits = shareUnits;
            this.price = price;
            this.number = number;
            this.presidentExchange = presidentExchange;

            companyName = company.Id;
            shareUnit = company.GetShareUnit();
            share = shareUnits * shareUnit;
        }

        /**
         * @return Returns the maximumNumber.
         */
        public int Number
        {
            get
            {
                return number;
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

        /**
         * @return Returns the companyName.
         */
        public string CompanyName
        {
            get
            {
                return companyName;
            }
        }

        public PublicCompany Company
        {
            get
            {
                return CompanyManager.GetPublicCompany(companyName);
            }
        }

        public int ShareUnits
        {
            get
            {
                return shareUnits;
            }
        }

        public int ShareUnit
        {
            get
            {
                return shareUnit;
            }
        }

        public int Share
        {
            get
            {
                return share;
            }
        }

        public int PresidentExchange
        {
            get
            {
                return presidentExchange;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            SellShares action = (SellShares)pa;

            return company.Equals(action.company)
                    && shareUnit.Equals(action.shareUnit)
                    && (shareUnits == action.shareUnits)
                    && (share == action.share)
                    && (price == action.price)
                    && (number == action.number);
            //        && Objects.equal(this.presidentExchange, action.presidentExchange)

            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("company", company)
                        .AddToString("shareUnit", shareUnit)
                        .AddToString("shareUnits", shareUnits)
                        .AddToString("share", share)
                        .AddToString("price", price)
                        .AddToString("number", number)
                        .AddToString("presidentExchange", presidentExchange)
                        .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            CompanyManager companyManager = CompanyManager;
            if (!string.IsNullOrEmpty(companyName))
            {
                companyName = companyManager.CheckAlias(companyName);
            }
            company = companyManager.GetPublicCompany(companyName);
        }
    }
}
