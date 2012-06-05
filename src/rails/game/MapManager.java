package rails.game;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.parser.*;
import rails.game.Stop.Loop;
import rails.game.Stop.RunThrough;
import rails.game.Stop.RunTo;
import rails.game.Stop.Score;
import rails.game.Stop.Type;
import rails.game.state.AbstractItem;
import rails.util.Util;

/**
 * MapManager configures the map layout from XML
 */
public class MapManager extends AbstractItem implements ConfigurableComponentI {

    private String mapUIClassName = null;

    // The next attributes are duplicates in MapHex. We'll see what we really
    // need.
    protected TileOrientation tileOrientation;
    protected boolean lettersGoHorizontal;
    protected boolean letterAHasEvenNumbers;

    // Optional map image (SVG file)
    protected String mapImageFilename = null;
    protected String mapImageFilepath = null;
    protected int mapXOffset = 0;
    protected int mapYOffset = 0;
    protected float mapScale = (float)1.0;
    protected boolean mapImageUsed = false;

    protected MapHex[][] hexes;
    protected Map<String, MapHex> mHexes = new HashMap<String, MapHex>();
    protected int minX, minY, maxX, maxY;
    protected int minCol, maxCol, minRow, maxRow;

    // upgrade costs on the map for noMapMode
    protected SortedSet<Integer> possibleTileCosts;

    // information to define neighbours
    protected static final int[] xDeltaNS = new int[] { 0, -1, -1, 0, +1, +1 };
    protected static final int[] yXEvenDeltaNS =
        new int[] { +1, 0, -1, -1, -1, 0 };
    protected static final int[] yXOddDeltaNS =
        new int[] { +1, +1, 0, -1, 0, +1 };
    protected static final int[] xYEvenDeltaEW =
        new int[] { -1, -1, -1, 0, +1, 0 };
    protected static final int[] xYOddDeltaEW =
        new int[] { 0, -1, 0, +1, +1, +1 };
    protected static final int[] yDeltaEW = new int[] { +1, 0, -1, -1, 0, +1 };

    // Stop property defaults per stop type
    protected Map<Type,RunTo> runToDefaults = new HashMap<Type, RunTo>();
    protected Map<Type,RunThrough> runThroughDefaults = new HashMap<Type, RunThrough>();
    protected Map<Type,Loop> loopDefaults = new HashMap<Type, Loop>();
    protected Map<Type,Score> scoreTypeDefaults = new HashMap<Type, Score>();

    protected static Logger log =
        LoggerFactory.getLogger(MapManager.class.getPackage().getName());

    public MapManager() {
    }

    /**
     * @see rails.common.parser.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        String attr = tag.getAttributeAsString("tileOrientation");
        if (attr == null)
            throw new ConfigurationException("Map orientation undefined");
        try {
            tileOrientation = TileOrientation.valueOf(attr);
        }
        catch(IllegalArgumentException exception) {
            throw new ConfigurationException("Invalid tile orientation: " + attr, exception);
        }

        switch(tileOrientation) {
        case NS:
            mapUIClassName = "rails.ui.swing.hexmap.NSHexMap";
            break;
        case EW:
            mapUIClassName = "rails.ui.swing.hexmap.EWHexMap";
            break;
        default:
            // Unexpected default.
            throw new AssertionError(tileOrientation);
        }

        attr = tag.getAttributeAsString("letterOrientation");
        if (attr.equals("horizontal")) {
            lettersGoHorizontal = true;
        } else if (attr.equals("vertical")) {
            lettersGoHorizontal = false;
        } else {
            throw new ConfigurationException("Invalid letter orientation: "
                    + attr);
        }

        attr = tag.getAttributeAsString("even");
        letterAHasEvenNumbers = ((attr.toUpperCase().charAt(0) - 'A')) % 2 == 0;

        // Map image attributes
        Tag mapImageTag = tag.getChild("Image");
        if (mapImageTag != null) {
            mapImageFilename = mapImageTag.getAttributeAsString("file");
            mapXOffset = mapImageTag.getAttributeAsInteger("x", mapXOffset);
            mapYOffset = mapImageTag.getAttributeAsInteger("y", mapYOffset);
            mapScale = mapImageTag.getAttributeAsFloat("scale", mapScale);
        }

        List<Tag> hexTags = tag.getChildren("Hex");
        MapHex hex;
        minX = minY = minCol = minRow = Integer.MAX_VALUE;
        maxX = maxY = maxCol = maxRow = Integer.MIN_VALUE;
        possibleTileCosts = new TreeSet<Integer>();
        for (Tag hexTag : hexTags) {
            hex = new MapHex();
            hex.configureFromXML(hexTag);
            hex.init(this, hex.getName());
            mHexes.put(hex.getId(), hex);
            minX = Math.min(minX, hex.getX());
            minY = Math.min(minY, hex.getY());
            maxX = Math.max(maxX, hex.getX());
            maxY = Math.max(maxY, hex.getY());
            minCol = Math.min(minCol, hex.getColumn());
            minRow = Math.min(minRow, hex.getRow());
            maxCol = Math.max(maxCol, hex.getColumn());
            maxRow = Math.max(maxRow, hex.getRow());
            //log.debug("+++ Hex "+hex.getName()+" x="+hex.getX()+" y="+hex.getY()+" row="+hex.getRow()+" col="+hex.getColumn());
            int[] tileCosts = hex.getTileCostAsArray();
            for (int i=0; i<tileCosts.length; i++){
                possibleTileCosts.add(tileCosts[i]);
            }
        }
        log.debug("Possible tileCosts on map are "+possibleTileCosts);

        int xOffset = 0;
        int yOffset = 0;
        if (minX < 0) {
            xOffset = -minX;
            maxX += xOffset;
            minX = 0;
        }
        if (minY < 0) {
            yOffset = -minY;
            maxY += yOffset;
            minY = 0;
        }

        hexes = new MapHex[1 + maxX][1 + maxY];

        for (String hexName : mHexes.keySet()) {
            hex = mHexes.get(hexName);
            if (xOffset > 0) hex.addX(xOffset);
            if (yOffset > 0) hex.addY(yOffset);
            hexes[hex.getX()][hex.getY()] = hex;
            //log.debug("--- Hex "+hex.getName()+" x="+hex.getX()+" y="+hex.getY()+" row="+hex.getRow()+" col="+hex.getColumn());
        }

        // Parse default stop types
        Type type;
        RunTo runTo;
        RunThrough runThrough;
        Loop loop;
        Score score;
        String s;
        Tag defaultsTag = tag.getChild("Defaults");
        if (defaultsTag != null) {
            List<Tag> accessTags = defaultsTag.getChildren("Access");
            for (Tag accessTag : accessTags) {
                // Type
                s = accessTag.getAttributeAsString("type", null);
                if (Util.hasValue(s)) {
                    try {
                        type = Type.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for default property type: "+s, e);
                    }
                } else {
                    type = null; // For default defaults
                }
                // RunTo
                s = accessTag.getAttributeAsString("runTo", null);
                if (Util.hasValue(s)) {
                    try {
                        runTo = RunTo.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default runTo property: "+s, e);
                    }
                    runToDefaults.put(type, runTo);
                }
                // RunThrough
                s = accessTag.getAttributeAsString("runThrough", null);
                if (Util.hasValue(s)) {
                    try {
                        runThrough = RunThrough.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default runThrough property: "+s, e);
                    }
                    runThroughDefaults.put(type, runThrough);
                }
                // Loop
                s = accessTag.getAttributeAsString("loop", null);
                if (Util.hasValue(s)) {
                    try {
                        loop = Loop.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default loop property: "+s, e);
                    }
                    loopDefaults.put(type, loop);
                }
                // Score type (not allowed for a null stop type)
                s = accessTag.getAttributeAsString("scoreType", null);
                if (type != null && Util.hasValue(s)) {
                    try {
                        score = Score.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default score type property: "+s, e);
                    }
                    scoreTypeDefaults.put(type, score);
                }
            }
        }
    }

    public void finishConfiguration (GameManager gameManager) throws ConfigurationException {

        MapHex hex;
        int i, j, k;
        MapHex nb;

        mapImageUsed = rails.util.Util.hasValue(mapImageFilename)
        && "yes".equalsIgnoreCase(Config.get("map.image.display"));
        if (mapImageUsed) {
            String rootDirectory = Config.get("map.root_directory");
            if (!rails.util.Util.hasValue(rootDirectory)) {
                rootDirectory = "data";
            }
            mapImageFilepath = rootDirectory + "/" + mapImageFilename;
        }

        for (String hexName : mHexes.keySet()) {
            hex = mHexes.get(hexName);
            hex.finishConfiguration(gameManager);
        }

        // Initialise the neighbours
        int ii, jj;
        for (i = minX; i <= maxX; i++) {
            for (j = minY; j <= maxY; j++) {
                if ((hex = hexes[i][j]) == null) continue;

                for (k = 0; k < 6; k++) {
                    ii = getAdjacentX (i, j, k);
                    jj = getAdjacentY (i, j, k);
                    if (ii >= minX && ii <= maxX && jj >= minY && jj <= maxY
                            && (nb = hexes[ii][jj]) != null) {
                        if (hex.isNeighbour(nb, k)
                                && nb.isNeighbour(hex, k + 3)) {
                            hex.setNeighbor(k, nb);
                            nb.setNeighbor(k + 3, hex);
                        }
                        if (hex.isImpassable(nb) || nb.isImpassable(hex)) {
                            hex.addImpassableSide(k);
                        }
                    }

                }
            }
        }

        List<MapHex> homeHexes;
        for (PublicCompany company : gameManager.getCompanyManager().getAllPublicCompanies()) {
            if ((homeHexes = company.getHomeHexes()) != null) {
                for (MapHex homeHex : homeHexes) {
                    homeHex.addHome(company, company.getHomeCityNumber());
                }
            }
            if ((hex = company.getDestinationHex()) != null) {
                hex.addDestination(company);
            }
        }
    }

    /**
     * @return Returns the letterAHasEvenNumbers.
     */
    public boolean letterAHasEvenNumbers() {
        return letterAHasEvenNumbers;
    }

    /**
     * @return Returns the lettersGoHorizontal.
     */
    public boolean lettersGoHorizontal() {
        return lettersGoHorizontal;
    }

    public int getAdjacentX (int x, int y, int orientation) {

        if (tileOrientation == TileOrientation.EW) {
            return x + (y % 2 == 0 ? xYEvenDeltaEW[orientation] : xYOddDeltaEW[orientation]);
        } else {
            return x + xDeltaNS[orientation];
        }
    }

    public int getAdjacentY (int x, int y, int orientation) {

        if (tileOrientation == TileOrientation.EW) {
            return y + yDeltaEW[orientation];
        } else {
            return y + ((x % 2 == 0) == letterAHasEvenNumbers ?
                    yXEvenDeltaNS[orientation] : yXOddDeltaNS[orientation]);
        }
    }

    /**
     * @return Returns the currentTileOrientation.
     */
    public TileOrientation getTileOrientation() {
        return tileOrientation;
    }

    /**
     * @return Returns the hexes.
     */
    public MapHex[][] getHexes() {
        return hexes;
    }

    public List<MapHex> getHexesAsList() {
        return new ArrayList<MapHex>(mHexes.values());
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxCol() {
        return maxCol;
    }

    public int getMaxRow() {
        return maxRow;
    }

    public int getMinCol() {
        return minCol;
    }

    public int getMinRow() {
        return minRow;
    }

    /**
     * @return Returns the mapUIClassName.
     */
    public String getMapUIClassName() {
        return mapUIClassName;
    }

    public MapHex getHex(String locationCode) {
        return mHexes.get(locationCode);
    }

    public List<Stop> getCurrentStations() {
        List<Stop> stations = new ArrayList<Stop>();
        for (MapHex hex : mHexes.values()) {
            stations.addAll(hex.getStops());
        }
        return stations;
    }

    public SortedSet<Integer> getPossibleTileCosts() {
        return possibleTileCosts;
    }

    public List<MapHex> parseLocations (String locationCodes)
    throws ConfigurationException {

        List<MapHex> locations = new ArrayList<MapHex>();
        MapHex hex;
        locations = new ArrayList<MapHex>();
        for (String hexName : locationCodes.split(",")) {
            hex = getHex(hexName);
            if (hex != null) {
                locations.add(hex);
            } else {
                throw new ConfigurationException ("Invalid hex "+hexName+
                        " specified in location string "+locationCodes);
            }
        }

        return locations;
    }

    /**
     * Calculate the distance between two hexes as in 1835,
     * i.e. as "the crow without a passport flies".
     * @param hex1
     * @param hex2
     * @return
     */
    public int getHexDistance (MapHex hex1, MapHex hex2) {

        if (distances == null) distances = new HashMap<MapHex, Map<MapHex, Integer>> ();
        if (distances.get(hex1) == null) {
            distances.put(hex1, new HashMap<MapHex, Integer>());
            calculateHexDistances(hex1, hex1, 0);
        }
        return distances.get(hex1).get(hex2);
    }

    private void calculateHexDistances (MapHex hex1, MapHex hex2, int depth) {

        if (distances.get(hex1).get(hex2) == null) {
            distances.get(hex1).put(hex2, depth);
        } else {
            if (distances.get(hex1).get(hex2) <= depth) return;
            distances.get(hex1).put(hex2, depth);
        }

        for (MapHex hex3 : hex2.getNeighbors()) {
            if (hex3 == null) continue;
            if (distances.get(hex1).get(hex3) == null) {
                calculateHexDistances (hex1, hex3, depth+1);
            } else if (distances.get(hex1).get(hex3) > depth+1) {
                calculateHexDistances (hex1, hex3, depth+1);
            }
        }
    }

    /** Cache to hold all unique distance values of tokenable cities from a given hex */
    private Map<MapHex, int[]> uniqueCityDistances;
    /** Cache to hold all minimal hex distances from given hexes */
    private Map<MapHex, Map<MapHex, Integer>> distances;

    /**
     * Calculate the distances between a given tokenable city hex
     * and all other tokenable city hexes.
     * <p> The array is cached, so it need be calculated only once.
     * @param hex Start hex
     * @return Sorted int array containing all occurring distances only once.
     */
    public int[] getCityDistances (MapHex hex) {

        if (!hex.getCurrentTile().hasStations()) return new int[0];
        if (uniqueCityDistances == null) uniqueCityDistances = new HashMap<MapHex, int[]> ();
        if (uniqueCityDistances.containsKey(hex)) return uniqueCityDistances.get(hex);

        int distance;
        Set<Integer> distancesSet = new TreeSet<Integer> ();
        for (MapHex hex2 : mHexes.values()) {
            if (!hex2.getCurrentTile().hasStations()) continue;
            distance = getHexDistance (hex, hex2);
            distancesSet.add(distance);
        }
        int[] distances = new int[distancesSet.size()];
        int i=0;
        for (int distance2 : distancesSet) {
            distances[i++] = distance2;
        }
        uniqueCityDistances.put(hex, distances);
        return distances;
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

    public boolean isMapImageUsed() {
        return mapImageUsed;
    }

    public RunTo getRunToDefault(Type type) {
        return runToDefaults.containsKey(type) ? runToDefaults.get(type) : null;
    }

    public RunThrough getRunThroughDefault(Type type) {
        return runThroughDefaults.containsKey(type) ? runThroughDefaults.get(type) : null;
    }

    public Loop getLoopDefault(Type type) {
        return loopDefaults.containsKey(type) ? loopDefaults.get(type) : null;
    }

    public Score getScoreTypeDefault(Type type) {
        return scoreTypeDefaults.containsKey(type) ? scoreTypeDefaults.get(type) : null;
    }

}
