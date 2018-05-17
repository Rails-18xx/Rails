using GameLib.Net.Algorithms;
using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

/**
 * Represents a certain tile <i>type</i>, identified by its id (tile number).
 * <p> For each tile number, only one tile object is created. The list
 * <b>tilesLaid</b> records in which hexes a certain tile number has been laid.

 */
namespace GameLib.Net.Game
{
    public class Tile : RailsModel, IComparable<Tile>
    {
        public enum Quantity { LIMITED, UNLIMITED, FIXED }

        private static Logger<Tile> log = new Logger<Tile>();

        /**
         * The 'external id', which is shown in the UI. Usually equal to the
         * internal id, but different in case of duplicates.
         */
        private string externalId;

        /**
         * The 'picture id', identifying the picture number to be loaded. Usually
         * equal to the internal id, but different in case of graphical variants
         * (such as the 18EU tiles 80-83).
         */
        private string pictureId;

        /**
         * The 'sorting id' which defines the ordering
         */
        private string sortingId;

        // if tile is painted on the map (for UI reasons)
        private bool prepainted;

        private TileColor color;
        private Dictionary<int, Station> stations;

        private TrackConfig trackConfig;
        private HexSidesSet possibleRotations;
        private List<TileUpgrade> upgrades;

        private Quantity quantity;
        private int count;
        private bool allowsMultipleBasesOfOneCompany = false;

        /** Fixed orientation; null if free to rotate */
        private HexSide fixedOrientation = null;

        // Stop properties
        private StopType stopType = null;

        /**
         * Flag indicating that player must reposition any basetokens during the
         * upgrade.
         */
        private bool relayBaseTokensOnUpgrade = false;

        /**
         * Records in which hexes a certain tile number has been laid. The size of
         * the collection indicates the number of tiles laid on the map board.
         */
        private HashSetState<MapHex> tilesLaid;

        /** Storage of revenueBonus that are bound to the tile */
        private List<RevenueBonusTemplate> revenueBonuses = null;

        /** CountModel to display the number of available tiles */
        private CountModel countModel;

        private Tile(IRailsItem owner, string id) : base(owner, id)
        {
            tilesLaid = HashSetState<MapHex>.Create(this, "tilesLaid");
            countModel = new CountModel(this);
        }

        public static Tile Create(TileManager parent, string id)
        {
            return new Tile(parent, id);
        }

        new public TileManager Parent
        {
            get
            {
                return (TileManager)base.Parent;
            }
        }

        /**
         * @param se &lt;Tile&gt; element from TileSet.xml
         * @param te &lt;Tile&gt; element from Tiles.xml
         */
        public void ConfigureFromXML(Tag setTag, Tag defTag)
        {

            if (defTag == null)
            {
                throw new ConfigurationException(LocalText.GetText("TileMissing", Id));
            }

            string colorName = defTag.GetAttributeAsString("color");
            if (colorName == null)
                throw new ConfigurationException(LocalText.GetText("TileColorMissing", Id));
            if (colorName.Equals("gray")) colorName = "grey";
            try
            {
                color = TileColor.ValueOf(colorName);
            }
            catch (ArgumentException e)
            {
                throw new ConfigurationException(LocalText.GetText(
                        "InvalidTileColourName", Id, colorName), e);
            }

            /* Stations */
            List<Tag> stationTags = defTag.GetChildren("Station");
            OrderedDictionary<int, Station> stationBuilder = new OrderedDictionary<int, Station>();
            if (stationTags != null)
            {
                foreach (Tag stationTag in stationTags)
                {
                    Station station = Station.Create(this, stationTag);
                    stationBuilder[station.Number] = station;
                }
            }
            stations = new Dictionary<int, Station>(stationBuilder);

            /* Tracks (only number per side, no cities yet) */
            List<Tag> trackTags = defTag.GetChildren("Track");
            List<Track> trackBuilder = new List<Track>();
            if (trackTags != null)
            {
                foreach (Tag trackTag in trackTags)
                {
                    string fromStr = trackTag.GetAttributeAsString("from");
                    string toStr = trackTag.GetAttributeAsString("to");
                    if (fromStr == null || toStr == null)
                    {
                        throw new ConfigurationException(LocalText.GetText("FromOrToMissing", Id));
                    }
                    TrackPoint from = TrackPoint.Create(this, fromStr);
                    TrackPoint to = TrackPoint.Create(this, toStr);
                    Track track = new Track(from, to);
                    trackBuilder.Add(track);
                }
            }
            trackConfig = new TrackConfig(this, trackBuilder);

            // define possibleRotations
            HashSet<TrackConfig> trackConfigsBuilder = new HashSet<TrackConfig>();
            HexSidesSet.Builder rotationsBuilder = HexSidesSet.GetBuilder();

            trackConfigsBuilder.Add(trackConfig);
            rotationsBuilder.Set(HexSide.DefaultRotation);
            foreach (HexSide rotation in HexSide.AllExceptDefault())
            {
                TrackConfig nextConfig = TrackConfig.CreateByRotation(trackConfig, rotation);
                if (trackConfigsBuilder.Contains(nextConfig)) continue;
                trackConfigsBuilder.Add(nextConfig);
                rotationsBuilder.Set(rotation);
            }
            possibleRotations = rotationsBuilder.Build();
            log.Debug("Allowed rotations for " + Id + " are " + possibleRotations);

            /* External (printed) id */
            externalId = setTag.GetAttributeAsString("extId", Id);

            /* Picture id */
            pictureId = setTag.GetAttributeAsString("pic", Id);

            /* prepainted */
            try
            {
                int intNb = int.Parse(Id);
                if (intNb <= 0)
                {
                    prepainted = true;
                }
                else
                {
                    prepainted = false;
                }
            }
            catch (FormatException)
            {
                // assume that it is not pre-painted to be save if id is non-numerical
                prepainted = false;
            }


            /* Quantity */
            count = setTag.GetAttributeAsInteger("quantity", 0);
            /* Value '99' and '-1' mean 'unlimited' */
            /*
             * BR: added option for unlimited plain tiles: tiles with one track and
             * no stations
             */
            string unlimitedTiles = GetRoot.GameOptions.Get("UnlimitedTiles");
            if (count == 99 || count == -1
                     || "yes".Equals(unlimitedTiles, StringComparison.OrdinalIgnoreCase)
                     || ("yellow plain".Equals(unlimitedTiles))
                         && trackConfig.Count == 1 && stations.Count == 0)
            {
                quantity = Quantity.UNLIMITED;
                count = 0;
            }
            else if (count == 0)
            {
                quantity = Quantity.FIXED;
            }
            else
            {
                quantity = Quantity.LIMITED;
                count += setTag.GetAttributeAsInteger("quantityIncrement", 0);
            }

            /* Multiple base tokens of one company allowed */
            allowsMultipleBasesOfOneCompany =
                    setTag.HasChild("AllowsMultipleBasesOfOneCompany");

            int orientation = setTag.GetAttributeAsInteger("orientation", -1);
            if (orientation != -1)
            {
                fixedOrientation = HexSide.Get(orientation);
            }

            /* Upgrades */
            List<Tag> upgradeTags = setTag.GetChildren("Upgrade");

            if (upgradeTags != null)
            {
                upgrades = TileUpgrade.CreateFromTags(this, upgradeTags);
            }
            else
            {
                upgrades = new List<TileUpgrade>();
            }

            // Set reposition base tokens flag
            relayBaseTokensOnUpgrade =
                    setTag.GetAttributeAsBoolean("relayBaseTokens", relayBaseTokensOnUpgrade);

            // revenue bonus
            List<Tag> bonusTags = setTag.GetChildren("RevenueBonus");
            if (bonusTags != null)
            {
                revenueBonuses = new List<RevenueBonusTemplate>();
                foreach (Tag bonusTag in bonusTags)
                {
                    RevenueBonusTemplate bonus = new RevenueBonusTemplate();
                    bonus.ConfigureFromXML(bonusTag);
                    revenueBonuses.Add(bonus);
                }
            }

            // Stop properties
            Tag accessTag = setTag.GetChild("Access");
            stopType = StopType.ParseStop(this, accessTag, Parent.DefaultStopTypes);
        }

        public void FinishConfiguration(RailsRoot root, int sortingDigits)
        {
            try
            {
                int externalNb = int.Parse(externalId);
                //NumberFormat nf = NumberFormat.getInstance();
                //nf.setMinimumIntegerDigits(sortingDigits);
                string format = $"{{0:D{sortingDigits}}}";
                sortingId = string.Format(format, externalNb); //nf.format(externalNb);
            }
            catch (FormatException)
            {
                sortingId = externalId;
            }

            foreach (TileUpgrade upgrade in upgrades)
            {
                upgrade.FinishConfiguration(root);
            }
        }

        public TileColor Color
        {
            get
            {
                return color;
            }
        }

        public string ColorText
        {
            get
            {
                return color.ToText();
            }
        }

        public int ColorNumber
        {
            get
            {
                return color.Number;
            }
        }

        public string PictureId
        {
            get
            {
                return pictureId;
            }
        }

        public bool IsPrepainted
        {
            get
            {
                return prepainted;
            }
        }

        public bool HasTracks(HexSide side)
        {
            return trackConfig.HasSideTracks(side);
        }

        public List<TrackPoint> GetTracks(HexSide side)
        {
            return trackConfig.GetSideTracks(side);
        }

        public TrackConfig TrackConfig
        {
            get
            {
                return trackConfig;
            }
        }

        /**
         * Is a tile upgradeable at any time (regardless of the phase)?
         */
        public bool IsUpgradeable
        {
            get
            {
                return color.IsUpgradeable;
            }
        }

        public bool AllowsMultipleBasesOfOneCompany
        {
            get
            {
                return allowsMultipleBasesOfOneCompany;
            }
        }

        public List<TileUpgrade> TileUpgrades
        {
            get
            {
                return upgrades;
            }
        }

        /**
         * Get all possible upgrades for a specific tile on a certain hex
         */

        public List<Tile> GetAllUpgrades(MapHex hex)
        {
            List<Tile> upgr = new List<Tile>();
            foreach (TileUpgrade upgrade in upgrades)
            {
                Tile tile = upgrade.TargetTile;
                if (upgrade.IsAllowedForHex(hex))
                {
                    upgr.Add(tile);
                }
            }
            return upgr;
        }

        public TileUpgrade GetSpecificUpgrade(Tile targetTile)
        {
            foreach (TileUpgrade upgrade in upgrades)
            {
                if (upgrade.TargetTile == targetTile)
                {
                    return upgrade;
                }
            }
            return TileUpgrade.CreateSpecific(this, targetTile);
        }

        /** Get a delimited list of all possible upgrades, regardless current phase */
        public string GetUpgradesString(MapHex hex)
        {
            StringBuilder b = new StringBuilder();
            Tile tile;
            foreach (TileUpgrade upgrade in upgrades)
            {
                tile = upgrade.TargetTile;
                if (upgrade.IsAllowedForHex(hex))
                {
                    if (b.Length > 0) b.Append(",");
                    b.Append(tile.ToText());
                }
            }

            return b.ToString();
        }

        public bool HasStations
        {
            get
            {
                return stations.Count > 0;
            }
        }

        public Station GetStation(int id)
        {
            return stations[id];
        }

        public IReadOnlyCollection<Station> Stations
        {
            get
            {
                return stations.Values; // ImmutableSet.copyOf(stations.values());
            }
        }

        public List<Track> Tracks
        {
            get
            {
                return trackConfig.Tracks;
            }
        }

        public bool HasNoStationTracks
        {
            get
            {
                return trackConfig.HasNoStationTracks;
            }
        }

        public ICollection<TrackPoint> GetTracksPerStation(Station station)
        {
            return trackConfig.GetStationTracks(station);
        }

        public HexSidesSet PossibleRotations
        {
            get
            {
                return possibleRotations;
            }
        }

        public int NumStations
        {
            get
            {
                return stations.Count;
            }
        }

        private int NumSlots
        {
            get
            {
                int slots = 0;
                foreach (Station station in stations.Values)
                {
                    slots += station.BaseSlots;
                }
                return slots;
            }
        }

        public bool RelayBaseTokensOnUpgrade
        {
            get
            {
                return relayBaseTokensOnUpgrade;
            }
        }

        public StopType StopType
        {
            get
            {
                return stopType;
            }
        }

        /** Register a tile of this type being laid on the map. */
        public bool Add(MapHex hex)
        {
            tilesLaid.Add(hex);
            return true;
        }

        /** Register a tile of this type being removed from the map. */
        public bool Remove(MapHex hex)
        {
            return tilesLaid.Remove(hex);
        }

        public int InitialCount
        {
            get
            {
                return count;
            }
        }

        public bool IsUnlimited
        {
            get
            {
                return quantity == Quantity.UNLIMITED;
            }
        }

        public bool IsFixed
        {
            get
            {
                return quantity == Quantity.FIXED;
            }
        }

        /** Return the number of free tiles */
        public int FreeCount
        {
            get
            {
                switch (quantity)
                {
                    case Quantity.LIMITED:
                        return count - tilesLaid.Count;
                    case Quantity.UNLIMITED:
                        return 1;
                    case Quantity.FIXED:
                        return 0;
                }
                return 0; // cannot happen but still
            }
        }

        public CountModel GetCountModel()
        {
            return countModel;
        }

        public HexSide FixedOrientation
        {
            get
            {
                return fixedOrientation;
            }
        }

        public List<RevenueBonusTemplate> RevenueBonuses
        {
            get
            {
                return revenueBonuses;
            }
        }


        override public string ToText()
        {
            return externalId;
        }

        /** ordering of tiles based first on colour, then on external id.
         * Here the external id is 
         *  */

        public int CompareTo(Tile other)
        {
            if (other == null) return 1;

            int result = color.CompareTo(other.color);
            if (result != 0) return result;

            result = other.NumSlots.CompareTo(NumSlots);
            if (result != 0) return result;

            result = other.NumStations.CompareTo(NumStations);
            if (result != 0) return result;

            result = Tracks.Count.CompareTo(other.Tracks.Count);
            if (result != 0) return result;

            return sortingId.CompareTo(other.sortingId);
            //return ComparisonChain.start()
            //        .compare(this.colour, other.colour)
            //        .compare(other.getNumSlots(), this.getNumSlots())
            //        .compare(other.getNumStations(), this.getNumStations())
            //        .compare(this.getTracks().size(), other.getTracks().size())
            //        .compare(this.sortingId, other.sortingId)
            //        .result();
        }

        public class CountModel : RailsModel
        {

            public CountModel(Tile parent) : base(parent, "CountModel")
            {

            }

            override public string ToText()
            {
                Tile parent = (Tile)Parent;
                string count = null;
                switch (parent.quantity)
                {
                    case Quantity.LIMITED:
                        count = " (" + parent.FreeCount.ToString() + ")";
                        break;
                    case Quantity.UNLIMITED:
                        count = " (+)";
                        break;
                    case Quantity.FIXED:
                        count = "";
                        break;
                }
                return "#" + parent.externalId + count;
            }
        }
    }
}
