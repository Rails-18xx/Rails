package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



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
 * a normal upgrade in this program. <p> See also the Stop class, which
 * represents stations on tiles that have actually been laid on a MapHex.
 *
 * Station has the following ids:
 * String id: The attribute "id" of the station tag (e.g. "city1")
 * int number: The number inside the string (e.g. 1)
 *
 */
public class Station extends TrackPoint implements Comparable<Station> {

    private static final Logger log = LoggerFactory.getLogger(Station.class);

    public static enum Type {
        CITY (StopType.Defaults.CITY, "City"),
        TOWN (StopType.Defaults.TOWN, "Town"),
        HALT (StopType.Defaults.TOWN, "Halt"),
        OFFMAPCITY (StopType.Defaults.OFFMAP, "OffMap"),
        PORT (StopType.Defaults.TOWN, "Port"),
        PASS (StopType.Defaults.CITY, "Pass"),
        JUNCTION (StopType.Defaults.NULL, "Junction");

        private final StopType stopType;
        private final String text;

        private Type(StopType.Defaults type, String text) {
            this.stopType = type.getStopType();
            this.text = text;
        }
        public StopType getStopType() {
            return stopType;
        }
        public String toText() {
            return text;
        }
    }

    private final String id;
    private final Station.Type type;
    private final int number;
    private final int value;
    private final int baseSlots;
    private final Tile tile;
    private final int position;
    private final String stopName;

    private Station(Tile tile, int number, String id, Station.Type type, int value,
            int slots, int position, String cityName) {
        this.tile = tile;
        this.number = number;
        this.id = id;
        this.type = type;
        this.value = value;
        this.baseSlots = slots;
        this.position = position;
        this.stopName = cityName;
        log.debug("Created " + this);
    }

    public static Station create(Tile tile, Tag stationTag) throws ConfigurationException {
        String sid = stationTag.getAttributeAsString("id");

        if (sid == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileStationHasNoID", tile.getId()));

        int number = - TrackPoint.parseTrackPointNumber(sid);

        String stype = stationTag.getAttributeAsString("type");
        if (stype == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileStationHasNoType", tile.getId()));

        Station.Type type = Station.Type.valueOf(stype.toUpperCase());
        if (type == null) {
            throw new ConfigurationException(LocalText.getText(
                    "TileStationHasInvalidType",
                    tile.getId(),
                    type ));
        }
        int value = stationTag.getAttributeAsInteger("value", 0);
        int slots = stationTag.getAttributeAsInteger("slots", 0);
        int position = stationTag.getAttributeAsInteger("position", 0);
        String cityName = stationTag.getAttributeAsString("city");
        return new Station(tile, number, sid, type, value, slots,
                    position, cityName);
    }


    public String getName() {
        return "Station " + id + " on " + tile.getClass().getSimpleName() + " "
        + tile.toText();
    }

    public String getStopName() {
        return stopName;
    }

    /**
     * @return Returns the holder.
     */
    public Tile getTile() {
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
     * @return Returns the baseSlots.
     */
    public int getBaseSlots() {
        return baseSlots;
    }

    /**
     * @return Returns the value.
     */
    public int getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    public StopType getStopType() {
        return type.getStopType();
    }

    public Station.Type getType() {
        return type;
    }

    // TrackPoint methods
    public int getTrackPointNumber() {
        return -number;
    }

    public TrackPoint.Type getTrackPointType() {
        return TrackPoint.Type.STATION;
    }

    public TrackPoint rotate(HexSide rotation) {
        return this;
    }

    public String toText() {
        return type.toText() + " " + number;
    }

    // Comparable method
    @Override
    public int compareTo(Station other) {
        return this.getId().compareTo(other.getId());
    }

    @Override
    public String toString() {
        return "Station " + number + " on tile #" + tile.getId() + " ID: " + id
        + ", Type: " + type + ", Slots: " + baseSlots + ", Value: "
        + value + ", Position:" + position;
    }
}
