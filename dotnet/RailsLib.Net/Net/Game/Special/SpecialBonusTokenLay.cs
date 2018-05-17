using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Special
{
    public class SpecialBonusTokenLay : SpecialProperty
    {
        private string locationCodes = null;
        private List<MapHex> locations = null;

        private BonusToken token;

        /**
         * Used by Configure (via reflection) only
         */
        public SpecialBonusTokenLay(IRailsItem parent, string id) : base(parent, id)
        {
        }


        override public void ConfigureFromXML(Tag tag)
        {
            base.ConfigureFromXML(tag);

            Tag tokenLayTag = tag.GetChild("SpecialBonusTokenLay");
            if (tokenLayTag == null)
            {
                throw new ConfigurationException("<SpecialBonusTokenLay> tag missing");
            }

            locationCodes = tokenLayTag.GetAttributeAsString("location");
            if (string.IsNullOrEmpty(locationCodes))
            {
                throw new ConfigurationException("SpecialBonusTokenLay: location missing");
            }
            // #FIXME Cast is to unrelated types
            BonusToken bToken = BonusToken.Create((IRailsOwner)Parent);
            token = bToken;
            bToken.ConfigureFromXML(tokenLayTag);

            string tokenName = bToken.Id;
            int tokenValue = bToken.Value;

            description = LocalText.GetText("LayBonusTokenInfo",
                        tokenName,
                        Bank.Format(this, tokenValue),
                        locationCodes);
        }

        override public void FinishConfiguration(RailsRoot root)
        {
            locations = root.MapManager.ParseLocations(locationCodes);

            if (token is BonusToken)
            {
                ((BonusToken)token).PrepareForRemoval(root.PhaseManager);
            }
        }

        public BonusToken Token
        {
            get
            {
                return token;
            }
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

        public string LocationCodeString
        {
            get
            {
                return locationCodes;
            }
        }

        override public string ToText()
        {
            return description;
        }

        override public string ToMenu()
        {
            return description;
        }

        override public string GetInfo()
        {
            return description;
        }
    }
}
