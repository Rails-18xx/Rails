/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/MapManager.java,v 1.10 2009/10/09 22:29:01 evos Exp $ */
package rails.game;

import java.util.*;

import rails.util.Tag;

/**
 * MapManager configures the map layout from XML
 */
public class MapManager implements ConfigurableComponentI {

    private String mapUIClassName = null;
    private static MapManager instance = null;

    // The next attributes are duplicates in MapHex. We'll see what we really
    // need.
    protected static int tileOrientation;
    protected static boolean lettersGoHorizontal;
    protected static boolean letterAHasEvenNumbers;

    protected static MapHex[][] hexes;
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

    public MapManager() {
        instance = this;
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
            MapHex.setTileOrientation(MapHex.EW);
        } else if (attr.equals("NS")) {
            tileOrientation = MapHex.NS;
            MapHex.setTileOrientation(MapHex.NS);
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
        MapHex.setLettersGoHorizontal(lettersGoHorizontal);

        attr = tag.getAttributeAsString("even");
        letterAHasEvenNumbers = ((attr.toUpperCase().charAt(0) - 'A')) % 2 == 0;
        MapHex.setLetterAHasEvenNumbers(letterAHasEvenNumbers);

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

    public void finishConfiguration (GameManager gameManager) {

        MapHex hex;
        int i, j, k, dx, dy;
        MapHex nb;

        for (String hexName : mHexes.keySet()) {
            hex = mHexes.get(hexName);
            hex.finishConfiguration(gameManager);
        }

        // Initialise the neighbours
        /**
         * TODO: impassable hexsides. TODO: blank sides of fixed and offboard
         * preprinted tiles.
         */
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
     * @return an instance of the MapManager
     */
    public static MapManager getInstance() {
        return instance;
    }

    /**
     * @return Returns the letterAHasEvenNumbers.
     */
    public static boolean letterAHasEvenNumbers() {
        return letterAHasEvenNumbers;
    }

    /**
     * @return Returns the lettersGoHorizontal.
     */
    public static boolean lettersGoHorizontal() {
        return lettersGoHorizontal;
    }

    /**
     * @return Returns the currentTileOrientation.
     */
    public static int getTileOrientation() {
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
}
