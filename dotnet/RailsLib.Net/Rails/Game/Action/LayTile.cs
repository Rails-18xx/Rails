using GameLib.Net.Game;
using GameLib.Net.Game.Special;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;

/**
 * Rails 2.0: Updated equals and toString methods (however see TODO below)
*/

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class LayTile : PossibleORAction, IComparable<LayTile>
    {
        /* LayTile types */
        public const int GENERIC = 0; // Stop-gap only
        public const int LOCATION_SPECIFIC = 1; // Valid hex and allowed tiles
        public const int SPECIAL_PROPERTY = 2; // Directed by a special property
        public const int CORRECTION = 99; // Correction tile lays

        protected int type = 0;

        /*--- Preconditions ---*/

        /** Where to lay a tile (null means anywhere) */
        /*transient*/
        [JsonIgnore]
        private List<MapHex> locations = null;
        private string locationNames;

        /** Highest tile color (empty means unspecified) */
        private Dictionary<string, int> tileColors = null;

        /** Allowed tiles on a specific location (empty means unspecified) */
        /*transient*/
        [JsonIgnore]
        private List<Tile> tiles = null;
        //private int[] tileIds;
        private string[] sTileIds;

        /**
         * Special property that will be fulfilled by this tile lay. If null, this
         * is a normal tile lay.
         */
        /*transient*/
        [JsonIgnore]
        private SpecialTileLay specialProperty = null;
        private int specialPropertyId;

        /**
         * Need base tokens be relaid?
         */
        private bool relayBaseTokens = false;

        /*--- Postconditions ---*/

        /** The tile actually laid */
        /*transient*/
        [JsonIgnore]
        private Tile laidTile = null;
#pragma warning disable 649
        private int laidTileId;
#pragma warning restore 649
        private string sLaidTileId;

        /** The map hex on which the tile is laid */
        /*transient*/
        [JsonIgnore]
        private MapHex chosenHex = null;
        private string chosenHexName;

        /** The tile orientation */
        private int orientation;

        /** Any manually assigned base token positions */
        private Dictionary<string, int> relaidBaseTokens = null;
        private string relaidBaseTokensString = null;

        new public const long serialVersionUID = 1L;

        public LayTile(int type)
        {
            this.type = type;
        }

        public LayTile(IDictionary<string, int> tileColors)
        {
            type = GENERIC;
            SetTileColors(tileColors);
            // NOTE: tileColours is currently only used for Help purposes.
        }

        public LayTile(SpecialTileLay specialProperty)
        {
            type = SPECIAL_PROPERTY;
            this.locations = specialProperty.Locations;
            if (locations != null) BuildLocationNameString();
            this.specialProperty = specialProperty;
            if (specialProperty != null)
            {
                this.specialPropertyId = specialProperty.UniqueId;
                Tile tile = specialProperty.Tile;
                if (tile != null)
                {
                    tiles = new List<Tile>();
                    tiles.Add(tile);
                }
            }
        }

        /**
         * @return Returns the chosenHex.
         */
        public MapHex ChosenHex
        {
            get
            {
                return chosenHex;
            }
            set
            {
                this.chosenHex = value;
                this.chosenHexName = chosenHex.Id;
            }
        }

        public int Orientation
        {
            get
            {
                return orientation;
            }
            set
            {
                orientation = value;
            }
        }

        /**
         * @return Returns the laidTile.
         */
        public Tile LaidTile
        {
            get
            {
                return laidTile;
            }
            set
            {
                this.laidTile = value;
                this.sLaidTileId = laidTile.Id;
            }
        }

        /**
         * @return Returns the specialProperty.
         */
        public SpecialTileLay SpecialProperty
        {
            get
            {
                return specialProperty;
            }
            set
            {
                specialProperty = value;
                // TODO this.specialPropertyName = specialProperty.getUniqueId();
            }
        }

        /**
         * @return Returns the tiles.
         */
        public List<Tile> GetTiles()
        {
            return tiles;
        }

        /**
         * @param tiles The tiles to set.
         */
        public void SetTiles(List<Tile> tiles)
        {
            this.tiles = tiles;
            this.sTileIds = new string[tiles.Count];
            for (int i = 0; i < tiles.Count; i++)
            {
                sTileIds[i] = tiles[i].Id;
            }
        }

        public List<MapHex> Locations
        {
            get
            {
                return locations;
            }
            set
            {
                this.locations = value;
                if (locations != null) BuildLocationNameString();
            }
        }

        public int LayTileType
        {
            get
            {
                return type;
            }
        }

        /**
         * @return Returns the tileColours.
         */
        public Dictionary<string, int> GetTileColors()
        {
            return tileColors;
        }

        public bool IsTileColorAllowed(string tileColor)
        {
            return tileColors != null
            && tileColors.ContainsKey(tileColor)
            && tileColors[tileColor] > 0;
        }

        public void SetTileColors(IDictionary<string, int> map)
        {
            tileColors = new Dictionary<string, int>();
            // Check the map. Sometimes 0 values creep in, and these can't easily
            // be intercepted in the UI code (see comment at previous method).
            // TODO This is a dirty fix, but the quickest one too.
            if (map != null)
            {
                foreach (string colorName in map.Keys)
                {
                    if (map[colorName] > 0) tileColors[colorName] = map[colorName];
                }
            }
        }


        public bool IsRelayBaseTokens
        {
            get
            {
                return relayBaseTokens;
            }
            set
            {
                relayBaseTokens = value;
            }
        }

        public void AddRelayBaseToken(string companyName, int cityNumber)
        {
            if (relaidBaseTokens == null)
            {
                relaidBaseTokens = new Dictionary<string, int>();
            }
            relaidBaseTokens[companyName] = cityNumber;
            relaidBaseTokensString = Util.AppendWithDelimiter(relaidBaseTokensString,
                    Util.AppendWithDelimiter(companyName, cityNumber.ToString(), ":"), ",");
        }

        public Dictionary<string, int> RelaidBaseTokens
        {
            get
            {
                return relaidBaseTokens;
            }
        }

        public int GetPotentialCost(MapHex hex)
        {
            if (specialProperty != null)
            {
                if (specialProperty.IsFree)
                {
                    return 0;
                }
                else
                {
                    return Math.Max(0, hex.GetTileCost() - specialProperty.Discount);
                }
            }
            return hex.GetTileCost();
        }


        // TODO: Check for and add the missing attributes
        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            LayTile action = (LayTile)pa;
            bool options = (locations == null || (locations.Count == 0) || locations.Contains(action.chosenHex))
                    && (tiles == null || (tiles.Count == 0) || tiles.SequenceEqual(action.tiles) || tiles.Contains(action.LaidTile))
                    //              && Objects.equal(this.type, action.type) // type is not always stored 
                    && specialProperty.Equals(action.specialProperty);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options
                && laidTile.Equals(action.laidTile)
                && chosenHex.Equals(action.chosenHex)
                && (orientation == action.orientation)
                //&& Objects.equal(this.relaidBaseTokens, action.relaidBaseTokens)
                && (relaidBaseTokens.Count == action.relaidBaseTokens.Count &&
                    !relaidBaseTokens.Except(action.relaidBaseTokens).Any());

        }

        // TODO: Check for and add the missing attributes
        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("locations", locations)
                        .AddToString("tiles", tiles)
                        .AddToString("type", type)
                        .AddToString("specialProperty", specialProperty)
                        .AddToStringOnlyActed("laidTile", laidTile)
                        .AddToStringOnlyActed("chosenHex", chosenHex)
                        .AddToStringOnlyActed("orientation", orientation)
                        .AddToStringOnlyActed("relaidBaseTokens", relaidBaseTokens)
                    .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            MapManager mmgr = GetRoot.MapManager;
            TileManager tmgr = GetRoot.TileManager;
            locations = new List<MapHex>();
            if (!string.IsNullOrEmpty(locationNames))
            {
                foreach (string hexName in locationNames.Split(','))
                {
                    locations.Add(mmgr.GetHex(hexName));
                }
            }

            // FIXME: Rewrite this with Rails1.x version flag
            //if (tileIds != null && tileIds.Length > 0)
            //{
            //    tiles = new List<Tile>();
            //    foreach (int tileNb in tileIds)
            //    {
            //        tiles.Add(tmgr.GetTile(tileNb.ToString()));
            //    }
            //}

            if (sTileIds != null && sTileIds.Length > 0)
            {
                tiles = new List<Tile>();
                foreach (string tileId in sTileIds)
                {
                    tiles.Add(tmgr.GetTile(tileId));
                }
            }

            if (specialPropertyId > 0)
            {
                specialProperty =
                    (SpecialTileLay)Net.Game.Special.SpecialProperty.GetByUniqueId(GetRoot, specialPropertyId);
            }
            // FIXME: Rewrite this with Rails1.x version flag
            if (laidTileId != 0)
            {
                sLaidTileId = laidTileId.ToString();
            }
            if (sLaidTileId != null)
            {
                laidTile = tmgr.GetTile(sLaidTileId);
            }

            if (chosenHexName != null && chosenHexName.Length > 0)
            {
                chosenHex = mmgr.GetHex(chosenHexName);
            }

        }

        private void BuildLocationNameString()
        {
            StringBuilder b = new StringBuilder();
            foreach (MapHex hex in locations)
            {
                if (b.Length > 0) b.Append(",");
                b.Append(hex.Id);
            }
            locationNames = b.ToString();
        }

        override public bool IsCorrection
        {
            get
            {
                return (type == LayTile.CORRECTION);
            }
        }

        public int CompareTo(LayTile o)
        {
            if (o == null) return 1;

            int result = type.CompareTo(o.type);
            if (result != 0) return result;

            if (specialProperty == o.specialProperty) return 0;
            if (specialProperty == null) return 1;
            if (o.specialProperty == null) return -1;

            return specialProperty.CompareTo(o.specialProperty);
            //return ComparisonChain.start()
            //        .compare(this.type, o.type)
            //        .compare(this.specialProperty, o.specialProperty, Ordering.natural().nullsLast())
            //        .result();

        }
    }
}
