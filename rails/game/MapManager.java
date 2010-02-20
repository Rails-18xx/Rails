/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/MapManager.java,v 1.18 2010/02/20 12:34:43 evos Exp $ */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.util.Tag;

/**
 * MapManager configures the map layout from XML
 */
public class MapManager implements ConfigurableComponentI {

    private String mapUIClassName = null;

    // The next attributes are duplicates in MapHex. We'll see what we really
    // need.
    protected int tileOrientation;
    protected boolean lettersGoHorizontal;
    protected boolean letterAHasEvenNumbers;

    protected MapHex[][] hexes;
    protected Map<String, MapHex> mHexes = new HashMap<String, MapHex>();
    protected int maxX, maxY;
    
    // upgrade costs on the map for noMapMode
    protected SortedSet<Integer> possibleTileCosts;

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

    protected static Logger log =
        Logger.getLogger(MapManager.class.getPackage().getName());

    public MapManager() {
    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        mapUIClassName = tag.getAttributeAsString("mapClass");
        if (mapUIClassName == null) {
            throw new ConfigurationException("Map class name missing");
        }

        String attr = tag.getAttributeAsString("tileOrientation");
        if (attr == null)
            throw new ConfigurationException("Map orientation undefined");
        if (attr.equals("EW")) {
            tileOrientation = MapHex.EW;
        } else if (attr.equals("NS")) {
            tileOrientation = MapHex.NS;
        } else {
            throw new ConfigurationException("Invalid tile orientation: "
                                             + attr);
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

        List<Tag> hexTags = tag.getChildren("Hex");
        MapHex hex;
        maxX = 0;
        maxY = 0;
        possibleTileCosts = new TreeSet<Integer>();
        for (Tag hexTag : hexTags) {
            hex = new MapHex(this);
            hex.configureFromXML(hexTag);
            mHexes.put(hex.getName(), hex);
            maxX = Math.max(maxX, hex.getX());
            maxY = Math.max(maxY, hex.getY());
            int[] tileCosts = hex.getTileCostAsArray();
            for (int i=0; i<tileCosts.length; i++){
                possibleTileCosts.add(tileCosts[i]);
            }
        }
        log.debug("Possible tileCosts on map are "+possibleTileCosts);
        
        hexes = new MapHex[1 + maxX][1 + maxY];

        for (String hexName : mHexes.keySet()) {
            hex = mHexes.get(hexName);
            hexes[hex.getX()][hex.getY()] = hex;
        }
    }

    public void finishConfiguration (GameManagerI gameManager) {

        MapHex hex;
        int i, j, k, dx, dy;
        MapHex nb;

        for (String hexName : mHexes.keySet()) {
            hex = mHexes.get(hexName);
            hex.finishConfiguration(gameManager);
        }

        // Initialise the neighbours
        for (i = 0; i <= maxX; i++) {
            for (j = 0; j <= maxY; j++) {
                if ((hex = hexes[i][j]) == null) continue;

                for (k = 0; k < 6; k++) {
                    if (tileOrientation == MapHex.EW) {
                        dx = (j % 2 == 0 ? xYEvenDeltaEW[k] : xYOddDeltaEW[k]);
                        dy = yDeltaEW[k];
                    } else {
                        dx = xDeltaNS[k];
                        dy = (i % 2 == 0 ? yXEvenDeltaNS[k] : yXOddDeltaNS[k]);
                    }
                    if (i + dx >= 0 && i + dx <= maxX && j + dy >= 0
                        && j + dy <= maxY
                        && (nb = hexes[i + dx][j + dy]) != null) {
                        if (hex.isNeighbour(nb, k)
                            && nb.isNeighbour(hex, k + 3)) {
                            hex.setNeighbor(k, nb);
                            nb.setNeighbor(k + 3, hex);
                        }
                        if (hex.isImpassable(nb) || nb.isImpassable(hex)) {
                            hex.addImpassableSide(k);
                            //nb.addImpassableSide(k+3);
                        }
                    }

                }
            }
        }

        for (PublicCompanyI company : gameManager.getCompanyManager().getAllPublicCompanies()) {
            if ((hex = company.getHomeHex()) != null) {
                hex.addHome(company, company.getHomeCityNumber());
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

    /**
     * @return Returns the currentTileOrientation.
     */
    public int getTileOrientation() {
        return tileOrientation;
    }

    /**
     * @return Returns the hexes.
     */
    public MapHex[][] getHexes() {
        return hexes;
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

    public List<City> getCurrentStations() {
        List<City> stations = new ArrayList<City>();
        for (MapHex hex : mHexes.values()) {
            stations.addAll(hex.getCities());
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
    	Map<MapHex, Integer> passed = new HashMap<MapHex, Integer>();
    	return calculateHexDistance (hex1, hex2, 1, passed);
    }
    
    /** Cache to hold distances between tokenable cities */
    private Map<MapHex, int[]> distanceMap;
    
    /** 
     * Calculate the distances between a given tokenable city hex
     * and all other tokenable city hexes. 
     * <p> The array is cached, so it need be calculated only once.
     * @param hex Start hex
     * @return Sorted int array containing all occurring distances only once. 
     */
    public int[] getCityDistances (MapHex hex) {
log.debug("+++ Checking distances from "+hex.getName());        
        if (!hex.getCurrentTile().hasStations()) return new int[0];
        if (distanceMap == null) distanceMap = new HashMap<MapHex, int[]> ();
        if (distanceMap.containsKey(hex)) return distanceMap.get(hex);
        
        int distance;
        Set<Integer> distancesSet = new TreeSet<Integer> (); 
        for (MapHex hex2 : mHexes.values()) {
log.debug("--- Checking other hex "+hex2.getName());
            if (!hex2.getCurrentTile().hasStations()) continue;
            distance = getHexDistance (hex, hex2);
            distancesSet.add(distance);
log.debug("=== Distance is "+distance);
        }
        int[] distances = new int[distancesSet.size()];
        int i=0;
        for (int distance2 : distancesSet) {
            distances[i++] = distance2;
        }
        distanceMap.put(hex, distances);
        return distances;
    }

    /** Helper method to calculate the distance between two hexes.
     * Called recursively. */
    private int calculateHexDistance (MapHex hex1, MapHex hex2, int depth,
    		Map<MapHex, Integer> passed) {

    	/* Map to sort the neighbours (roughly) into decreasing distance */
    	SortedMap<Integer, MapHex> neighbours = new TreeMap<Integer, MapHex>();

    	for (MapHex hex3 : hex1.getNeighbors()) {

    		if (hex3 == null) continue;

    		// Are we finished?
    		if (hex3 == hex2) {
				return 1;
    		}

    		if (passed.containsKey(hex3) && passed.get(hex3) < depth - 1) {
    			// Backtrack
    			return -1;
    		}

    		// Sort neighbours on decreasing (rough) distance
        	int distance = Math.abs(hex2.getX() - hex3.getX())
				+ Math.abs(hex2.getY() - hex3.getY());
        	neighbours.put(distance, hex3);
    	}
		passed.put (hex1, depth);
		for (MapHex neighbour : neighbours.values()) {
    		if (passed.containsKey(neighbour)) continue;
    		int result = calculateHexDistance (neighbour, hex2, depth+1, passed);
    		if (result < 0) {
    			return 0; // Continue backtracking
    		} else if (result > 0) {
    			return result + 1;
    		}
    		// Continue loop if result == 0
    	}
		// Should never get here
    	return 0;
    }
}
