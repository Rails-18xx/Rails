using GameLib.Net.Algorithms;
using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

// TODO: Rewrite the mechanisms as model

/**
 * Represents a Hex on the Map from the Model side.
 */

namespace GameLib.Net.Game
{
    public class MapHex : RailsModel, IRailsOwner, IConfigurable
    {
        private static Logger<MapHex> log = new Logger<MapHex>();

        public class Coordinates
        {

            // externally used coordinates
            private int row;
            private int col;

            private static Regex namePattern = new Regex("(\\D+?)(-?\\d+)", RegexOptions.Compiled);

            private Coordinates(int row, int col)
            {
                this.row = row;
                this.col = col;
            }

            public static Coordinates CreateFromId(string id, MapOrientation mapOrientation)
            {
                Match m = namePattern.Match(id);

                if (!m.Success || m.Groups.Count < 3)
                {
                    throw new ConfigurationException("Invalid name format: " + id);
                }
                string letters = m.Groups[1].Value;
                int letter;
                if (letters.Length == 1)
                {
                    letter = letters[0];
                }
                else
                { // for row 'AA' in 1825U1
                    letter = 26 + letters[1];
                }
                // FIXME: Replace with negative numbers instead of > 100
                int number;
                try
                {
                    number = int.Parse(m.Groups[2].Value);
                    if (number > 90) number -= 100; // For 1825U1 column 99 (= -1)
                }
                catch (FormatException)
                {
                    throw new ConfigurationException(
                            "Invalid number format: " + m.Groups[2].Value);
                }

                /*
                 * Translate hex names (as on the board) to coordinates used for
                 * drawing.
                 */
                int row, column;
                if (mapOrientation.LettersGoHorizontal)
                {
                    row = number;
                    column = letter - '@';
                }
                else
                { // letters go vertical (normal case)
                    row = letter - '@';
                    column = number;
                }
                return new Coordinates(row, column);
            }

            public static Coordinates Maximum(IEnumerable<MapHex> hexes)
            {
                int maxRow, maxCol;
                maxRow = maxCol = int.MinValue;
                foreach (MapHex hex in hexes)
                {
                    Coordinates coordinates = hex.coordinates;
                    maxRow = Math.Max(maxRow, coordinates.row);
                    maxCol = Math.Max(maxCol, coordinates.col);
                }
                return new Coordinates(maxRow, maxCol);
            }

            public static Coordinates Minimum(IEnumerable<MapHex> hexes)
            {
                int minRow, minCol;
                minRow = minCol = int.MaxValue;
                foreach (MapHex hex in hexes)
                {
                    Coordinates coordinates = hex.coordinates;
                    minRow = Math.Min(minRow, coordinates.row);
                    minCol = Math.Min(minCol, coordinates.col);
                }
                return new Coordinates(minRow, minCol);
            }

            public int Row
            {
                get
                {
                    return row;
                }
            }

            public int Col
            {
                get
                {
                    return col;
                }
            }

            public Coordinates Translate(int deltaRow, int deltaCol)
            {
                return new Coordinates(row + deltaRow, col + deltaCol);
            }

            override public int GetHashCode()
            {
                return (int)(((uint)row) << 16 + (col & 0xFFFF));  //Objects.hashCode(row, col);
            }

            override public bool Equals(object other)
            {
                if (!(other is Coordinates)) return false;
                return row == ((Coordinates)other).row
                       && col == ((Coordinates)other).col;
            }

            override public string ToString()
            {
                //return Objects.toStringHelper(this).addValue(row).addValue(
                //        col).toString();
                return $"{this.GetType().Name}{{row={row}}}{{col={col}}}";
            }
        }

        ////////////////////////
        // static fields
        ////////////////////////

        public Coordinates coordinates;

        private string preprintedTileId;
        private string preprintedPictureId;
        private HexSide preprintedTileRotation;

        private List<int> tileCost;

        private string stopName;
        private string reservedForCompanyName = null;
        private PublicCompany reservedForCompany = null;

        /** Values if this is an off-board hex */
        private List<int> valuesPerPhase = null;

        /*
         * Temporary storage for impassable hexsides. Once neighbors has been set
         * up, this attribute is no longer used. Only the black or blue bars on the
         * map need be specified, and each one only once. Impassable non-track sides
         * of "offboard" (red) and "fixed" (grey or brown) preprinted tiles will be
         * derived and need not be specified.
         */
        private string impassableTemplate = null;
        private HexSidesSet.Builder impassableBuilder = HexSidesSet.GetBuilder();
        private HexSidesSet impassableSides;

        private HexSidesSet.Builder invalidBuilder = HexSidesSet.GetBuilder();
        private HexSidesSet invalidSides;

        private List<PublicCompany> destinations = null;

        /** Storage of revenueBonus that are bound to the hex */
        private List<RevenueBonusTemplate> revenueBonuses = null;

        /**
         * Optional attribute to provide the type of any stops on the hex. Normally
         * the type will be derived from the tile properties.
         */
        private StopType stopType = null;

        ////////////////////////
        // dynamic fields
        ////////////////////////
        private GenericState<Tile> currentTile;
        private GenericState<HexSide> currentTileRotation;

        // Stops (Cities, Towns etc.)
        private BiDictionaryState<Station, Stop> stops;

        // Homes (in 18EU and others the home is selected later in the game
        // Remark: this was a static field in Rails1.x, causing potential undo
        // problems
        private DictionaryState<PublicCompany, Stop> homes;

        private GenericState<PrivateCompany> blockingPrivateCompany;

        /**
         * Is the hex blocked for home tokens? <p> NOTE:<br> ALWAYS means: Always
         * Blocked, no token lay possible (until attribute is changed) RESERVE_SLOT
         * means: Reserves slots (for multi-cities depending on
         * isHomeBlockedForAllCities<br> NEVER means: Never blocked (unless there is
         * not a single free slot remaining)<br> Remark: The latter is used for 1835
         * Berlin, which is home to PR, but the absence of a PR token does not block
         * the third slot when the green tile is laid. <br>
         * 
         * Remark: in Rails 1.x it was a static field, causing potential undo
         * problems
         */
        public enum BlockedToken
        {
            ALWAYS, RESERVE_SLOT, NEVER
        };

        private GenericState<BlockedToken> isBlockedForTokenLays;

        /** OffStation BonusTokens */
        private PortfolioSet<BonusToken> bonusTokens;

        private MapHex(MapManager parent, string id, Coordinates coordinates) : base(parent, id)
        {
            currentTile = GenericState<Tile>.Create(this, "currentTile");
            currentTileRotation = GenericState<HexSide>.Create(this, "currentTileRotation");
            stops = BiDictionaryState<Station, Stop>.Create(this, "stops");
            homes = DictionaryState<PublicCompany, Stop>.Create(this, "homes");
            blockingPrivateCompany = GenericState<PrivateCompany>.Create(this, "blockingPrivateCompany");
            isBlockedForTokenLays = GenericState<BlockedToken>.Create(this, "isBlockedForTokenLays");
            bonusTokens = PortfolioSet<BonusToken>.Create(this, "bonusTokens");

            this.coordinates = coordinates;
        }

        public static MapHex Create(MapManager parent, Tag tag)
        {
            // name serves as id
            string id = tag.GetAttributeAsString("name");
            Coordinates coordinates =
                        Coordinates.CreateFromId(id, parent.MapOrientation);
            MapHex hex = new MapHex(parent, id, coordinates);
            hex.ConfigureFromXML(tag);
            return hex;
        }

        public void ConfigureFromXML(Tag tag)
        {

            preprintedTileId = tag.GetAttributeAsString("tile", null);
            preprintedPictureId = tag.GetAttributeAsString("pic", preprintedTileId);
            int orientation = tag.GetAttributeAsInteger("orientation", 0);
            preprintedTileRotation = HexSide.Get(orientation);

            impassableTemplate = tag.GetAttributeAsString("impassable");
            tileCost = new List<int>(tag.GetAttributeAsIntegerList("cost"));

            // Off-board revenue values
            valuesPerPhase = new List<int>(tag.GetAttributeAsIntegerList("value"));

            // City name
            stopName = tag.GetAttributeAsString("city", "");

            if (tag.GetAttributeAsString("unlaidHomeBlocksTokens") == null)
            {
                // default (undefined) is RESERVE_SLOT
                isBlockedForTokenLays.Set(BlockedToken.RESERVE_SLOT);
            }
            else
            {
                if (tag.GetAttributeAsBoolean("unlaidHomeBlocksTokens", false))
                {
                    isBlockedForTokenLays.Set(BlockedToken.ALWAYS);
                }
                else
                {
                    isBlockedForTokenLays.Set(BlockedToken.NEVER);
                }
            }
            reservedForCompanyName = tag.GetAttributeAsString("reserved");

            // revenue bonus
            List<Tag> bonusTags = tag.GetChildren("RevenueBonus");
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
            Tag accessTag = tag.GetChild("Access");
            stopType = StopType.ParseStop(this, accessTag, Parent.DefaultStopTypes);
        }

        public void FinishConfiguration(RailsRoot root)
        {
            currentTile.Set(root.TileManager.GetTile(preprintedTileId));
            currentTileRotation.Set(preprintedTileRotation);

            reservedForCompany = GetRoot.CompanyManager.GetPublicCompany(reservedForCompanyName);

            // We need completely new objects, not just references to the Tile's
            // stations.
            foreach (Station station in currentTile.Value.Stations)
            {
                Stop stop = Stop.Create(this, station);
                stops.Put(station, stop);
            }

            impassableSides = impassableBuilder.Build();
            invalidSides = invalidBuilder.Build();
        }

        new public MapManager Parent
        {
            get
            {
                return (MapManager)base.Parent;
            }
        }

        public void AddImpassableSide(HexSide side)
        {
            impassableBuilder.Set(side);
            log.Debug("Added impassable " + side + " to " + this);
            // all impassable sides are invalids
            AddInvalidSide(side);
        }

        public HexSidesSet ImpassableSides
        {
            get
            {
                return impassableSides;
            }
        }

        public void AddInvalidSide(HexSide side)
        {
            invalidBuilder.Set(side);
            log.Debug("Added invalid " + side + " to " + this);
        }

        public HexSidesSet InvalidSides
        {
            get
            {
                return invalidSides;
            }
        }

        public bool IsImpassableNeighbor(MapHex neighbor)
        {
            return impassableTemplate != null
                   && impassableTemplate.IndexOf(neighbor.Id) > -1;
        }

        public bool IsValidNeighbor(MapHex neighbor, HexSide side)
        {
            if (IsImpassableNeighbor(neighbor)) return false;
            /*
             * The preprinted tile on this hex is offmap or fixed and has no track
             * to this side.
             */
            Tile neighborTile = neighbor.CurrentTile;
            if (neighborTile.IsUpgradeable) return true;
            HexSide rotated = (HexSide)side.Opposite.Rotate(neighbor.CurrentTileRotation.Negative);
            return neighborTile.HasTracks(rotated);
        }

        public string GetOrientationName(HexSide orientation)
        {
            return Parent.MapOrientation.GetORNames(orientation);
        }

        [Obsolete]
        public string GetOrientationName(int orientation)
        {
            return GetOrientationName(HexSide.Get(orientation));
        }

        /* ----- Instance methods ----- */

        public Coordinates GetCoordinates()
        {
            return coordinates;
        }

        public bool IsPreprintedTileCurrent
        {
            get
            {
                return currentTile.Value.Id.Equals(preprintedTileId);
            }
        }

        /**
         * @return Returns the preprintedTileId.
         */
        public string PreprintedTileId
        {
            get
            {
                return preprintedTileId;
            }
        }

        public HexSide PreprintedTileRotation
        {
            get
            {
                return preprintedTileRotation;
            }
        }

        /**
         * Return the current picture ID (i.e. the tile ID to be displayed, rather
         * than used for route determination). <p> Usually, the picture ID is equal
         * to the tile ID. Different values may be defined per hex or per tile.
         * Restriction: definitions per hex can apply to preprinted tiles only.
         * 
         * @return The current picture ID
         */
        public string GetPictureId(Tile tile)
        {
            if (tile.Id.Equals(preprintedTileId))
            {
                return preprintedPictureId;
            }
            else
            {
                return tile.PictureId;
            }
        }

        public Tile CurrentTile
        {
            get
            {
                return currentTile.Value;
            }
        }

        public HexSide CurrentTileRotation
        {
            get
            {
                return currentTileRotation.Value;
            }
        }

        public int GetTileCost()
        {
            if (IsPreprintedTileCurrent)
            {
                return GetTileCost(0);
            }
            else
            {
                return GetTileCost(currentTile.Value.ColorNumber);
            }
        }

        // TODO: Replace index by TileColours
        private int GetTileCost(int index)
        {
            try
            {
                return tileCost[index];
            }
            catch (IndexOutOfRangeException)
            {
                return 0;
            }
        }

        public List<int> TileCostsList
        {
            get
            {
                return tileCost;
            }
        }

        public StopType StopType
        {
            get
            {
                return stopType;
            }
        }

        /**
         * new wrapper function for the LayTile action that calls the actual upgrade
         * mehod
         * 
         * @param action executed LayTile action
         */
        public void Upgrade(LayTile action)
        {
            Tile newTile = action.LaidTile;
            HexSide newRotation = HexSide.Get(action.Orientation);
            Dictionary<string, int> relaidTokens = action.RelaidBaseTokens;

            Upgrade(newTile, newRotation, relaidTokens);
        }

        public void Upgrade(Tile newTile, HexSide newRotation, Dictionary<string, int> relaidTokens)
        {
            TileUpgrade upgrade = currentTile.Value.GetSpecificUpgrade(newTile);
            TileUpgrade.Rotation rotation = upgrade.GetRotation(
            (HexSide)newRotation.Rotate(currentTileRotation.Value.Negative));
            Dictionary<Station, Station> stationMapping;

            /*
             * Martin Brumm 17.12.2016 18AL and maybe others allows a tile to be
             * laid with an additional stop So we need to check that the current
             * station number is maintained or we have to check special cases..
             */

            if (rotation != null)
            {
                log.Debug("Valid rotation found " + rotation);
                stationMapping = rotation.StationMapping;
            }
            else
            {
                stationMapping = null;
                log.Error("No valid rotation was found: newRotation= " + newRotation
                          + "currentRotation" + currentTileRotation.Value);
            }

            BiDictionary<Stop, Station> stopsToNewStations = new BiDictionary<Stop, Station>();
            HashSet<Stop> droppedStops = new HashSet<Stop>();
            IEnumerable<Stop> unassignedStops;

            if (relaidTokens != null)
            {
                // Check for manual handling of tokens
                foreach (string compName in relaidTokens.Keys)
                {
                    PublicCompany company = GetRoot.CompanyManager.GetPublicCompany(compName);
                    foreach (Stop stop in stops)
                    {
                        if (stop.HasTokenOf(company))
                        {
                            Station newStation = newTile.GetStation(relaidTokens[compName]);
                            stopsToNewStations[stop] = newStation;
                            log.Debug("Mapped by relaid tokens: station "
                                      + stop.GetRelatedStation() + " to "
                                      + newStation);
                            break;
                        }
                    }
                }
                // Map all other stops in sequence to the remaining stations
                unassignedStops = stops.ViewValues().Except(stopsToNewStations.Keys);
                //unassignedStops = Sets.difference(stops.viewValues(),
                //            stopsToNewStations.keySet());

                foreach (Stop stop in unassignedStops)
                {
                    foreach (Station newStation in newTile.Stations)
                    {
                        if (!stopsToNewStations.ContainsValue(newStation))
                        {
                            stopsToNewStations[stop] = newStation;
                            log.Debug("Mapped after relaid tokens: station "
                                      + stop.GetRelatedStation() + " to "
                                      + newStation);
                            break;
                        }
                    }
                }
            }
            else
            { // default mapping routine

                foreach (Stop stop in stops)
                {
                    if (stopsToNewStations.ContainsKey(stop)) continue;
                    Station oldStation = stop.GetRelatedStation();
                    // Search stationMapping for assignments of stops to new
                    // stations
                    Station newStation = null;
                    string debugText = null;
                    if (stationMapping == null)
                    {
                        int oldNumber = stop.GetRelatedStation().Number;
                        newStation = newTile.GetStation(oldNumber);
                        debugText = "Mapped by default id";
                    }
                    else if (stationMapping.ContainsKey(oldStation))
                    {
                        // Match found in StationMapping, then assign the new
                        // station to the stop
                        newStation = stationMapping[oldStation];
                        debugText = "Mapped by stationMapping";
                    }
                    if (newStation == null)
                    { // no mapping => log error
                        droppedStops.Add(stop);
                        log.Debug(debugText + ": station " + oldStation + " is dropped");
                    }
                    else
                    {
                        if (stopsToNewStations.ContainsValue(newStation))
                        {
                            // new station already assigned a stop, use that
                            // and move tokens between stops
                            Stop otherStop = stopsToNewStations.Reverse[newStation];
                            MoveTokens(stop, otherStop);
                            droppedStops.Add(stop);
                            // FIXME: Due to Rails1.x compatibility
                            otherStop.AddPreviousNumbers(stop.GetLegacyNumber());
                        }
                        else
                        {
                            // otherwise use the existing stop
                            stopsToNewStations[stop] = newStation;
                        }
                        log.Debug(debugText + ": station " + oldStation + " to " + newStation);
                    }
                }
            }
            if ((stops.Count == 0) && (newTile.NumStations > 0))
            {

                foreach (Station newStation in newTile.Stations)
                {
                    Stop stop = Stop.Create(this, newStation);
                    stopsToNewStations[stop] = newStation;
                }
            }

            // Check for unassigned Stops
            //unassignedStops = Sets.difference(stops.viewValues(),
            //        Sets.union(stopsToNewStations.keySet(), droppedStops));
            unassignedStops = stops.ViewValues().Except(stopsToNewStations.Keys.Union(droppedStops));
            if (new List<Stop>(unassignedStops).Count > 0)
            {
                log.Error("Unassigned Stops :" + unassignedStops);
            }

            // Check for unassigned Stations
            List<Station> unassignedStations = new List<Station>(stopsToNewStations.Values.Except(newTile.Stations));
            if (unassignedStations.Count > 0)
            {
                log.Error("Unassigned Stations :" + unassignedStations);
            }
            ExecuteTileLay(newTile, newRotation, stopsToNewStations);

        }

        private void MoveTokens(Stop origin, Stop target)
        {
            foreach (BaseToken token in origin.GetBaseTokens())
            {
                PublicCompany company = token.Parent;
                if (target.HasTokenOf(company))
                {
                    // No duplicate tokens allowed in one city, so move to free
                    // tokens
                    token.MoveTo(company);
                    log.Debug("Duplicate token " + token.UniqueId
                              + " moved from " + origin.GetSpecificId() + " to "
                              + company.Id);
                    ReportBuffer.Add(this, LocalText.GetText(
                            "DuplicateTokenRemoved", company.Id, Id));
                }
                else
                {
                    token.MoveTo(target);
                    log.Debug("Token " + token.UniqueId + " moved from "
                              + origin.GetSpecificId() + " to "
                              + target.GetSpecificId());
                }
            }
        }

        /**
         * Execute a tile replacement. This method should only be called from
         * TileMove objects. It is also used to undo tile lays.
         *
         * @param newTile The new tile to be laid on this hex.
         * @param newOrientation The orientation of the new tile (0-5).
         * @param newStops The new stops used now
         */
        private void ExecuteTileLay(Tile newTile, HexSide newOrientation,
                BiDictionary<Stop, Station> newStops)
        {

            // TODO: Is the check for null still required?
            if (currentTile.Value != null)
            {
                currentTile.Value.Remove(this);
            }

            log.Debug("On hex " + Id + " replacing tile "
                      + currentTile.Value.Id + "/" + currentTileRotation
                      + " by " + newTile.Id + "/" + newOrientation);

            newTile.Add(this);
            currentTile.Set(newTile);
            currentTileRotation.Set(newOrientation);

            stops.Clear();
            if (newStops != null)
            {
                foreach (Stop stop in newStops.Keys)
                {
                    Station station = newStops[stop];
                    stops.Put(station, stop);
                    stop.SetRelatedStation(station);
                    log.Debug("Tile #" + newTile.Id + " station "
                              + station.Number + " has tracks to "
                              + GetConnectionString(station));
                }
            }
        }

        public bool LayBaseToken(PublicCompany company, Stop stop)
        {
            if (stops.Count == 0)
            {
                log.Error("Tile " + Id
                          + " has no station for home token of company "
                          + company.Id);
                return false;
            }

            BaseToken token = company.GetNextBaseToken();
            if (token == null)
            {
                log.Error("Company " + company.Id + " has no free token");
                return false;
            }
            else
            {
                // transfer token
                token.MoveTo(stop);
                if (IsHomeFor(company)
                    && isBlockedForTokenLays.Value == BlockedToken.ALWAYS)
                {
                    // FIXME: Assume that there is only one home base on such a
                    // tile,
                    // so we don't need to check for other ones
                    // Solution is to check for the number of home tokens still to
                    // lay
                    isBlockedForTokenLays.Set(BlockedToken.NEVER);
                }

                return true;
            }
        }

        /**
         * Lay a bonus token.
         * 
         * @param token The bonus token object to place
         * @param phaseManager The PhaseManager is also passed in case the token
         * must register itself for removal when a certain phase starts.
         * @return
         */
        public bool LayBonusToken(BonusToken token, PhaseManager phaseManager)
        {
            Precondition.CheckArgument(token != null, "No token specified");
            bonusTokens.Add(token);
            token.PrepareForRemoval(phaseManager);
            return true;
        }

        public List<BaseToken> GetBaseTokens()
        {
            List<BaseToken> tokens = new List<BaseToken>();
            foreach (Stop stop in stops)
            {
                tokens.AddRange(stop.GetBaseTokens());
            }
            return tokens;
        }

        public PortfolioSet<BonusToken> BonusTokens
        {
            get
            {
                return bonusTokens;
            }
        }

        public bool HasTokenSlotsLeft(Station station)
        {
            // FIXME: Is this still required
            // if (station == 0) station = 1; // Temp. fix for old save files
            return stops.Get(station).HasTokenSlotsLeft;
        }

        public bool HasTokenSlotsLeft()
        {
            foreach (Stop stop in stops)
            {
                if (stop.HasTokenSlotsLeft) return true;
            }
            return false;
        }

        /** Check if the hex has already a token of the company in any station */
        public bool HasTokenOfCompany(PublicCompany company)
        {
            return (GetStopOfBaseToken(company) != null);
        }

        /**
         * Return the stop that contains the base token of a company If no token in
         * the hex, returns null
         */
        public Stop GetStopOfBaseToken(PublicCompany company)
        {
            foreach (Stop stop in stops)
            {
                if (stop.HasTokenOf(company)) return stop;
            }
            return null;
        }

        public IReadOnlyCollection<Stop> Stops
        {
            get
            {
                return stops.ViewValues();
            }
        }

        public List<Stop> GetTokenableStops(PublicCompany company)
        {
            List<Stop> tokenableStops = new List<Stop>();
            foreach (Stop stop in stops)
            {
                if (stop.IsTokenableFor(company))
                {
                    tokenableStops.Add(stop);
                }
            }
            return tokenableStops;
        }

        public Stop GetRelatedStop(Station station)
        {
            return stops.Get(station);
        }

        // FIXME: Due to Rails1.x compatibility use legacy number or previous
        // numbers
        public Stop GetRelatedStop(int stationNb)
        {
            foreach (Stop stop in stops)
            {
                if (stop.GetLegacyNumber() == stationNb) return stop;
            }
            foreach (Stop stop in stops)
            {
                if (stop.CheckPreviousNumbers(stationNb)) return stop;
            }
            //return null;
            return stops.Get(GetStation(stationNb));
        }

        public IReadOnlyCollection<Station> getStations()
        {
            return stops.ViewKeys();
        }

        public Station GetStation(int stationNb)
        {
            return currentTile.Value.GetStation(stationNb);
        }

        public void AddHome(PublicCompany company, Stop home)
        {
            if (stops.Count == 0)
            {
                log.Error("No cities for home station on hex " + Id);
            }
            else
            {
                // not yet decided => create a null stop
                if (home == null)
                {
                    homes.Put(company, Stop.Create(this, null));
                    log.Debug("Added home of " + company + " in hex "
                              + this.ToString() + " city not yet decided");
                }
                else
                {
                    homes.Put(company, home);
                    log.Debug("Added home of " + company + " set to " + home
                              + " id= " + home.GetSpecificId());
                }
            }
        }

        public IReadOnlyDictionary<PublicCompany, Stop> GetHomes()
        {
            return homes.View();
        }

        public bool IsHomeFor(PublicCompany company)
        {
            return homes.ContainsKey(company);
        }

        public void AddDestination(PublicCompany company)
        {
            if (destinations == null) destinations = new List<PublicCompany>();
            destinations.Add(company);
        }

        public List<PublicCompany> Destinations
        {
            get
            {
                return destinations;
            }
        }

        /**
         * @return true if the hex is blocked by private company
         */
        public bool IsBlockedByPrivateCompany
        {
            get
            {
                return blockingPrivateCompany.Value != null;
            }
        }

        /**
         * @return blocking private company
         */
        public PrivateCompany GetBlockingPrivateCompany()
        {
            return blockingPrivateCompany.Value;
        }

        /**
         * @param private company that blocks the hex (use argument null to unblock)
         */
        public void SetBlockingPrivateCompany(PrivateCompany company)
        {
            blockingPrivateCompany.Set(company);
        }

        /**
         * @return Returns false if no base tokens may yet be laid on this hex and
         * station.
         *
         * NOTE: this method currently only checks for prohibitions caused by the
         * presence of unlaid home base tokens. It does NOT (yet) check for free
         * space.
         *
         *
         * There are the following cases to check for each company located there
         *
         * A) City is decided or there is only one city => check if the city has a
         * free slot or not (examples: NYNH in 1830 for a two city tile, NYC for a
         * one city tile) B) City is not decided (example: Erie in 1830) two
         * subcases depending on isHomeBlockedForAllCities - (true): all cities of
         * the hex have remaining slots available - (false): no city of the hex has
         * remaining slots available C) Or the company does not block its home city
         * at all (example:Pr in 1835) then isBlockedForTokenLays attribute is used
         *
         * NOTE: It now deals with more than one company with a home base on the
         * same hex.
         * 
         * Remark: This was a static field in Rails1.x causing potential undo
         * problems.
         * 
         */
        public bool IsBlockedForTokenLays(PublicCompany company, Stop stopToLay)
        {
            if (IsHomeFor(company))
            {
                // Company can always lay a home base
                return false;
            }

            switch (isBlockedForTokenLays.Value)
            {
                case BlockedToken.ALWAYS:
                    return true;
                case BlockedToken.NEVER:
                    return false;
                case BlockedToken.RESERVE_SLOT:
                    return IsBlockedForReservedHomes(stopToLay);
            }

            return false;
        }

        public bool IsBlockedForReservedHomes(Stop stopToLay)
        {
            // if no slots are reserved or home is empty
            if (isBlockedForTokenLays.Value != BlockedToken.RESERVE_SLOT
                || (homes.Count == 0))
            {
                return false;
            }

            // check if the city is potential home for other companies
            int anyBlockCompanies = 0;
            int cityBlockCompanies = 0;
            foreach (PublicCompany comp in homes.ViewKeys())
            {
                if (comp.HasLaidHomeBaseTokens || comp.IsClosed()) continue;
                // home base not laid yet
                Stop homeStop = homes.Get(comp);
                if (homeStop == null)
                {
                    anyBlockCompanies++; // undecided companies that block any
                                         // cities
                }
                else if (stopToLay == homeStop)
                {
                    cityBlockCompanies++; // companies which are located in the city
                                          // in question
                }
                else
                {
                    anyBlockCompanies++; // companies which are located somewhere
                                         // else
                }
            }
            log.Debug("IsBlockedForTokenLays: anyBlockCompanies = "
                      + anyBlockCompanies + " , cityBlockCompanies = "
                      + cityBlockCompanies);

            // check if there are sufficient individual city slots
            if (cityBlockCompanies + 1 > stopToLay.TokenSlotsLeft)
            {
                return true; // the additional token exceeds the number of available
                             // slots
            }

            // check if the overall hex slots are sufficient
            int allTokenSlotsLeft = 0;
            foreach (Stop stop in stops)
            {
                allTokenSlotsLeft += stop.TokenSlotsLeft;
            }
            if (anyBlockCompanies + cityBlockCompanies + 1 > allTokenSlotsLeft)
            {
                return true; // all located companies plus the additional token
                             // exceeds the available slots
            }
            return false;
        }

        public BlockedToken GetBlockedForTokenLays()
        {
            return isBlockedForTokenLays.Value;
        }

        public bool HasValuesPerPhase
        {
            get
            {
                return valuesPerPhase.Count > 0;
            }
        }

        // FIXME: Replace by Map to Phases
        public List<int> GetValuesPerPhase()
        {
            return valuesPerPhase;
        }

        public int GetCurrentValueForPhase(Phase phase)
        {
            if (HasValuesPerPhase && phase != null)
            {
                return valuesPerPhase[Math.Min(valuesPerPhase.Count, phase.OffBoardRevenueStep) - 1];
            }
            else
            {
                return 0;
            }
        }

        public string StopName
        {
            get
            {
                return stopName;
            }
        }

        public PublicCompany ReservedForCompany
        {
            get
            {
                return reservedForCompany;
            }
        }

        public bool IsReservedForCompany
        {
            get
            {
                return reservedForCompany != null;
            }
        }

        public List<RevenueBonusTemplate> RevenueBonuses
        {
            get
            {
                return revenueBonuses;
            }
        }

        public string GetConnectionString(Station station)
        {
            return TrackConfig.GetConnectionString(this, currentTile.Value,
                    currentTileRotation.Value, station);
        }

        override public string ToText()
        {
            if (!string.IsNullOrEmpty(stopName))
            {
                return Id + " " + stopName;
            }
            else
            {
                return Id;
            }
        }

        override public string ToString()
        {
            return base.ToString() + coordinates;
        }
    }
}
