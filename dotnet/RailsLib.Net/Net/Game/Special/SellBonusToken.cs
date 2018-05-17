using GameLib.Net.Common.Parser;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Special
{
    public class SellBonusToken : SpecialProperty
    {
        private string locationCodes = null;
        private List<MapHex> locations = null;
        private GenericState<IOwner> seller;
        private string name;
        private int price;
        private int value;
        private int maxNumberToSell;
        private IntegerState numberSold;

        /**
         * Used by Configure (via reflection) only
         */
        public SellBonusToken(IRailsItem parent, string id) : base(parent, id)
        {
            seller = GenericState<IOwner>.Create(this, "seller");
            numberSold = IntegerState.Create(this, "numberSold");
        }


        override public void ConfigureFromXML(Tag tag)
        {
            base.ConfigureFromXML(tag);

            Tag sellBonusTokenTag = tag.GetChild("SellBonusToken");
            if (sellBonusTokenTag == null)
            {
                throw new ConfigurationException("<SellBonusToken> tag missing");
            }

            locationCodes = sellBonusTokenTag.GetAttributeAsString("location");
            if (string.IsNullOrEmpty(locationCodes))
            {
                throw new ConfigurationException("SellBonusToken: location missing");
            }

            name = sellBonusTokenTag.GetAttributeAsString("name");

            value = sellBonusTokenTag.GetAttributeAsInteger("value", 0);
            if (value <= 0)
            {
                throw new ConfigurationException("Value invalid [" + value + "] or missing");
            }

            price = sellBonusTokenTag.GetAttributeAsInteger("price", 0);
            if (price <= 0)
            {
                throw new ConfigurationException("Price invalid [" + price + "] or missing");
            }

            maxNumberToSell = sellBonusTokenTag.GetAttributeAsInteger("amount", 1);

        }

        override public void FinishConfiguration(RailsRoot root)
        {

            locations = root.MapManager.ParseLocations(locationCodes);
        }


        override public void SetExercised()
        {
            numberSold.Add(1);
        }

        public void MakeResellable()
        {
            numberSold.Add(-1);
        }

        override public bool IsExercised()
        {
            return maxNumberToSell >= 0 && numberSold.Value >= maxNumberToSell;
        }

        override public bool IsExecutionable
        {
            get
            {
                return true;
            }
        }

        public List<MapHex> Locations
        {
            get
            {
                return locations;
            }
        }

        public string LocationNameString
        {
            get
            {
                return locationCodes;
            }
        }

        public string Name
        {
            get
            {
                return name;
            }
        }

        public int Price
        {
            get
            {
                return price;
            }
        }

        public int Value
        {
            get
            {
                return value;
            }
        }

        public IOwner GetSeller()
        {
            return seller.Value;
        }

        public void SetSeller(IOwner seller)
        {
            this.seller.Set(seller);
        }

        override public string ToText()
        {
            return "SellBonusToken comp=" + originalCompany.Id + " hex="
                   + locationCodes + " value=" + value + " price=" + price
                   + " max=" + maxNumberToSell + " sold=" + numberSold.Value;
        }
    }
}
