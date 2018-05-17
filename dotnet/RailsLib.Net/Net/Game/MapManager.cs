using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * MapManager configures the map layout from XML
 */

namespace GameLib.Net.Game
{
    public class MapManager : RailsManager, IConfigurable
    {
        private MapOrientation mapOrientation;

        private Dictionary<MapHex.Coordinates, MapHex> hexes;
        //private ImmutableTable<MapHex, HexSide, MapHex> hexTable;
        private Table<MapHex, HexSide, MapHex> hexTable;

        private MapHex.Coordinates minimum;
        private MapHex.Coordinates maximum;

        // upgrade costs on the map for noMapMode
        private List<int> possibleTileCosts; // Note, this will be sorted

        // Stop property defaults per stop type
        private IReadOnlyDictionary<string, StopType> defaultStopTypes;

        // if required: distance table
        private Table<MapHex, MapHex, int> hexDistances;

        // Optional map image (SVG file)
        // FIXME: Move to UI class
        private string mapImageFilename = null;
        private string mapImageFilepath = null;
        private int mapXOffset = 0;
        private int mapYOffset = 0;
        private float mapScale = (float)1.0;
        private bool mapImageUsed = false;

        /**
         * Used by Configure (via reflection) only
         */
        public MapManager(RailsRoot parent, string id) : base(parent, id)
        {

        }

        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {
            mapOrientation = MapOrientation.Create(tag);

            List<Tag> hexTags = tag.GetChildren("Hex");
            Dictionary<MapHex.Coordinates, MapHex> hexBuilder = new Dictionary<MapHex.Coordinates, MapHex>();
            List<int> tileCostsBuilder = new List<int>();

            foreach (Tag hexTag in hexTags)
            {
                MapHex hex = MapHex.Create(this, hexTag);
                hexBuilder[hex.GetCoordinates()] = hex;
                tileCostsBuilder.AddRange(hex.TileCostsList);
            }
            hexes = hexBuilder;
            tileCostsBuilder.Sort();
            possibleTileCosts = tileCostsBuilder;

            minimum = MapHex.Coordinates.Minimum(hexes.Values);
            maximum = MapHex.Coordinates.Maximum(hexes.Values);

            // Default Stop Types
            Tag defaultsTag = tag.GetChild("Defaults");
            if (defaultsTag != null)
            {
                List<Tag> accessTags = defaultsTag.GetChildren("Access");
                defaultStopTypes = StopType.ParseDefaults(this, accessTags);
            }
            else
            {
                defaultStopTypes = new Dictionary<string, StopType>();
            }

            // Map image attributes
            // FIXME: Move to an UI class
            Tag mapImageTag = tag.GetChild("Image");
            if (mapImageTag != null)
            {
                mapImageFilename = mapImageTag.GetAttributeAsString("file");
                mapXOffset = mapImageTag.GetAttributeAsInteger("x", mapXOffset);
                mapYOffset = mapImageTag.GetAttributeAsInteger("y", mapYOffset);
                mapScale = mapImageTag.GetAttributeAsFloat("scale", mapScale);
            }
        }

        public void FinishConfiguration(RailsRoot root)
        {
            foreach (MapHex hex in hexes.Values)
            {
                hex.FinishConfiguration(root);
            }

            // Initialize the neighbors
            Table<MapHex, HexSide, MapHex> hexTableBuilder = new Table<MapHex, HexSide, MapHex>();
            foreach (MapHex hex in hexes.Values)
            {
                foreach (HexSide side in HexSide.All())
                {
                    var h = mapOrientation.GetAdjacentCoordinates(hex.GetCoordinates(), side);
                    MapHex neighbor = null;
                    if (hexes.ContainsKey(h))
                    {
                        neighbor = hexes[h];
                    }
                    if (neighbor != null)
                    {
                        if (hex.IsValidNeighbor(neighbor, side))
                        {
                            hexTableBuilder.Put(hex, side, neighbor);
                        }
                        else
                        {
                            hex.AddInvalidSide(side);
                            if (hex.IsImpassableNeighbor(neighbor))
                            {
                                hex.AddImpassableSide(side);
                                neighbor.AddImpassableSide(side.Opposite);
                            }
                        }
                    }
                    else
                    { // neighbor is null
                        hex.AddInvalidSide(side);
                    }
                }
            }
            hexTable = hexTableBuilder;

            foreach (PublicCompany company in root.CompanyManager.GetAllPublicCompanies())
            {
                List<MapHex> homeHexes = company.GetHomeHexes();
                if (homeHexes != null)
                {
                    foreach (MapHex homeHex in homeHexes)
                    {
                        int homeNumber = company.HomeCityNumber;
                        Stop home = homeHex.GetRelatedStop(homeNumber);
                        if (home == null && homeNumber != 0)
                        {
                            throw new ConfigurationException("Invalid home number " + homeNumber + " for hex " + homeHex
                                    + " which has " + homeHex.Stops.Count + " stop");
                        }
                        else
                        {
                            homeHex.AddHome(company, home);
                        }
                    }
                }
                MapHex hex = company.DestinationHex;
                if (hex != null)
                {
                    hex.AddDestination(company);
                }
            }

            // FIXME: Move this configuration to an UI class
            // #FIXME_GUI_INTERFACE
            mapImageUsed = !string.IsNullOrEmpty(mapImageFilename)
                && "yes".Equals(Config.Get("map.image.display"), StringComparison.OrdinalIgnoreCase);
            if (mapImageUsed)
            {
                string rootDirectory = Config.Get("map.root_directory");
                if (string.IsNullOrEmpty(rootDirectory))
                {
                    rootDirectory = "data";
                }
                mapImageFilepath = "/" + rootDirectory + "/" + mapImageFilename;
            }

        }

        /**
         * @return Returns the currentTileOrientation.
         */
        public MapOrientation MapOrientation
        {
            get
            {
                return mapOrientation;
            }
        }

        /**
         * @return Returns the hexes.
         */
        public ICollection<MapHex> GetHexes()
        {
            return hexes.Values;
        }


        public MapHex GetNeighbor(MapHex hex, HexSide side)
        {
            return hexTable.Get(hex, side);
        }

        public MapHex GetHex(string locationCode)
        {
            // MapManager is a RailsManager so it is possible to locate by id
            return (MapHex)Locate(locationCode);
        }

        public MapHex.Coordinates Minimum
        {
            get
            {
                return minimum;
            }
        }

        public MapHex.Coordinates Maximum
        {
            get
            {
                return maximum;
            }
        }

        public string MapUIClassName
        {
            get
            {
                return mapOrientation.GetUIClassName();
            }
        }

        public IReadOnlyDictionary<string, StopType> DefaultStopTypes
        {
            get
            {
                return defaultStopTypes;
            }
        }

        public List<Stop> GetCurrentStops()
        {
            List<Stop> stops = new List<Stop>();
            foreach (MapHex hex in hexes.Values)
            {
                stops.AddRange(hex.Stops);
            }
            return stops;
        }

        public List<int> PossibleTileCosts()
        {
            return possibleTileCosts;
        }

        public List<MapHex> ParseLocations(string locationCodes)
        {

            List<MapHex> locationBuilder = new List<MapHex>();
            foreach (string hexName in locationCodes.Split(','))
            {
                MapHex hex = GetHex(hexName);
                if (hex != null)
                {
                    locationBuilder.Add(hex);
                }
                else
                {
                    throw new ArgumentException("Invalid hex " + hexName +
                            " specified in location string " + locationCodes);
                }
            }
            return locationBuilder;
        }

        /**
         * Calculate the distance between two hexes as in 1835,
         * i.e. as "the crow without a passport flies".
         */
        public int GetHexDistance(MapHex hex1, MapHex hex2)
        {
            if (hexDistances == null)
            {
                hexDistances = new Table<MapHex, MapHex, int>();
            }

            if (!hexDistances.Contains(hex1, hex2))
            {
                CalculateHexDistances(hex1, hex1, 0);
            }
            return hexDistances.Get(hex1, hex2);
        }

        private void CalculateHexDistances(MapHex initHex, MapHex currentHex, int depth)
        {
            hexDistances.Put(initHex, currentHex, depth);

            // check for next hexes
            depth++;
            foreach (MapHex nextHex in hexTable.GetRow(currentHex).Values)
            {
                if (!hexDistances.Contains(initHex, nextHex) ||
                        depth < hexDistances.Get(initHex, nextHex))
                {
                    CalculateHexDistances(initHex, nextHex, depth);
                }
            }
        }


        /**
         * Calculate the distances between a given tokenable city hex
         * and all other tokenable city hexes.
         * <p> Distances are cached.
         * @param hex Start hex
         * @return Sorted integer list containing all occurring distances only once.
         */
        public List<int> GetCityDistances(MapHex initHex)
        {
            if (hexDistances == null)
            {
                hexDistances = new Table<MapHex, MapHex, int>();
            }

            if (!hexDistances.ContainsRow(initHex))
            {
                CalculateHexDistances(initHex, initHex, 0);
            }

            List<int> distances = new List<int>();

            foreach (var otherHex in hexDistances.GetRow(initHex))
            {
                if (otherHex.Key.CurrentTile.HasStations)
                {
                    distances.Add(otherHex.Value);
                }
            }
            distances.Sort();
            return distances;
        }

        public string MapImageFilepath
        {
            get
            {
                return mapImageFilepath;
            }
        }

        public int MapXOffset
        {
            get
            {
                return mapXOffset;
            }
            set
            {
                mapXOffset = value;
            }
        }

        public int MapYOffset
        {
            get
            {
                return mapYOffset;
            }
            set
            {
                mapYOffset = value;
            }
        }

        public float MapScale
        {
            get
            {
                return mapScale;
            }
            set
            {
                mapScale = value;
            }
        }

        public bool IsMapImageUsed
        {
            get
            {
                return mapImageUsed;
            }
        }

        public class Table<R, C, V>
        {
            public Table()
            {
                Data = new Dictionary<R, Dictionary<C, V>>();
            }

            public Dictionary<R, Dictionary<C, V>> Data { get; private set; }

            public Dictionary<C, V> GetRow(R row)
            {
                return Data[row];
            }

            public V Get(R row, C col)
            {
                return Data[row][col];
            }

            public void Put(R row, C col, V value)
            {
                if (!ContainsRow(row))
                {
                    Data[row] = new Dictionary<C, V>();
                }
                Data[row][col] = value;
            }

            public bool Contains(R row, C col)
            {
                return Data.ContainsKey(row) && Data[row].ContainsKey(col);
            }

            public bool ContainsRow(R row)
            {
                return Data.ContainsKey(row);
            }
        }
    }
}
