using GameLib.Net.Game;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class BuyBonusToken : PossibleORAction
    {
        // Initial attributes
        /*transient*/
        [JsonIgnore]
        private PrivateCompany privateCompany;
        private string privateCompanyName;
        /*transient*/
        [JsonIgnore]
        private IOwner seller = null;
        private string sellerName = null;
        /*transient*/
        [JsonIgnore]
        protected SellBonusToken specialProperty = null;
        protected int specialPropertyId;

        private string name;
        private int price;
        private int value;
        private string locationString;

        new public const long serialVersionUID = 1L;

        /**
         *
         */
        public BuyBonusToken(SellBonusToken specialProperty)
        {

            this.specialProperty = specialProperty;
            this.specialPropertyId = specialProperty.UniqueId;
            this.privateCompany = (PrivateCompany)specialProperty.OriginalCompany;
            this.privateCompanyName = privateCompany.Id;
            this.seller = specialProperty.GetSeller();
            if (seller != null) this.sellerName = seller.Id;
            this.name = specialProperty.Name;
            this.price = specialProperty.Price;
            this.value = specialProperty.Value;
            this.locationString = specialProperty.LocationNameString;
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

        public string PrivateCompanyName
        {
            get
            {
                return privateCompanyName;
            }
        }

        public IOwner Seller
        {
            get
            {
                return seller;
            }
        }

        public string SellerName
        {
            get
            {
                return sellerName;
            }
        }

        public SellBonusToken SpecialProperty
        {
            get
            {
                return specialProperty;
            }
        }

        public string Name
        {
            get
            {
                return name;
            }
        }

        public int Value
        {
            get
            {
                return value;
            }
        }

        public string LocationString
        {
            get
            {
                return locationString;
            }
        }

        public int Price
        {
            get
            {
                return price;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            BuyBonusToken action = (BuyBonusToken)pa;
            return privateCompany.Equals(action.privateCompany)
                    && (name == action.name)
                    && (price == action.price)
                    && (value == action.value)
                    && (locationString == action.locationString);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("privateCompany", privateCompany)
                        .AddToString("name", name)
                        .AddToString("price", price)
                        .AddToString("value", value)
                        .AddToString("locationString", locationString)
                        .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            privateCompany = CompanyManager.GetPrivateCompany(privateCompanyName);
            if (sellerName.Equals("Bank", StringComparison.OrdinalIgnoreCase))
            {
                // TODO: Assume that it is the pool, not the ipo
                seller = GetRoot.Bank.Pool;
            }
            else if (sellerName != null)
            {
                seller = CompanyManager.GetPublicCompany(sellerName);
            }
            if (specialPropertyId > 0)
            {
                specialProperty = (SellBonusToken)Net.Game.Special.SpecialProperty.GetByUniqueId(GetRoot, specialPropertyId);
            }
        }
    }
}
