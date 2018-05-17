using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Special
{
    public class SpecialBaseTokenLay : SpecialProperty
    {
        private string locationCodes = null;
        private List<MapHex> locations = null;

        private bool extra = false;
        private bool free = false;
        private bool connected = false;
        private bool requiresTile = false;
        private bool requiresNoTile = false;

        /**
         * Used by Configure (via reflection) only
         */
        public SpecialBaseTokenLay(IRailsItem parent, string id) : base(parent, id)
        {

        }

        override public void ConfigureFromXML(Tag tag)
        {

            base.ConfigureFromXML(tag);

            Tag tokenLayTag = tag.GetChild("SpecialBaseTokenLay");
            if (tokenLayTag == null)
            {
                throw new ConfigurationException("<SpecialBaseTokenLay> tag missing");
            }

            locationCodes = tokenLayTag.GetAttributeAsString("location");
            if (string.IsNullOrEmpty(locationCodes))
            {
                throw new ConfigurationException(
                        "SpecialBaseTokenLay: location missing");
            }

            extra = tokenLayTag.GetAttributeAsBoolean("extra", extra);
            free = tokenLayTag.GetAttributeAsBoolean("free", free);
            connected = tokenLayTag.GetAttributeAsBoolean("connected", connected);
            requiresTile = tokenLayTag.GetAttributeAsBoolean("requiresTile", requiresTile);
            requiresNoTile = tokenLayTag.GetAttributeAsBoolean("requiresNoTile", requiresNoTile);

            description = LocalText.GetText("LayBaseTokenInfo",
                    locationCodes,
                    (extra ? LocalText.GetText("extra") : LocalText.GetText("notExtra")),
                    (free ? LocalText.GetText("noCost") : LocalText.GetText("normalCost")));
        }

        override public void FinishConfiguration(RailsRoot root)
        {
            locations = root.MapManager.ParseLocations(locationCodes);
        }

        override public bool IsExecutionable
        {
            get
            {
                return true;
            }
        }

        public bool IsExtra
        {
            get
            {
                return extra;
            }
        }

        public bool IsFree
        {
            get
            {
                return free;
            }
        }

        public bool RequiresTile
        {
            get
            {
                return requiresTile;
            }
        }

        public bool RequiresNoTile
        {
            get
            {
                return requiresNoTile;
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
