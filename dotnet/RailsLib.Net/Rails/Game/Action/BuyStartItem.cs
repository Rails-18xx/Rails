using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class BuyStartItem : StartItemAction
    {
        /* Server-provided fields */
        private int price;
        private bool selected;
        protected bool sharePriceToSet = false;
        protected string companyNeedingSharePrice = null;
        private bool setSharePriceOnly = false;
        private SortedSet<string> startSpaces = null;

        // Client-provided fields
        private int associatedSharePrice;

        new public const long serialVersionUID = 1L;

        /**
         * 
         * Rails 2.0: Added updated equals methods
         */
        public BuyStartItem(StartItem startItem, int price, bool selected,
                bool setSharePriceOnly) : base(startItem)
        {
            this.price = price;
            this.selected = selected;
            this.setSharePriceOnly = setSharePriceOnly;

            PublicCompany company;
            if ((company = startItem.NeedsPriceSetting()) != null)
            {
                sharePriceToSet = true;
                companyNeedingSharePrice = company.Id;
            }
        }

        public BuyStartItem(StartItem startItem, int price, bool selected) : this(startItem, price, selected, false)
        {

        }

        public int Price
        {
            get
            {
                return price;
            }
        }

        public bool IsSelected
        {
            get
            {
                return selected;
            }
        }

        public int AssociatedSharePrice
        {
            get
            {
                return associatedSharePrice;
            }
            set
            {
                associatedSharePrice = value;
            }
        }

        public bool HasSharePriceToSet
        {
            get
            {
                return sharePriceToSet;
            }
        }

        public bool SetSharePriceOnly
        {
            get
            {
                return setSharePriceOnly;
            }
        }

        public string CompanyToSetPriceFor
        {
            get
            {
                return companyNeedingSharePrice;
            }
        }

    override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            // super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            BuyStartItem action = (BuyStartItem)pa;
            bool options =  (price == action.price)
                            && (selected == action.selected)
                            && (setSharePriceOnly == action.setSharePriceOnly)
                            && (sharePriceToSet == action.sharePriceToSet)
                            && (companyNeedingSharePrice == action.companyNeedingSharePrice);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (associatedSharePrice == action.associatedSharePrice);
        }

        override public string ToString()
        {
            return base.ToString()
                   + RailsObjects.GetStringHelper(this).AddToString("price", price).AddToString(
                           "selected", selected).AddToString("setSharePriceOnly",
                           setSharePriceOnly).AddToString("sharePriceToSet",
                           sharePriceToSet).AddToString("companyNeedingSharePrice",
                           companyNeedingSharePrice).AddToStringOnlyActed(
                           "associatedSharePrice", associatedSharePrice).ToString();
        }

        public bool ContainsStartSpaces
        {
            get
            {
                if (startSpaces == null)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
        }

        public SortedSet<string> StartSpaces
        {
            get
            {
                return startSpaces;
            }
            set
            {
                startSpaces = value;
            }
        }
    }
}
