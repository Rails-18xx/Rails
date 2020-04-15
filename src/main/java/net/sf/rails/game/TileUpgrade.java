package net.sf.rails.game;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.google.common.collect.Sets.SetView;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TileUpgrade implements Upgrade {

    private static final Logger log = LoggerFactory.getLogger(TileUpgrade.class);

    /**
     * Rotation defines the following details for a tile upgrade
     */
    public static class Rotation {
        private final HexSidesSet connectedSides;
        private final HexSidesSet sidesWithNewTrack;
        private final HexSide rotation;
        private final Map<Station, Station> stationMapping;
        private final Set<Station> stationsWithNewTrack;
        private final boolean symmetric;

        private Rotation(Set<Track> connectedTracks, Set<Track> newTracks, HexSide rotation,
                         Map<Station, Station> mapping, boolean symmetric) {

            this.rotation = rotation;
            this.stationMapping = mapping;
            this.symmetric = symmetric;

            HexSidesSet.Builder sidesBuilder = HexSidesSet.builder();
            for (Track t : connectedTracks) {
                if (t.getStart().getTrackPointType() == TrackPoint.Type.SIDE) {
                    sidesBuilder.set((HexSide) t.getStart());
                }
                if (t.getEnd().getTrackPointType() == TrackPoint.Type.SIDE) {
                    sidesBuilder.set((HexSide) t.getEnd());
                }
            }
            connectedSides = sidesBuilder.build();

            sidesBuilder = HexSidesSet.builder();
            ImmutableSet.Builder<Station> stationBuilder = ImmutableSet.builder();
            for (Track t : newTracks) {
                if (t.getStart().getTrackPointType() == TrackPoint.Type.SIDE) {
                    sidesBuilder.set((HexSide) t.getStart());
                } else {
                    stationBuilder.add((Station) t.getStart());
                }
                if (t.getEnd().getTrackPointType() == TrackPoint.Type.SIDE) {
                    sidesBuilder.set((HexSide) t.getEnd());
                } else {
                    stationBuilder.add((Station) t.getEnd());
                }
            }
            stationsWithNewTrack = stationBuilder.build();

            // Special condition for restrictive tile lays:
            // If a station with new track has more slots then the replaced station of the base tile
            // then all sides connecting to the station of the base tile are considered as
            // sides with new track as well
            if (stationMapping != null && !stationsWithNewTrack.isEmpty()) {
                for (Track t : connectedTracks) {
                    if (t.getStart().getTrackPointType() == TrackPoint.Type.STATION
                            && t.getEnd().getTrackPointType() == TrackPoint.Type.SIDE) {
                        Station start = (Station) t.getStart();
                        if (stationsWithNewTrack.contains(start)
                                && stationMapping.containsKey(start)
                                && start.getBaseSlots()
                                < stationMapping.get(start).getBaseSlots()) {
                            sidesBuilder.set((HexSide) t.getEnd());
                        }
                    }
                }
            }
            sidesWithNewTrack = sidesBuilder.build();
        }

        public HexSidesSet getConnectedSides() {
            return connectedSides;
        }

        public HexSidesSet getSidesWithNewTrack() {
            return sidesWithNewTrack;
        }

        public Map<Station, Station> getStationMapping() {
            return stationMapping;
        }

        public Set<Station> getStationsWithNewTrack() {
            return stationsWithNewTrack;
        }

        public boolean isSymmetric() {
            return symmetric;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("rotation = ").append(rotation).append(", connectedSides = ").
                    append(connectedSides.toString()).append(", sidesWithNewTrack = ").append(sidesWithNewTrack.toString()).
                    append(", stationMapping = ").append(stationMapping).append(", stationsWithNewTrack = ").append(stationsWithNewTrack).toString();
        }
    }

    /**
     * Tile to upgrade
     */
    private final Tile baseTile;

    /**
     * The upgrade tile number
     */
    private final String targetTileId;

    /**
     * Temporary Strings to exclude hexes and phases. This will be
     * processed at finishConfiguration.
     */
    private final String hexes;
    private final String phases;

    /**
     * The upgrade tile
     */
    private Tile targetTile;

    /**
     * Possible rotations given the trackConfiguration
     */
    private Map<HexSide, Rotation> rotations;
    private HexSidesSet rotationSides;

    /**
     * Hexes where the upgrade can be executed
     */
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


    private TileUpgrade(Tile baseTile, String targetId, String hexes, String phases) {
        this.baseTile = baseTile;
        this.targetTileId = targetId;
        this.hexes = hexes;
        this.phases = phases;
    }

    public static List<TileUpgrade> createFromTags(Tile tile, List<Tag> upgradeTags)
            throws ConfigurationException {

        ImmutableList.Builder<TileUpgrade> allUpgrades = ImmutableList.builder();
        for (Tag upgradeTag : upgradeTags) {
            String ids = upgradeTag.getAttributeAsString("id");
            if (ids == null) continue;

            String hexes = upgradeTag.getAttributeAsString("hex");
            String phases = upgradeTag.getAttributeAsString("phase");

            for (String sid : ids.split(",")) {
                try {
                    TileUpgrade upgrade = new TileUpgrade(tile, sid, hexes, phases);
                    allUpgrades.add(upgrade);
                } catch (NumberFormatException e) {
                    log.error("Catched Exception", e);
                    throw new ConfigurationException(LocalText.getText(
                            "InvalidUpgrade", tile.toText(), sid));
                }
            }
        }
        return allUpgrades.build();
    }

    public static TileUpgrade createSpecific(Tile current, Tile specific) {
        TileUpgrade upgrade = new TileUpgrade(current, specific.getId(), null, null);
        try {
            upgrade.finishConfiguration(current.getRoot());
        } catch (ConfigurationException e) {
            log.error(LocalText.getText("InvalidUpgrade",
                    current.toText(), specific.toText()), e);
        }
        return upgrade;
    }

    public void finishConfiguration(RailsRoot root) throws ConfigurationException {
        targetTile = root.getTileManager().getTile(targetTileId);
        if (targetTile == null) {
            throw new ConfigurationException(LocalText.getText("InvalidUpgrade",
                    baseTile.toText(), targetTileId));
        }
        initRotations();
        parsePhases(root);
        parseHexes(root);
    }

    public Tile getTargetTile() {
        return targetTile;
    }

    public String getTileId() {
        return targetTileId;
    }

    public boolean isAllowedForHex(MapHex hex) {
        if (allowedHexes != null) {
            return allowedHexes.contains(hex);
        } else if (disallowedHexes != null) {
            return !disallowedHexes.contains(hex);
        } else {
            return true;
        }
    }

    public boolean isAllowedForPhase(Phase phase) {
        if (allowedPhases != null
                && !allowedPhases.contains(phase)) {
            return false;
        } else {
            return true;
        }
    }

    private void initRotations() {
        HexSidesSet.Builder sideBuilder = HexSidesSet.builder();
        ImmutableMap.Builder<HexSide, Rotation> rotationBuilder = ImmutableMap.builder();
        for (HexSide side : HexSide.all()) {
            Rotation rotation = processRotations(side);
            if (rotation != null) {
                sideBuilder.set(side);
                rotationBuilder.put(side, rotation);
            }
        }
        rotationSides = sideBuilder.build();
        rotations = rotationBuilder.build();
    }

    private void parsePhases(RailsRoot root) throws ConfigurationException {
        if (phases == null) return;

        ImmutableList.Builder<Phase> phaseBuilder = ImmutableList.builder();
        for (String phaseName : phases.split(",")) {
            Phase phase = root.getPhaseManager().getPhaseByName(phaseName);
            if (phase == null) {
                throw new ConfigurationException(LocalText.getText(
                        "IllegalPhaseDefinition",
                        this.toString()
                ));
            } else {
                phaseBuilder.add(phase);
            }
        }
        allowedPhases = phaseBuilder.build();
    }

    private List<MapHex> parseHexString(RailsRoot root, String sHexes)
            throws ConfigurationException {
        ImmutableList.Builder<MapHex> hexBuilder = ImmutableList.builder();

        for (String sHex : sHexes.split(",")) {
            MapHex hex = root.getMapManager().getHex(sHex);
            if (hex == null) {
                throw new ConfigurationException(LocalText.getText("InvalidUpgrade",
                        baseTile.toText(), sHexes));
            } else {
                hexBuilder.add(hex);
            }
        }
        return hexBuilder.build();
    }

    private void parseHexes(RailsRoot root) throws ConfigurationException {
        if (hexes == null) return;

        boolean allowed = !hexes.startsWith("-");
        if (allowed) {
            allowedHexes = parseHexString(root, hexes);
        } else {
            disallowedHexes = parseHexString(root, hexes.substring(1));
        }
    }


    private boolean checkInvalidSides(Rotation rotation, HexSidesSet impassable) {
        log.trace("Check rotation {} against  impassable{}", rotation, impassable);
        if (impassable == null) return false; // null implies that no station exists
        return (rotation.getConnectedSides().intersects(impassable));
    }

    private boolean checkSideConnectivity(Rotation rotation, HexSidesSet connected, boolean restrictive) {
        log.trace("Check rotation {} against {}", rotation, connected);
        if (connected == null) return true; // null implies no connectivity required
        if (restrictive && !rotation.getSidesWithNewTrack().isEmpty()) {
            return rotation.getSidesWithNewTrack().intersects(connected);
        } else {
            return (rotation.getConnectedSides().intersects(connected));
        }
    }

    private boolean checkStationConnectivity(Rotation rotation, Collection<Station> stations) {
        if (rotation.getStationMapping() == null) return false;
        log.trace("Check Stations {}, rotation = {}", stations, rotation);
        for (Station station : stations) {
            Station targetStation = rotation.getStationMapping().get(station);
            if (targetStation != null && rotation.getStationsWithNewTrack().contains(targetStation)) {
                return true;
            }
        }
        return false;
    }

    public HexSidesSet getRotationSet() {
        return rotationSides;
    }

    public Rotation getRotation(HexSide rotation) {
        return rotations.get(rotation);
    }

    public HexSidesSet getAllowedRotations(HexSidesSet connected, HexSidesSet impassable, HexSide baseRotation,
                                           Collection<Station> stations, boolean restrictive) {

        HexSidesSet.Builder builder = HexSidesSet.builder();
        for (HexSide side : rotationSides) {
            Rotation rotation = rotations.get(side);
            if (checkInvalidSides(rotation, impassable)) continue;
            if (checkSideConnectivity(rotation, connected, restrictive) ||
                    checkStationConnectivity(rotation, stations)) {
                builder.set(side.rotate(baseRotation));
            }
        }
        HexSidesSet allowed = builder.build();
        log.trace("allowed = {}hexSides = {}impassable ={} rotationSides = {}", allowed, connected, impassable, rotationSides);
        return allowed;
    }

    private Rotation processRotations(HexSide side) {

        TrackConfig base = baseTile.getTrackConfig();
        TrackConfig target = targetTile.getTrackConfig();
        // create rotation of target, unless default (= 0) rotation
        if (side != HexSide.get(0)) {
            target = TrackConfig.createByRotation(target, side);
        }
        // check if there are stations to map
        Map<Station, Station> stationMapping = assignStations(base, target);
        if (stationMapping != null && !stationMapping.isEmpty()) {
            if (stationMapping.containsValue(null)) {
                base = TrackConfig.createByDowngrade(base, base.getTile().getStation(1));
            }
            base = TrackConfig.createByStationMapping(base, stationMapping);
        }

        // and finally check if all tracks are maintained
        Set<Track> baseTracks = base.getTracks();
        Set<Track> targetTracks = target.getTracks();
        SetView<Track> diffTrack = Sets.difference(baseTracks, targetTracks);
        if (diffTrack.isEmpty()) {
            SetView<Track> newTracks = Sets.difference(targetTracks, baseTracks);
            boolean allowed = (targetTile.getPossibleRotations().get(side));
            Rotation rotObject = new Rotation(targetTracks, newTracks, side, stationMapping, allowed);
            log.trace("New Rotation for {} => {}: \n{}", baseTile, targetTile, rotObject);
            return rotObject;
        } else {
            log.trace("No Rotation found {} => {}, rotation ={}, remaining Tracks = {}", baseTile, targetTile, side, diffTrack);
            return null;
        }
    }

    private Map<Station, Station> assignStations(TrackConfig base, TrackConfig target) {
        int baseNb = base.getTile().getNumStations();
        if (baseNb == 0) return null;

        int targetNb = target.getTile().getNumStations();
        Map<Station, Station> stationMap = Maps.newHashMapWithExpectedSize(baseNb);
        if (baseNb == 1) {
            // only one station in base => baseStation
            Station baseStation = base.getTile().getStation(1);
            if (targetNb == 1) {
                // only one station in target => targetStation
                Station targetStation = target.getTile().getStation(1);
                // default case: 1 => 1 mapping
                stationMap.put(baseStation, targetStation);
            } else if (targetNb == 0) {
                // special case: downgrade in 1856 and there is only one station to consider
                Set<TrackPoint> baseTrack = base.getStationTracks(baseStation);
                for (TrackPoint side : baseTrack) {
                    Set<TrackPoint> targetTrack = target.getSideTracks((HexSide) side);
                    targetTrack.add(side); // connectivity with all other sides
                    SetView<TrackPoint> diffTrack = Sets.difference(baseTrack, targetTrack);
                    if (!diffTrack.isEmpty()) return null;
                }
                stationMap.put(baseStation, null);
            }
        } else { // more than one base station, assign by side connectivity
            List<Station> noTrackBaseStations = Lists.newArrayList();
            TreeSet<Station> targetStations = Sets.newTreeSet(target.getTile().getStations());
            for (Station b : base.getTile().getStations()) {
                Set<TrackPoint> baseTrack = base.getStationTracks(b);
                if (baseTrack.isEmpty()) { // if track is empty, keep track to add target later
                    noTrackBaseStations.add(b);
                } else {
                    for (Station t : target.getTile().getStations()) {
                        Set<TrackPoint> targetTrack = target.getStationTracks(t);
                        if (checkTrackConnectivity(baseTrack, targetTrack)) {
                            stationMap.put(b, t);
                            targetStations.remove(t);
                            break;
                        }
                    }
                }
            }
            // any base Stations remaining
            for (Station b : noTrackBaseStations) {
                Station t = targetStations.pollFirst();
                if (t != null) {
                    stationMap.put(b, t);
                }
            }
            // check if all base and target stations are assigned
            if (stationMap.keySet().size() != baseNb ||
                    Sets.newHashSet(stationMap.values()).size() != targetNb) {
                stationMap = null;
                log.debug("Mapping: Not all stations assigned, set stationMap to null");
            }
        }
        return stationMap;
    }

    private boolean checkTrackConnectivity(Set<TrackPoint> baseTrack, Set<TrackPoint> targetTrack) {
        SetView<TrackPoint> diffTrack = Sets.difference(baseTrack, targetTrack);
        if (diffTrack.isEmpty()) {
            // target maintains connectivity
            return true;
        } else {
            // if not all connections are maintained,
            Predicate<TrackPoint> checkForStation = new Predicate<TrackPoint>() {
                public boolean apply(TrackPoint p) {
                    return (p.getTrackPointType() == TrackPoint.Type.SIDE);
                }
            };
            // check if remaining tracks only lead to other stations
            if (Sets.filter(diffTrack, checkForStation).isEmpty()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("base", baseTile)
                .add("targetTile", targetTile)
                .toString();
    }
}
