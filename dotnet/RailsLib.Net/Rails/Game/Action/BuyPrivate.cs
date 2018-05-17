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
    public class BuyPrivate : PossibleORAction
    {
        // Initial attributes
        /*transient*/
        [JsonIgnore]
        private PrivateCompany privateCompany;
        private string privateCompanyName;
        private int minimumPrice;
        private int maximumPrice;

        // User-assigned attributes
        private int price = 0;

        new public const long serialVersionUID = 1L;

        public BuyPrivate(PrivateCompany privateCompany, int minimumPrice,
                int maximumPrice)
        {
            this.privateCompany = privateCompany;
            this.privateCompanyName = privateCompany.Id;
            this.minimumPrice = minimumPrice;
            this.maximumPrice = maximumPrice;
        }

        /**
         * @return Returns the maximumPrice.
         */
        public int MaximumPrice
        {
            get
            {
                return maximumPrice;
            }
        }

        /**
         * @return Returns the minimumPrice.
         */
        public int MinimumPrice
        {
            get
            {
                return minimumPrice;
            }
        }

        /**
         * @return Returns the privateCompany.
         */
        public PrivateCompany PrivateCompany
        {
            get
            {
                return privateCompany;
            }
        }

        public int Price
        {
            get
            {
                return price;
            }
            set
            {
                price = value;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            BuyPrivate action = (BuyPrivate)pa;
            bool options = privateCompany.Equals(action.privateCompany)
                    && (minimumPrice == action.minimumPrice)
                    && (maximumPrice == action.maximumPrice);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (price == action.price);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("privateCompany", privateCompany)
                        .AddToString("minimumPrice", minimumPrice)
                        .AddToString("maximumPrice", maximumPrice)
                        .AddToStringOnlyActed("price", price)
                        .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            privateCompany = CompanyManager.GetPrivateCompany(privateCompanyName);
        }
    }
}
