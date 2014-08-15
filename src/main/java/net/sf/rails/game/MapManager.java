package net.sf.rails.game;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

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
    private ImmutableMap<String, StopType> defaultStopTypes;
    
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

    /**
     * Used by Configure (via reflection) only
     */
    public MapManager(RailsRoot parent, String id) {
        super(parent, id);
    }
    
    /**
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
            defaultStopTypes = StopType.parseDefaults(this, accessTags);
        } else {
            defaultStopTypes = ImmutableMap.of();
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

    public Map<String, StopType> getDefaultStopTypes() {
        return defaultStopTypes;
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
     * @param hex Start hex
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
