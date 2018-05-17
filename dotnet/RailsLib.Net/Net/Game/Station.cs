using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;


/**
 * A Station object represents any junction on a tile, where one, two or more
 * track fragments meet. The usual Station types are "City", "Town" and
 * "OffMapCity". Other types found in some games are "Pass" (1841), "Port"
 * (1841, 18EU) and "Halt" (1860). <p> The station types "City" and "OffMapCity"
 * may have slots for placing tokens. <p> Station objects are used in Tile
 * objects, to represent the station(s) on a tile. Each tile type is represented
 * by just one Tile object (which is NOT cloned or newly instantiated when a
 * Tile is laid). Please note, that all preprinted tiles on the map are also
 * represented by Tile objects, so laying the first tile on a hex is treated as
 * a normal upgrade in this program. <p> See also the Stop class, which
 * represents stations on tiles that have actually been laid on a MapHex.
 * 
 * Station has the following ids:
 * string id: The attribute "id" of the station tag (e.g. "city1")
 * int number: The number inside the string (e.g. 1)
 * 
 */

namespace GameLib.Net.Game
{
    public class Station : TrackPoint, IComparable<Station>
    {
        private static Logger<Station> log = new Logger<Station>();

        public class StationType
        {
            public static StationType CITY = new StationType(StopType.Defaults.CITY, "City");
            public static StationType TOWN = new StationType(StopType.Defaults.TOWN, "Town");
            public static StationType HALT = new StationType(StopType.Defaults.TOWN, "Halt");
            public static StationType OFFMAPCITY = new StationType(StopType.Defaults.OFFMAP, "OffMap");
            public static StationType PORT = new StationType(StopType.Defaults.TOWN, "Port");
            public static StationType PASS = new StationType(StopType.Defaults.CITY, "Pass");
            public static StationType JUNCTION = new StationType(StopType.Defaults.NULL, "Junction");

            private StopType stopType;
            private string text;

            private StationType(StopType.Defaults type, string text)
            {
                this.stopType = type.StopType;
                this.text = text;
            }
            public StopType StopType
            {
                get
                {
                    return stopType;
                }
            }
            public string ToText()
            {
                return text;
            }
            public static StationType ValueOf(string s)
            {
                switch (s)
                {
                    case "CITY":
                        return CITY;
                    case "TOWN":
                        return TOWN;
                    case "HALT":
                        return HALT;
                    case "OFFMAPCITY":
                        return OFFMAPCITY;
                    case "PORT":
                        return PORT;
                    case "PASS":
                        return PASS;
                    case "JUNCTION":
                        return JUNCTION;
                    default:
                        return null;
                }
            }
        }

        private string id;
        private Station.StationType type;
        private int number;
        private int value;
        private int baseSlots;
        private Tile tile;
        private int position;
        private string stopName;

        private Station(Tile tile, int number, string id, StationType type, int value,
                int slots, int position, string cityName)
        {
            this.tile = tile;
            this.number = number;
            this.id = id;
            this.type = type;
            this.value = value;
            this.baseSlots = slots;
            this.position = position;
            this.stopName = cityName;
            log.Debug("Created " + this);
        }

        public static Station Create(Tile tile, Tag stationTag)
        {
            string sid = stationTag.GetAttributeAsString("id");

            if (sid == null)
                throw new ConfigurationException(LocalText.GetText(
                        "TileStationHasNoID", tile.Id));

            int number = -TrackPoint.ParseTrackPointNumber(sid);

            string stype = stationTag.GetAttributeAsString("type");
            if (stype == null)
                throw new ConfigurationException(LocalText.GetText(
                        "TileStationHasNoType", tile.Id));

            StationType type = StationType.ValueOf(stype.ToUpper());
            if (type == null)
            {
                throw new ConfigurationException(LocalText.GetText(
                        "TileStationHasInvalidType",
                        tile.Id,
                        type));
            }
            int value = stationTag.GetAttributeAsInteger("value", 0);
            int slots = stationTag.GetAttributeAsInteger("slots", 0);
            int position = stationTag.GetAttributeAsInteger("position", 0);
            string cityName = stationTag.GetAttributeAsString("city");
            return new Station(tile, number, sid, type, value, slots,
                        position, cityName);
        }


        public string Name
        {
            get
            {
                return "Station " + id + " on " + tile.GetType().Name + " "
                + tile.ToText();
            }
        }

        public string StopName
        {
            get
            {
                return stopName;
            }
        }

        /**
         * @return Returns the holder.
         */
        public Tile Tile
        {
            get
            {
                return tile;
            }
        }

        /**
         * @return Returns the id.
         */
        public string Id
        {
            get
            {
                return id;
            }
        }

        public int Number
        {
            get
            {
                return number;
            }
        }

        /**
         * @return Returns the baseSlots.
         */
        public int BaseSlots
        {
            get
            {
                return baseSlots;
            }
        }

        /**
         * @return Returns the value.
         */
        public int Value
        {
            get
            {
                return value;
            }
        }

        public int Position
        {
            get
            {
                return position;
            }
        }

        public StopType StopType
        {
            get
            {
                return type.StopType;
            }
        }

        public StationType GetStationType()
        {
            return type;
        }

        // TrackPoint methods
        override public int TrackPointNumber
        {
            get
            {
                return -number;
            }
        }

        override public TrackPointTypeEnum TrackPointType
        {
            get
            {
                return TrackPointTypeEnum.STATION;
            }
        }

        override public TrackPoint Rotate(HexSide rotation)
        {
            return this;
        }

        public string ToText()
        {
            return type.ToText() + " " + number;
        }

        // Comparable method
        public int CompareTo(Station other)
        {
            if (other == null) return 1;

            return this.Id.CompareTo(other.Id);
        }

        override public string ToString()
        {
            return "Station " + number + " on tile #" + tile.Id + " ID: " + id
            + ", Type: " + type + ", Slots: " + baseSlots + ", Value: "
            + value + ", Position:" + position;
        }
    }
}
