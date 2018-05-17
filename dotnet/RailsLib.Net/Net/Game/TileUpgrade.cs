using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace GameLib.Net.Game
{
    public class TileUpgrade : IUpgrade
    {
        private static Logger<TileUpgrade> log = new Logger<TileUpgrade>();

        /**
         * Rotation defines the following details for a tile upgrade
         */
        public class Rotation
        {
            private HexSidesSet connectedSides;
            private HexSidesSet sidesWithNewTrack;
            private HexSide rotation;
            private Dictionary<Station, Station> stationMapping;
            private List<Station> stationsWithNewTrack;
            private bool symmetric;

            public Rotation(List<Track> connectedTracks, List<Track> newTracks, HexSide rotation,
                    Dictionary<Station, Station> mapping, bool symmetric)
            {

                this.rotation = rotation;
                this.stationMapping = mapping;
                this.symmetric = symmetric;

                HexSidesSet.Builder sidesBuilder = HexSidesSet.GetBuilder();
                foreach (Track t in connectedTracks)
                {
                    if (t.Start.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE)
                    {
                        sidesBuilder.Set((HexSide)t.Start);
                    }
                    if (t.End.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE)
                    {
                        sidesBuilder.Set((HexSide)t.End);
                    }
                }
                connectedSides = sidesBuilder.Build();

                sidesBuilder = HexSidesSet.GetBuilder();
                List<Station> stationBuilder = new List<Station>();
                foreach (Track t in newTracks)
                {
                    if (t.Start.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE)
                    {
                        sidesBuilder.Set((HexSide)t.Start);
                    }
                    else
                    {
                        stationBuilder.Add((Station)t.Start);
                    }
                    if (t.End.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE)
                    {
                        sidesBuilder.Set((HexSide)t.End);
                    }
                    else
                    {
                        stationBuilder.Add((Station)t.End);
                    }
                }
                stationsWithNewTrack = stationBuilder;

                // Special condition for restrictive tile lays:
                // If a station with new track has more slots then the replaced station of the base tile
                // then all sides connecting to the station of the base tile are considered as
                // sides with new track as well
                if (stationMapping != null && stationsWithNewTrack.Count > 0)
                {
                    foreach (Track t in connectedTracks)
                    {
                        if (t.Start.TrackPointType == TrackPoint.TrackPointTypeEnum.STATION
                                && t.End.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE)
                        {
                            Station start = (Station)t.Start;
                            if (stationsWithNewTrack.Contains(start)
                                    && stationMapping.ContainsKey(start)
                                    && start.BaseSlots
                                    < stationMapping[start].BaseSlots)
                            {
                                sidesBuilder.Set((HexSide)t.End);
                            }
                        }
                    }
                }
                sidesWithNewTrack = sidesBuilder.Build();
            }

            public HexSidesSet ConnectedSides
            {
                get
                {
                    return connectedSides;
                }
            }

            public HexSidesSet SidesWithNewTrack
            {
                get
                {
                    return sidesWithNewTrack;
                }
            }

            public Dictionary<Station, Station> StationMapping
            {
                get
                {
                    return stationMapping;
                }
            }

            public List<Station> StationsWithNewTrack
            {
                get
                {
                    return stationsWithNewTrack;
                }
            }

            public bool IsSymmetric
            {
                get
                {
                    return symmetric;
                }
            }

            override public string ToString()
            {
                return "rotation = " + rotation.ToString() + ", connectedSides = "
                        + connectedSides.ToString() + ", sidesWithNewTrack = " + sidesWithNewTrack.ToString()
                        + ", stationMapping = " + stationMapping + ", stationsWithNewTrack = " + stationsWithNewTrack;
            }
        }

        /** Tile to upgrade */
        private Tile baseTile;

        /** The upgrade tile number */
        private string targetTileId;

        /**
         * Temporary Strings to exclude hexes and phases. This will be
         * processed at finishConfiguration.
         */
        private string hexes;
        private string phases;

        /** The upgrade tile */
        private Tile targetTile;

        /** Possible rotations given the trackConfiguration */
        private Dictionary<HexSide, Rotation> rotations;
        private HexSidesSet rotationSides;

        /** Hexes where the upgrade can be executed */
        private List<MapHex> allowedHexes = null;

        /**
         * Hexes where the upgrade cannot be executed Only one of allowedHexes
         * and disallowedHexes should be used
         */
        private List<MapHex> disallowedHexes = null;

        /**
         * Phases in which the upgrade can be executed.
         */
        private List<Phase> allowedPhases = null;


        private TileUpgrade(Tile baseTile, string targetId, string hexes, string phases)
        {
            this.baseTile = baseTile;
            this.targetTileId = targetId;
            this.hexes = hexes;
            this.phases = phases;
        }

        public static List<TileUpgrade> CreateFromTags(Tile tile, List<Tag> upgradeTags)
        {

            List<TileUpgrade> allUpgrades = new List<TileUpgrade>();
            foreach (Tag upgradeTag in upgradeTags)
            {
                string ids = upgradeTag.GetAttributeAsString("id");
                if (ids == null) continue;

                string hexes = upgradeTag.GetAttributeAsString("hex");
                string phases = upgradeTag.GetAttributeAsString("phase");

                foreach (string sid in ids.Split(','))
                {
                    try
                    {
                        TileUpgrade upgrade = new TileUpgrade(tile, sid, hexes, phases);
                        allUpgrades.Add(upgrade);
                    }
                    catch (FormatException e)
                    {
                        log.Error("Caught Exception : " + e.Message);
                        throw new ConfigurationException(LocalText.GetText(
                                "InvalidUpgrade", tile.ToText(), sid));
                    }
                }
            }
            return allUpgrades;
        }

        public static TileUpgrade CreateSpecific(Tile current, Tile specific)
        {
            TileUpgrade upgrade = new TileUpgrade(current, specific.Id, null, null);
            try
            {
                upgrade.FinishConfiguration(current.GetRoot);
            }
            catch (ConfigurationException)
            {
                log.Error(LocalText.GetText("InvalidUpgrade",
                        current.ToText(), specific.ToText()));
            }
            return upgrade;
        }

        public void FinishConfiguration(RailsRoot root)
        {
            targetTile = root.TileManager.GetTile(targetTileId);
            if (targetTile == null)
            {
                throw new ConfigurationException(LocalText.GetText("InvalidUpgrade",
                        baseTile.ToText(), targetTileId));
            }
            InitRotations();
            ParsePhases(root);
            ParseHexes(root);
        }

        public Tile TargetTile
        {
            get
            {
                return targetTile;
            }
        }

        public string TileId
        {
            get
            {
                return targetTileId;
            }
        }

        public bool IsAllowedForHex(MapHex hex)
        {
            if (allowedHexes != null)
            {
                return allowedHexes.Contains(hex);
            }
            else if (disallowedHexes != null)
            {
                return !disallowedHexes.Contains(hex);
            }
            else
            {
                return true;
            }
        }

        public bool IsAllowedForPhase(Phase phase)
        {
            if (allowedPhases != null
                    && !allowedPhases.Contains(phase))
            {
                return false;
            }
            else
            {
                return true;
            }
        }

        private void InitRotations()
        {
            HexSidesSet.Builder sideBuilder = HexSidesSet.GetBuilder();
            Dictionary<HexSide, Rotation> rotationBuilder = new Dictionary<HexSide, Rotation>();
            foreach (HexSide side in HexSide.All())
            {
                Rotation rotation = ProcessRotations(side);
                if (rotation != null)
                {
                    sideBuilder.Set(side);
                    rotationBuilder[side] = rotation;
                }
            }
            rotationSides = sideBuilder.Build();
            rotations = rotationBuilder;
        }

        private void ParsePhases(RailsRoot root)
        {
            if (phases == null) return;

            List<Phase> phaseBuilder = new List<Phase>();
            foreach (string phaseName in phases.Split(','))
            {
                Phase phase = root.PhaseManager.GetPhaseByName(phaseName);
                if (phase == null)
                {
                    throw new ConfigurationException(LocalText.GetText("IllegalPhaseDefinition", this.ToString()));
                }
                else
                {
                    phaseBuilder.Add(phase);
                }
            }
            allowedPhases = phaseBuilder;
        }

        private List<MapHex> ParseHexString(RailsRoot root, string sHexes)
        {
            List<MapHex> hexBuilder = new List<MapHex>();

            foreach (string sHex in sHexes.Split(','))
            {
                MapHex hex = root.MapManager.GetHex(sHex);
                if (hex == null)
                {
                    throw new ConfigurationException(LocalText.GetText("InvalidUpgrade", baseTile.ToText(), sHexes));
                }
                else
                {
                    hexBuilder.Add(hex);
                }
            }
            return hexBuilder;
        }

        private void ParseHexes(RailsRoot root)
        {
            if (hexes == null) return;

            bool allowed = !hexes.StartsWith("-");
            if (allowed)
            {
                allowedHexes = ParseHexString(root, hexes);
            }
            else
            {
                disallowedHexes = ParseHexString(root, hexes.Substring(1));
            }
        }


        private bool CheckInvalidSides(Rotation rotation, HexSidesSet impassable)
        {
            log.Debug("Check rotation " + rotation + " against  impassable" + impassable);
            if (impassable == null) return false; // null implies that no station exists
            return (rotation.ConnectedSides.Intersects(impassable));
        }

        private bool CheckSideConnectivity(Rotation rotation, HexSidesSet connected, bool restrictive)
        {
            log.Debug("Check rotation " + rotation + " against " + connected);
            if (connected == null) return true; // null implies no connectivity required
            if (restrictive && !rotation.SidesWithNewTrack.IsEmpty)
            {
                return rotation.SidesWithNewTrack.Intersects(connected);
            }
            else
            {
                return (rotation.ConnectedSides.Intersects(connected));
            }
        }

        private bool CheckStationConnectivity(Rotation rotation, IEnumerable<Station> stations)
        {
            if (rotation.StationMapping == null) return false;
            log.Debug("Check Stations " + stations + ", rotation = " + rotation);
            foreach (Station station in stations)
            {
                Station targetStation;
                rotation.StationMapping.TryGetValue(station, out targetStation);
                if (targetStation != null && rotation.StationsWithNewTrack.Contains(targetStation))
                {
                    return true;
                }
            }
            return false;
        }

        public HexSidesSet RotationSet
        {
            get
            {
                return rotationSides;
            }
        }

        public Rotation GetRotation(HexSide rotation)
        {
            return rotations[rotation];
        }

        public HexSidesSet GetAllowedRotations(HexSidesSet connected, HexSidesSet impassable, HexSide baseRotation,
                IEnumerable<Station> stations, bool restrictive)
        {

            HexSidesSet.Builder builder = HexSidesSet.GetBuilder();
            foreach (HexSide side in rotationSides)
            {
                Rotation rotation = rotations[side];
                if (CheckInvalidSides(rotation, impassable)) continue;
                if (CheckSideConnectivity(rotation, connected, restrictive) ||
                        CheckStationConnectivity(rotation, stations))
                {
                    builder.Set((HexSide)side.Rotate(baseRotation));
                }
            }
            HexSidesSet allowed = builder.Build();
            log.Debug("allowed = " + allowed + "hexSides = " + connected + "impassable =" + impassable + " rotationSides = " + rotationSides);
            return allowed;
        }

        private Rotation ProcessRotations(HexSide side)
        {

            TrackConfig baseConfig = baseTile.TrackConfig;
            TrackConfig target = targetTile.TrackConfig;
            // create rotation of target, unless default (= 0) rotation
            if (side != HexSide.Get(0))
            {
                target = TrackConfig.CreateByRotation(target, side);
            }
            // check if there are stations to map
            Dictionary<Station, Station> stationMapping = AssignStations(baseConfig, target);
            if (stationMapping != null && stationMapping.Count > 0)
            {
                if (stationMapping.ContainsValue(null))
                {
                    baseConfig = TrackConfig.CreateByDowngrade(baseConfig, baseConfig.Tile.GetStation(1));
                }
                baseConfig = TrackConfig.CreateByStationMapping(baseConfig, stationMapping);
            }

            // and finally check if all tracks are maintained
            List<Track> baseTracks = baseConfig.Tracks;
            List<Track> targetTracks = target.Tracks;

            List<Track> diffTrack = new List<Track>(baseTracks.Except(targetTracks));
            if (diffTrack.Count == 0)
            {
                IEnumerable<Track> newTracks = targetTracks.Except(baseTracks);
                bool allowed = (targetTile.PossibleRotations.Get(side));
                Rotation rotObject = new Rotation(targetTracks, new List<Track>(newTracks), side, stationMapping, allowed);
                log.Debug("New Rotation for " + baseTile + " => " + targetTile + ": \n" + rotObject);
                return rotObject;
            }
            else
            {
                log.Debug("No Rotation found " + baseTile + " => " + targetTile + ", rotation =" + side +
                        ", remaining Tracks = " + diffTrack);
                return null;
            }
        }

        private Dictionary<Station, Station> AssignStations(TrackConfig baseConfig, TrackConfig target)
        {
            int baseNb = baseConfig.Tile.NumStations;
            if (baseNb == 0) return null;

            int targetNb = target.Tile.NumStations;
            Dictionary<Station, Station> stationMap = new Dictionary<Station, Station>(baseNb);//Maps.newHashMapWithExpectedSize(baseNb);
            if (baseNb == 1)
            {
                // only one station in base => baseStation
                Station baseStation = baseConfig.Tile.GetStation(1);
                if (targetNb == 1)
                {
                    // only one station in target => targetStation
                    Station targetStation = target.Tile.GetStation(1);
                    // default case: 1 => 1 mapping
                    stationMap[baseStation] = targetStation;
                }
                else if (targetNb == 0)
                {
                    // special case: downgrade in 1856 and there is only one station to consider
                    /*List<TrackPoint>*/var baseTrack = baseConfig.GetStationTracks(baseStation);
                    foreach (TrackPoint side in baseTrack)
                    {
                        List<TrackPoint> targetTrack = target.GetSideTracks((HexSide)side);
                        targetTrack.Add(side); // connectivity with all other sides
                        List<TrackPoint> diffTrack = new List<TrackPoint>(baseTrack.Except(targetTrack));
                        if (diffTrack.Count > 0) return null;
                    }
                    stationMap[baseStation] = null;
                }
            }
            else
            { // more than one base station, assign by side connectivity
                List<Station> noTrackBaseStations = new List<Station>();
                SortedSet<Station> targetStations = new SortedSet<Station>(target.Tile.Stations);
                foreach (Station b in baseConfig.Tile.Stations)
                {
                    /*List<TrackPoint>*/var baseTrack = baseConfig.GetStationTracks(b);
                    if (baseTrack.Count == 0)
                    { // if track is empty, keep track to add target later
                        noTrackBaseStations.Add(b);
                    }
                    else
                    {
                        foreach (Station t in target.Tile.Stations)
                        {
                            /*List<TrackPoint>*/var targetTrack = target.GetStationTracks(t);
                            if (CheckTrackConnectivity(baseTrack, targetTrack))
                            {
                                stationMap[b] = t;
                                targetStations.Remove(t);
                                break;
                            }
                        }
                    }
                }
                // any base Stations remaining
                foreach (Station b in noTrackBaseStations)
                {
                    Station t = targetStations.Min;
                    if (t != null)
                    {
                        targetStations.Remove(t);
                        stationMap[b] = t;
                    }
                }
                // check if all base and target stations are assigned
                if (stationMap.Keys.Count != baseNb ||
                        stationMap.Values.Count != targetNb)
                {
                    stationMap = null;
                    log.Debug("Mapping: Not all stations assigned, set stationMap to null");
                }
            }
            return stationMap;
        }

        private bool CheckTrackConnectivity(IEnumerable<TrackPoint> baseTrack, IEnumerable<TrackPoint> targetTrack)
        {
            List<TrackPoint> diffTrack = new List<TrackPoint>(baseTrack.Except(targetTrack));
            if (diffTrack.Count == 0)
            {
                // target maintains connectivity
                return true;
            }
            else
            {
                //    // if not all connections are maintained, 
                //    Predicate<TrackPoint> checkForStation = new Predicate<TrackPoint>()
                //    {
                //            public bool apply(TrackPoint p)
                //    {
                //        return (p.getTrackPointType() == TrackPoint.Type.SIDE);
                //    }
                //};
                // check if remaining tracks only lead to other stations 
                //if (Sets.filter(diffTrack, checkForStation).isEmpty())
                //{
                //    return true;
                //}
                if (!diffTrack.Any(p => p.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE))
                {
                    return true;
                }
            }
            return false;
        }

        override public string ToString()
        {
            //return Objects.toStringHelper(this).add("base", baseTile).add("targetTile", targetTile).toString();
            return $"{this.GetType().Name}{{base={baseTile.ToString()}}}{{targetTile={targetTile.ToString()}}}";
        }
    }
}
