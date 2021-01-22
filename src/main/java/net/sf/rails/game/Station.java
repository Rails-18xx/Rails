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

    private final String id;
    private final Stop.Type type;
    private final int number;
    private final int value;
    private final int baseSlots;
    private final Tile tile;
    private final int position;
    private final String stopName;

    /** Included, but not yet set or used.
     *  If the need arises (two different stations on one hex with non-default
     *  access parameters), an Access section should be defined in TileSet.xml
     *  (NOT in Tiles.xml, which can be overwritten).
     */
    private Access access = null;

    private Station(Tile tile, int number, String id, Stop.Type type, int value,
                    int slots, int position, String cityName, Tag accessTag) {
        this.tile = tile;
        this.number = number;
        this.id = id;
        this.type = type;
        this.value = value;
        this.baseSlots = slots;
        this.position = position;
        this.stopName = cityName;

        log.debug ("----- Tile={} station={} type={} value={}",
                tile.getId(), id, type, value);
        if (accessTag != null) {
            try {
                access = Access.parseAccessTag(tile, accessTag);
            } catch (ConfigurationException e) {
                log.error("Exception while parsing Access of tile {}: {}", tile, e);
            }
        }
    }

    public static Station create(Tile tile, Tag stationDefTag, Tag stationSetTag)
            throws ConfigurationException {
        String sid = stationDefTag.getAttributeAsString("id");

        if (sid == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileStationHasNoID", tile.getId()));

        int number = - TrackPoint.parseTrackPointNumber(sid);

        String stype = stationDefTag.getAttributeAsString("type");
        Tag accessTag = null;
        if (stype == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileStationHasNoType", tile.getId()));
        if (stationSetTag != null) {
            stype = stationSetTag.getAttributeAsString("type", stype);
            accessTag = stationSetTag.getChild("Access");
        }

        if ("OffMapCity".equalsIgnoreCase(stype)) stype = "OffMap";  // Can also be a town
        Stop.Type type = Stop.Type.valueOf(stype.toUpperCase());
        if (type == null) {
            throw new ConfigurationException(LocalText.getText(
                    "TileStationHasInvalidType",
                    tile.getId(),
                    type ));
        }
        int value = stationDefTag.getAttributeAsInteger("value", 0);
        int slots = stationDefTag.getAttributeAsInteger("slots", 0);
        int position = stationDefTag.getAttributeAsInteger("position", 0);
        String cityName = stationDefTag.getAttributeAsString("city");

        //Station station = new Station(tile, number, sid, type, value, slots,
        Station station = new Station(tile, number, String.valueOf(number), type, value, slots,
                    position, cityName, accessTag);
        return station;
    }


    public String getName() {
        return "Station " + id + " on " + tile.getClass().getSimpleName() + " "
        + tile.toText();
    }

    // Replaced by mutexId
    @Deprecated
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

    public Access getAccess() {
        return access;
    }

    public Stop.Type getType() {
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
        return type.toString() + " " + number;
    }

    // Comparable method
    @Override
    public int compareTo(Station other) {
        return this.getId().compareTo(other.getId());
    }

    @Override
    // EV oct 2020: shortened
    public String toString() {
        //return "Station " + number + " on tile #" + tile.getId() + " ID: " + id
        //+ ", Type: " + type + ", Slots: " + baseSlots + ", Value: "
        //+ value + ", Position:" + position;
        return "station " + number;
    }
}
