/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/MapManager.java,v 1.23 2010/06/24 21:48:08 stefanfrey Exp $ */
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
        minX = minY = minCol = minRow = 9999;
        maxX = maxY = maxCol = maxRow = -9999;
        possibleTileCosts = new TreeSet<Integer>();
        for (Tag hexTag : hexTags) {
            hex = new MapHex(this);
            hex.configureFromXML(hexTag);
            mHexes.put(hex.getName(), hex);
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
    }

    public void finishConfiguration (GameManagerI gameManager) throws ConfigurationException {

        MapHex hex;
        int i, j, k;
        MapHex nb;

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
        for (PublicCompanyI company : gameManager.getCompanyManager().getAllPublicCompanies()) {
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
        
         if (tileOrientation == MapHex.EW) {
            return x + (y % 2 == 0 ? xYEvenDeltaEW[orientation] : xYOddDeltaEW[orientation]);
         } else {
            return x + xDeltaNS[orientation];
         }
    }
    
    public int getAdjacentY (int x, int y, int orientation) {
        
        if (tileOrientation == MapHex.EW) {
            return y + yDeltaEW[orientation];
        } else {
            return y + ((x % 2 == 0) == letterAHasEvenNumbers ? 
                    yXEvenDeltaNS[orientation] : yXOddDeltaNS[orientation]);
        }
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

}
