/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/MapManager.java,v 1.15 2010/02/08 21:20:39 evos Exp $ */
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
        for (Tag hexTag : hexTags) {
            hex = new MapHex(this);
            hex.configureFromXML(hexTag);
            mHexes.put(hex.getName(), hex);
            maxX = Math.max(maxX, hex.getX());
            maxY = Math.max(maxY, hex.getY());
        }

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


}
