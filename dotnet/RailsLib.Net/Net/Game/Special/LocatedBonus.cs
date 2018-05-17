using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * An object of class LocatedBonus represent extra income for the owning company,
 * usually connected to certain map locations.
 * <p>LocatedBonus objects are configured as Special Properties in CompanyManager.xml.
 * @author VosE
 *
 */

namespace GameLib.Net.Game.Special
{
    public sealed class LocatedBonus : SpecialProperty
    {
        private string locationCodes = null;
        private List<MapHex> locations = null;
        private string name;
        private int value;

        /**
         * Used by Configure (via reflection) only
         */
        public LocatedBonus(IRailsItem parent, string id) : base(parent, id)
        {
        }

        override public void ConfigureFromXML(Tag tag)
        {
            base.ConfigureFromXML(tag);

            Tag bonusTag = tag.GetChild("LocatedBonus");
            if (bonusTag == null)
            {
                throw new ConfigurationException("<LocatedBonus> tag missing");
            }

            locationCodes = bonusTag.GetAttributeAsString("location");
            if (string.IsNullOrEmpty(locationCodes))
                throw new ConfigurationException("LocatedBonus: location missing");

            name = bonusTag.GetAttributeAsString("name");

            value = bonusTag.GetAttributeAsInteger("value");
            if (value <= 0)
                throw new ConfigurationException("Value invalid [" + value + "] or missing");
        }

        override public void FinishConfiguration(RailsRoot root)
        {
            locations = root.MapManager.ParseLocations(locationCodes);
        }

        override public bool IsExecutionable
        {
            get
            {
                return false;
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

        public int Value
        {
            get
            {
                return value;
            }
        }

        public string Name
        {
            get
            {
                return name;
            }
        }

        override public string ToText()
        {
            return "LocatedBonus " + name + " comp=" + originalCompany.Id + " hex="
                   + locationCodes + " value=" + value;
        }
    }
}
