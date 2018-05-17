using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class StartCompany : BuyCertificate
    {
        // Server parameters
        protected int[] startPrices;

        new public const long serialVersionUID = 1L;

        public StartCompany(PublicCompany company, int[] prices, int maximumNumber) :
                        base(company, company.GetPresidentsShare().Share,
                    RailsRoot.Instance.Bank.Ipo, 0, maximumNumber)

        {
            this.startPrices = (int[])prices.Clone();
        }

        public StartCompany(PublicCompany company, int[] startPrice) : this(company, startPrice, 1)
        {

        }

        public StartCompany(PublicCompany company, int price, int maximumNumber) :
                        base(company, company.GetPresidentsShare().Share,
                    RailsRoot.Instance.Bank.Ipo, 0, maximumNumber)

        {
            this.price = price;
        }

        public StartCompany(PublicCompany company, int price) : this(company, price, 1)
        {

        }

        public int[] StartPrices
        {
            get
            {
                return startPrices;
            }
        }

        public bool MustSelectAPrice
        {
            get
            {
                return startPrices != null/* && startPrices.length > 1*/;
            }
        }

        public void SetStartPrice(int startPrice)
        {
            price = startPrice;
        }

        // FIXME: Attribute price of BuyCertificate now mutable, instead of static
        // Consider changing the class hierarchy, currently price in BuyCertificate is not checked
        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            StartCompany action = (StartCompany)pa;
            bool options = Enumerable.SequenceEqual(this.startPrices, action.startPrices);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (price == action.price);
            // TODO: price has to be checked here, as this cannot be done in BuyCertificate
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("startPrices", startPrices.ToString())
                        .ToString();
        }

    }
}
