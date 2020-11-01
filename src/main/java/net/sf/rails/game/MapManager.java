package net.sf.rails.game;

import java.util.*;
import java.util.Map.Entry;

import net.sf.rails.common.Config;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * MapManager configures the map layout from XML
 */
public class MapManager extends RailsManager implements Configurable {

    private MapOrientation mapOrientation;

    private ImmutableMap<MapHex.Coordinates, MapHex> hexes;
    private ImmutableTable<MapHex, HexSide, MapHex> hexTable;

    private MapHex.Coordinates minimum;
    private MapHex.Coordinates maximum;

    // upgrade costs on the map for noMapMode
    private ImmutableSortedSet<Integer> possibleTileCosts;

    // Stop property defaults per stop type
    private EnumMap<Stop.Type, Access> defaultAccessTypes = new EnumMap<>(Stop.Type.class);
    
    // if required: distance table
    private Table<MapHex, MapHex, Integer> hexDistances;

    // Optional map image (SVG file)
    // FIXME: Move to UI class
    private String mapImageFilename = null;
    private String mapImageFilepath = null;
    private int mapXOffset = 0;
    private int mapYOffset = 0;
    private float mapScale = (float)1.0;
    private boolean mapImageUsed = false;

    private static final Logger log = LoggerFactory.getLogger(MapManager.class);

    /**
     * Used by Configure (via reflection) only
     */
    public MapManager(RailsRoot parent, String id) {
        super(parent, id);
    }
    
    /*
     * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        mapOrientation = MapOrientation.create(tag);

        List<Tag> hexTags = tag.getChildren("Hex");
        ImmutableMap.Builder<MapHex.Coordinates, MapHex> hexBuilder = ImmutableMap.builder();
        ImmutableSortedSet.Builder<Integer> tileCostsBuilder= ImmutableSortedSet.naturalOrder();

        for (Tag hexTag : hexTags) {
            MapHex hex = MapHex.create(this, hexTag);
            hexBuilder.put(hex.getCoordinates(), hex);
            tileCostsBuilder.addAll(hex.getTileCostsList());
        }
        hexes = hexBuilder.build();
        possibleTileCosts = tileCostsBuilder.build();
        
        minimum = MapHex.Coordinates.minimum(hexes.values());
        maximum = MapHex.Coordinates.maximum(hexes.values());
        
        // Default Stop Types
        Tag defaultsTag = tag.getChild("Defaults");
        if (defaultsTag != null) {
            List<Tag> accessTags = defaultsTag.getChildren("Access");
            defaultAccessTypes = Access.parseDefaults(this, accessTags);
        }

        // Map image attributes
        // FIXME: Move to an UI class
        Tag mapImageTag = tag.getChild("Image");
        if (mapImageTag != null) {
            mapImageFilename = mapImageTag.getAttributeAsString("file");
            mapXOffset = mapImageTag.getAttributeAsInteger("x", mapXOffset);
            mapYOffset = mapImageTag.getAttributeAsInteger("y", mapYOffset);
            mapScale = mapImageTag.getAttributeAsFloat("scale", mapScale);
        }
    }

    public void finishConfiguration (RailsRoot root) throws ConfigurationException {

        for (MapHex hex:hexes.values()) {
            hex.finishConfiguration(root);
        }

        // Initialise the neighbours
        ImmutableTable.Builder<MapHex, HexSide, MapHex> hexTableBuilder = ImmutableTable.builder();
        for (MapHex hex:hexes.values()) {
           for (HexSide side:HexSide.all()){
                MapHex neighbour = hexes.get(mapOrientation.
                        getAdjacentCoordinates(hex.getCoordinates(), side));
                if (neighbour != null) {
                    if (hex.isValidNeighbour(neighbour, side)) {
                        hexTableBuilder.put(hex, side, neighbour);
                        if (hex.isRiverNeighbour(neighbour)) {
                            hex.addRiverSide(side);
                            neighbour.addRiverSide(side.opposite());
                        }
                    } else {
                        hex.addInvalidSide(side);
                        if (hex.isImpassableNeighbour(neighbour)) {
                            hex.addImpassableSide(side);
                            neighbour.addImpassableSide(side.opposite());
                        }
                    }
                } else { // neighbour is null
                    hex.addInvalidSide(side);
                }
            }
        }
        hexTable = hexTableBuilder.build();

        for (PublicCompany company : root.getCompanyManager().getAllPublicCompanies()) {
            List<MapHex> homeHexes = company.getHomeHexes();
            if (homeHexes != null) {
                for (MapHex homeHex : homeHexes) {
                   int homeNumber = company.getHomeCityNumber();
                   Stop home = homeHex.getRelatedStop(homeNumber);
                   if (home == null && homeNumber != 0) {
                       throw new ConfigurationException ("Invalid home number "+homeNumber+" for hex "+homeHex
                               +" which has "+homeHex.getStops().size()+" stop");
                   } else {
                       homeHex.addHome(company, home);
                   }
                }
            }
            MapHex hex = company.getDestinationHex();
            if (hex != null) {
                hex.addDestination(company);
            }
        }

        // FIXME: Move this configuration to an UI class
        mapImageUsed = net.sf.rails.util.Util.hasValue(mapImageFilename)
        && "yes".equalsIgnoreCase(Config.get("map.image.display"));
        if (mapImageUsed) {
            String rootDirectory = Config.get("map.root_directory");
            if (!net.sf.rails.util.Util.hasValue(rootDirectory)) {
                rootDirectory = "data";
            }
            mapImageFilepath = "/" + rootDirectory + "/" + mapImageFilename;
        }

    }

    /**
     * @return Returns the currentTileOrientation.
     */
    public MapOrientation getMapOrientation() {
        return mapOrientation;
    }

    /**
     * @return Returns the hexes.
     */
    public Collection<MapHex> getHexes() {
        return hexes.values();
    }
    
    
    public MapHex getNeighbour(MapHex hex, HexSide side) {
        return hexTable.get(hex, side);
    }

    /**
     * A utility to find if a newly laid tile will create a "bridge"
     * (i.e. connect track across a "river" between neighbouring hexes).
     * It is placed here because it is used in both the UI and the server.
     *
     * @param hex The MapHex where a new tile is (being) laid.
     * @param newTile The tile (being) laid.
     * @param newTileRotation The rotation of that tile.
     * @return A HexSideSet object that describes any new bridges,
     * or null if there isn't any.
     */
    public HexSidesSet findNewBridgeSides(MapHex hex, Tile newTile, int newTileRotation) {

        // Has this hex any river sides?
        HexSidesSet rivers = hex.getRiverSides();
        logHexSides(rivers, "River:");
        if (rivers.isEmpty()) return null;

        // Which tracks has the newly laid tile?
        HexSidesSet newTracks = hex.getTrackSides(newTile, newTileRotation);
        logHexSides(newTracks, "New tile track:");

        // Which tracks has the current (i.e. old) tile?
        HexSidesSet oldTracks = hex.getTrackSides();
        logHexSides(oldTracks, "Old tile track:");

        // Which tracks are really new?
        newTracks = newTracks.symDiff(oldTracks);
        logHexSides(newTracks, "New track:");

        // Which new tracks reach a river?
        HexSidesSet bridgePoints = rivers.intersection(newTracks);
        logHexSides(bridgePoints, "New track+river:");
        if (bridgePoints.isEmpty()) return null;

        // Do neighbours have track reaching the same river sides?
        BitSet newBridgeBitSet = new BitSet();
        Iterator it = bridgePoints.iterator();
        while (it.hasNext()) {
            HexSide side = (HexSide) it.next();
            MapHex neighbour = getNeighbour(hex, side);
            Tile nbTile = neighbour.getCurrentTile();
            int nbRot = neighbour.getCurrentTileRotation().getTrackPointNumber();
            if (neighbour.getTrackSides(nbTile, nbRot).get(side.opposite())) {
                newBridgeBitSet.set(side.getTrackPointNumber(), true);
            }
        }
        HexSidesSet newBridges = HexSidesSet.create(newBridgeBitSet);
        logHexSides(newBridges, "New bridge:");

        return newBridges.isEmpty() ? null : newBridges;
    }

    // For debugging
    public void logHexSides(HexSidesSet sides, String prefix) {
        if (!log.isDebugEnabled() || sides == null) return;
        Iterator<HexSide> it = sides.iterator();
        while (it.hasNext()) {
            log.debug("{} {}", prefix, it.next());
        }
    }

    public MapHex getHex(String locationCode) {
        // MapManager is a RailsManager so it is possible to locate by id
        return (MapHex) locate(locationCode);
    }

    public MapHex.Coordinates getMinimum() {
        return minimum;
    }
    
    public MapHex.Coordinates getMaximum() {
        return maximum;
    }
    
    public String getMapUIClassName() {
        return mapOrientation.getUIClassName();
    }

    public Access getDefaultAccessType(Stop.Type type) {
        return defaultAccessTypes.get(type);
    }

    public List<Stop> getCurrentStops() {
        ImmutableList.Builder<Stop> stops = ImmutableList.builder();
        for (MapHex hex : hexes.values()) {
            stops.addAll(hex.getStops());
        }
        return stops.build();
    }

    public SortedSet<Integer> getPossibleTileCosts() {
        return possibleTileCosts;
    }

    public List<MapHex> parseLocations (String locationCodes)
            throws IllegalArgumentException {

        ImmutableList.Builder<MapHex> locationBuilder = ImmutableList.builder(); 
        for (String hexName : locationCodes.split(",")) {
            MapHex hex = getHex(hexName);
            if (hex != null) {
                locationBuilder.add(hex);
            } else {
                throw new IllegalArgumentException("Invalid hex "+hexName+
                        " specified in location string " + locationCodes);
            }
        }
        return locationBuilder.build();
    }

    /**
     * Calculate the distance between two hexes as in 1835,
     * i.e. as "the crow without a passport flies".
     */
    public int getHexDistance (MapHex hex1, MapHex hex2) {
        if (hexDistances == null) {
            hexDistances = HashBasedTable.create();
        }

        if (!hexDistances.contains(hex1, hex2)) {
            calculateHexDistances(hex1, hex1, 0);
        }
        return hexDistances.get(hex1, hex2);
    }

    private void calculateHexDistances (MapHex initHex, MapHex currentHex, int depth) {
        hexDistances.put(initHex, currentHex, depth);
        
        // check for next hexes
        depth ++;
        for (MapHex nextHex:hexTable.row(currentHex).values()) {
            if (!hexDistances.contains(initHex, nextHex) ||
                    depth < hexDistances.get(initHex, nextHex)) {
                calculateHexDistances(initHex, nextHex, depth);
            }
        }
    }


    /**
     * Calculate the distances between a given tokenable city hex
     * and all other tokenable city hexes.
     * <p> Distances are cached.
     * @param initHex Start hex
     * @return Sorted integer list containing all occurring distances only once.
     */
    public SortedSet<Integer> getCityDistances (MapHex initHex) {
        
        if (hexDistances == null) {
            hexDistances = HashBasedTable.create();
        }

        if (!hexDistances.containsRow(initHex)) {
            calculateHexDistances(initHex, initHex, 0);
        }
        
        ImmutableSortedSet.Builder<Integer> distances = 
                ImmutableSortedSet.naturalOrder();
        
        for (Entry<MapHex, Integer> otherHex:hexDistances.row(initHex).entrySet()) {
            if (otherHex.getKey().getCurrentTile().hasStations()) {
                distances.add(otherHex.getValue());
            }
        }
        return distances.build();
    }

    public String getMapImageFilepath() {
        return mapImageFilepath;
    }

    public int getMapXOffset() {
        return mapXOffset;
    }

    public int getMapYOffset() {
        return mapYOffset;
    }

    public float getMapScale() {
        return mapScale;
    }

    public void setMapXOffset(int mapXOffset) {
        this.mapXOffset = mapXOffset;
    }

    public void setMapYOffset(int mapYOffset) {
        this.mapYOffset = mapYOffset;
    }

    public void setMapScale(float mapScale) {
        this.mapScale = mapScale;
    }

    public boolean isMapImageUsed() {
        return mapImageUsed;
    }

}
