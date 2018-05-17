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
    public class TakeLoans : PossibleORAction
    {
        // Initial attributes
        // TODO: This is a duplication of the field in PossibleORAction
        // Is there a reason for that? (potentially that it could be used outside of ORs)
        /*transient*/
        [JsonIgnore]
        new private PublicCompany company;
        new private string companyName;
        private int maxNumber;
        private int price;

        // User-assigned attributes
        private int numberTaken = 0;

        new public const long serialVersionUID = 1L;

        /**
         *
         */
        public TakeLoans(PublicCompany company, int maxNumber, int price)
        {
            this.company = company;
            this.companyName = company.Id;
            this.maxNumber = maxNumber;
            this.price = price;
        }

        /**
         * @return Returns the minimumPrice.
         */
        public int MaxNumber
        {
            get
            {
                return maxNumber;
            }
        }

        /**
         * @return Returns the company.
         */
        override public PublicCompany Company
        {
            get
            {
                return company;
            }
        }

        public int Price
        {
            get
            {
                return price;
            }
        }

        public int NumberTaken
        {
            get
            {
                return numberTaken;
            }
            set
            {
                numberTaken = value;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            TakeLoans action = (TakeLoans)pa;
            bool options = company.Equals(action.company)
                    && (maxNumber == action.maxNumber)
                    && (price == action.price);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (numberTaken == action.numberTaken);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("company", company)
                        .AddToString("maxNumber", maxNumber)
                        .AddToString("price", price)
                        .AddToStringOnlyActed("numberTaken", numberTaken)
                        .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            company = CompanyManager.GetPublicCompany(companyName);
        }
    }
}
