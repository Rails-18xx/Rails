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
    public class RepayLoans : PossibleAction
    {
        // Initial attributes
        /*transient*/
        [JsonIgnore]
        private PublicCompany company;
        private string companyName;
        private int minNumber;
        private int maxNumber;
        private int price;

        // User-assigned attributes
        private int numberRepaid = 0;

        new public const long serialVersionUID = 1L;

        public RepayLoans(PublicCompany company, int minNumber, int maxNumber, int price) : base(null)
        {
            this.company = company;
            this.companyName = company.Id;
            this.minNumber = minNumber;
            this.maxNumber = maxNumber;
            this.price = price;
        }

        public int MinNumber
        {
            get
            {
                return minNumber;
            }
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
        public PublicCompany Company
        {
            get
            {
                return company;
            }
        }

        /**
         * @return Returns the company.
         */
        public string CompanyName
        {
            get
            {
                return companyName;
            }
        }

        public int Price
        {
            get
            {
                return price;
            }
        }

        public void SetNumberTaken(int numberRepaid)
        {
            this.numberRepaid = numberRepaid;
        }

        public int NumberRepaid
        {
            get
            {
                return numberRepaid;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            RepayLoans action = (RepayLoans)pa;
            bool options = company.Equals(action.company)
                    && (minNumber == action.minNumber)
                    && (maxNumber == action.maxNumber)
                    && (price == action.price);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (numberRepaid == action.numberRepaid);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("company", company)
                        .AddToString("minNumber", minNumber)
                        .AddToString("maxNumber", maxNumber)
                        .AddToString("price", price)
                        .AddToStringOnlyActed("numberRepaid", numberRepaid)
                        .ToString()
            ;
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            company = CompanyManager.GetPublicCompany(companyName);
        }

    }
}
