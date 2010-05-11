/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Station.java,v 1.12 2010/05/11 21:47:21 stefanfrey Exp $ */
package rails.game;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A Station object represents any junction on a tile, where one, two or more
 * track fragments meet. The usual Station types are "City", "Town" and
 * "OffMapCity". Other types found in some games are "Pass" (1841), "Port"
 * (1841, 18EU) and "Halt" (1860). <p> The station types "City" and "OffMapCity"
 * may have slots for placing tokens. <p> Station objects are used in Tile
 * objects, to represent the station(s) on a tile. Each tile type is represented
 * by just one Tile object (which is NOT cloned or newly instantiated when a
 * Tile is laid). Please note, that all preprinted tiles on the map are also
 * represented by Tile objects, so laying the first tile on a hex is treated as
 * a normal upgrade in this program. <p> See also the City class, which
 * represents stations on tiles that have actually been laid on a MapHex.
 *
 * @author Erik Vos
 */
public class Station {

    private String id;
    private String type;
    private int number;
    private int value;
    private int baseSlots;
    private TileI tile;
    private int position;
    private String cityName;
    private int x;
    private int y;

    public static final String CITY = "City";
    public static final String TOWN = "Town";
    public static final String HALT = "Halt";
    public static final String OFF_MAP_AREA = "OffMapCity";
    public static final String PORT = "Port";
    public static final String PASS = "Pass";
    public static final String JUNCTION = "Junction"; // No station, just a
    // branching point.
    private static final String[] types =
            { CITY, TOWN, HALT, OFF_MAP_AREA, PORT, PASS, JUNCTION };
    private static final List<String> validTypes = Arrays.asList(types);

    protected static Logger log =
            Logger.getLogger(Station.class.getPackage().getName());

    /** Check validity of a Station type */
    public static boolean isTypeValid(String type) {
        return validTypes.contains(type);
    }

    public Station(TileI tile, int number, String id, String type, int value,
            int slots, int position, String cityName) {
        this.tile = tile;
        this.number = number;
        this.id = id;
        this.type = type;
        this.value = value;
        this.baseSlots = slots;
        this.position = position;
        this.cityName = cityName;
        convertPosition();
        // log.debug(toString()+": x="+x+" y="+y);
    }

    public String getName() {
        return "Station " + id + " on " + tile.getClass().getSimpleName() + " "
               + tile.getName();
    }
    
    public String getCityName() {
        return cityName;
    }

    /**
     * @return Returns the holder.
     */
    public Object getTile() {
        return tile;
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return Returns the baseSlots.
     */
    public int getBaseSlots() {
        return baseSlots;
    }

    /**
     * @return Returns the value.
     */
    public int getValue() {
        // log.debug(toString());
        return value;
    }

    public int getPosition() {
        return position;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private void convertPosition() {
        if (position == 0) {
            x = y = 0;
            return;
        }

        x = 0;
        y = 12;
        rotatePosition(position / 100);
    }

    private void rotatePosition(int rotation) {
        double r = Math.toRadians(60 * rotation);
        double dx = x * Math.sin(r) + y * Math.cos(r);
        double dy = y * Math.sin(r) + x * Math.cos(r);
        x = (int) Math.round(dx);
        y = (int) Math.round(dy);
    }

    @Override
    public String toString() {
        return "Station " + number + " on tile #" + tile.getId() + " ID: " + id
               + ", Type: " + type + ", Slots: " + baseSlots + ", Value: "
               + value;
    }

}
