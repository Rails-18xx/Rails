using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace GameLib.Net.Game.Special
{
    public class SpecialTileLay : SpecialProperty
    {
        private string locationCodes = null;
        private List<MapHex> locations = null;
        private string tileId = null;
        private Tile tile = null;
        private string name = null;
        private bool extra = false;
        private bool free = false;
        private int discount = 0;
        private bool connected = false;

        /** Tile colors that can be laid with this special property.
         * Default is same colors as is allowed in a a normal tile lay.
         * Don't use if specific tiles are specified! */
        protected string[] tileColors = null;

        /**
         * Used by Configure (via reflection) only
         */
        public SpecialTileLay(IRailsItem parent, string id) : base(parent, id)
        {
        }

        override public void ConfigureFromXML(Tag tag)
        {
            base.ConfigureFromXML(tag);

            Tag tileLayTag = tag.GetChild("SpecialTileLay");
            if (tileLayTag == null)
            {
                throw new ConfigurationException("<SpecialTileLay> tag missing");
            }

            locationCodes = tileLayTag.GetAttributeAsString("location");
            if (string.IsNullOrEmpty(locationCodes))
            {
                throw new ConfigurationException("SpecialTileLay: location missing");
            }

            tileId = tileLayTag.GetAttributeAsString("tile", null);

            string colorsString = tileLayTag.GetAttributeAsString("color");
            if (!string.IsNullOrEmpty(colorsString))
            {
                tileColors = colorsString.Split(',');
            }

            name = tileLayTag.GetAttributeAsString("name");

            extra = tileLayTag.GetAttributeAsBoolean("extra", extra);
            free = tileLayTag.GetAttributeAsBoolean("free", free);
            connected = tileLayTag.GetAttributeAsBoolean("connected", connected);
            discount = tileLayTag.GetAttributeAsInteger("discount", discount);

            if (tileId != null)
            {
                description = LocalText.GetText("LayNamedTileInfo",
                        tileId,
                        name != null ? name : "",
                                locationCodes,
                                (extra ? LocalText.GetText("extra") : LocalText.GetText("notExtra")),
                                (free ? LocalText.GetText("noCost") : discount != 0 ? LocalText.GetText("discount", discount) :
                                    LocalText.GetText("normalCost")),
                                (connected ? LocalText.GetText("connected") : LocalText.GetText("unconnected"))
                );
            }
            else
            {
                description = LocalText.GetText("LayTileInfo",
                        locationCodes,
                        // #FIXME array formatting
                        (tileColors != null ? Regex.Replace(tileColors.ToString(), "[\\[\\]]", "") : ""),
                        (extra ? LocalText.GetText("extra") : LocalText.GetText("notExtra")),
                        (free ? LocalText.GetText("noCost") : discount != 0 ? LocalText.GetText("discount", discount) :
                            LocalText.GetText("normalCost")),
                        (connected ? LocalText.GetText("connected") : LocalText.GetText("unconnected")));
            }

        }

        override public void FinishConfiguration(RailsRoot root)
        {

            TileManager tmgr = root.TileManager;
            MapManager mmgr = root.MapManager;
            MapHex hex;

            if (tileId != null)
            {
                tile = tmgr.GetTile(tileId);
            }

            locations = new List<MapHex>();
            foreach (string hexName in locationCodes.Split(','))
            {
                hex = mmgr.GetHex(hexName);
                if (hex == null)
                {
                    throw new ConfigurationException("Location " + hexName
                            + " does not exist");
                }
                locations.Add(hex);
            }

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

        public int Discount
        {
            get
            {
                return discount;
            }
        }

        public bool RequiresConnection
        {
            get
            {
                return connected;
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

        public string TileId
        {
            get
            {
                return tileId;
            }
        }

        public Tile Tile
        {
            get
            {
                return tile;
            }
        }

        public string[] TileColors
        {
            get
            {
                return tileColors;
            }
        }

        override public string ToText()
        {
            return "SpecialTileLay comp=" + originalCompany.Id
            + " hex=" + locationCodes
            + " color=" + string.Join(",", tileColors)
            + " extra=" + extra + " cost=" + free + " connected=" + connected;
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
