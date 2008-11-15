/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Tile.java,v 1.22 2008/11/15 21:22:50 evos Exp $ */
package rails.game;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rails.game.model.ModelObject;
import rails.util.LocalText;
import rails.util.Tag;

public class Tile extends ModelObject implements TileI, StationHolderI {

    /** The 'internal id', identifying the tile in the XML files */
    private final int id;
    /**
     * The 'external id', which is shown in the UI. Usually equal to the
     * internal id, but different in case of duplicates.
     */
    private int externalId;
    /**
     * The 'picture id', identifying the picture number to be loaded. Usually
     * equal to the internal id, but different in case of graphical variants
     * (such as the 18EU tiles 80-83).
     */
    private int pictureId;
    private String name;
    private String colourName; // May become a separate class TileType
    private int colourNumber;

    private final List<Upgrade> upgrades = new ArrayList<Upgrade>(); // Contains
    // Upgrade
    // instances
    private String upgradesString = "";
    private final List[] tracksPerSide = new ArrayList[6]; // Cannot
    // parametrise
    // collection array
    private Map<Integer, List<Track>> tracksPerStation = null;
    private final List<Track> tracks = new ArrayList<Track>();
    private final List<Station> stations = new ArrayList<Station>();
    private static final Pattern sidePattern = Pattern.compile("side(\\d+)");
    private static final Pattern cityPattern = Pattern.compile("city(\\d+)");
    private int quantity;
    private boolean unlimited = false;
    public static final int UNLIMITED_TILES = -1;

    /** Off-board preprinted tiles */
    public static final String RED_COLOUR_NAME = "red";
    public static final int RED_COLOUR_NUMBER = -2;
    /** Non-upgradeable preprinted tiles (colour grey or dark brown) */
    public static final String FIXED_COLOUR_NAME = "fixed";
    public static final int FIXED_COLOUR_NUMBER = -1;
    /** Preprinted pre-yellow tiles */
    public static final String WHITE_COLOUR_NAME = "white";
    public static final int WHITE_COLOUR_NUMBER = 0;
    public static final String YELLOW_COLOPUR_NAME = "yellow";
    public static final int YELLOW_COLOUR_NUMBER = 1;
    public static final String GREEN_COLOUR_NAME = "green";
    public static final int GREEN_COLOUR_NUMBER = 2;
    public static final String BROWN_COLOUR_NAME = "brown";
    public static final int BROWN_COLOUR_NUMBER = 3;
    public static final String GREY_COLOUR_NAME = "grey";
    public static final int GREY_COLOUR_NUMBER = 4;

    protected static final List<String> VALID_COLOUR_NAMES =
            Arrays.asList(new String[] { RED_COLOUR_NAME, FIXED_COLOUR_NAME,
                    WHITE_COLOUR_NAME, YELLOW_COLOPUR_NAME, GREEN_COLOUR_NAME,
                    BROWN_COLOUR_NAME, GREY_COLOUR_NAME });

    /**
     * The offset to convert tile numbers to tilename index. Colour number 0 and
     * higher are upgradeable.
     */

    protected static final int TILE_NUMBER_OFFSET = 2;

    private final ArrayList<MapHex> tilesLaid = new ArrayList<MapHex>();

    public Tile(Integer id) {
        this.id = id;
        externalId = pictureId = id;
        name = "" + this.id;

        for (int i = 0; i < 6; i++)
            tracksPerSide[i] = new ArrayList<Track>();
    }

    /**
     * @param se &lt;Tile&gt; element from TileSet.xml
     * @param te &lt;Tile&gt; element from Tiles.xml
     */
    @SuppressWarnings("unchecked")
    public void configureFromXML(Tag setTag, Tag defTag)
            throws ConfigurationException {

        if (defTag == null) {
            throw new ConfigurationException(LocalText.getText("TileMissing",
                    String.valueOf(id)));
        }

        name = defTag.getAttributeAsString("name", name);

        colourName = defTag.getAttributeAsString("colour");
        if (colourName == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileColorMissing", String.valueOf(id)));
        colourName = colourName.toLowerCase();
        if (colourName.equals("gray")) colourName = "grey";
        colourNumber = VALID_COLOUR_NAMES.indexOf(colourName);
        if (colourNumber < 0) {
            throw new ConfigurationException(LocalText.getText(
                    "InvalidTileColourName", new String[] { name, colourName }));
        }
        colourNumber -= TILE_NUMBER_OFFSET;

        /* Stations */
        List<Tag> stationTags = defTag.getChildren("Station");
        Map<String, Station> stationMap = new HashMap<String, Station>();
        if (stationTags != null) {
            tracksPerStation = new HashMap<Integer, List<Track>>();
            String sid, type;
            int number, value, slots, position;
            Station station;
            for (Tag stationTag : stationTags) {
                sid = stationTag.getAttributeAsString("id");
                if (sid == null)
                    throw new ConfigurationException(LocalText.getText(
                            "TileStationHasNoID", String.valueOf(id)));
                number = -getPointNumber(sid);
                type = stationTag.getAttributeAsString("type");
                if (type == null)
                    throw new ConfigurationException(LocalText.getText(
                            "TileStationHasNoType", String.valueOf(id)));
                if (!Station.isTypeValid(type)) {
                    throw new ConfigurationException(LocalText.getText(
                            "TileStationHasInvalidType", new String[] {
                                    String.valueOf(id), type }));
                }
                value = stationTag.getAttributeAsInteger("value", 0);
                // log.debug("Tile #"+id+" st."+number+" value="+value);
                slots = stationTag.getAttributeAsInteger("slots", 0);
                position = stationTag.getAttributeAsInteger("position", 0);
                station =
                        new Station(this, number, sid, type, value, slots,
                                position);
                stations.add(station);
                stationMap.put(sid, station);
            }
        }

        /* Tracks (only number per side, no cities yet) */
        List<Tag> trackTags = defTag.getChildren("Track");
        if (trackTags != null) {
            Track track;
            int from, to;
            String fromStr, toStr;
            for (Tag trackTag : trackTags) {
                fromStr = trackTag.getAttributeAsString("from");
                toStr = trackTag.getAttributeAsString("to");
                if (fromStr == null || toStr == null) {
                    throw new ConfigurationException(LocalText.getText(
                            "FromOrToMissing", String.valueOf(id)));
                }

                from = getPointNumber(fromStr);
                to = getPointNumber(toStr);
                track = new Track(from, to);
                tracks.add(track);
                if (from >= 0) {
                    tracksPerSide[from].add(track);
                } else {
                    if (tracksPerStation.get(-from) == null) {
                        tracksPerStation.put(-from, new ArrayList<Track>(4));
                    }
                    tracksPerStation.get(-from).add(track);
                }
                if (to >= 0) {
                    tracksPerSide[to].add(track);
                } else {
                    if (tracksPerStation.get(-to) == null) {
                        tracksPerStation.put(-to, new ArrayList<Track>(4));
                    }
                    tracksPerStation.get(-to).add(track);
                }
            }
        }

        /* External (printed) id */
        externalId = setTag.getAttributeAsInteger("extId", externalId);
        /* Picture id */
        pictureId = setTag.getAttributeAsInteger("pic", pictureId);
        /* Quantity */
        quantity = setTag.getAttributeAsInteger("quantity", 0);
        /* Value '99' and '-1' mean 'unlimited' */
        unlimited = (quantity == 99 || quantity == UNLIMITED_TILES);
        if (unlimited) quantity = UNLIMITED_TILES;

        /* Upgrades */
        List<Tag> upgradeTags = setTag.getChildren("Upgrade");

        if (upgradeTags != null) {
            String ids;
            int id;
            String[] idArray;
            TileI upgradeTile;
            Upgrade upgrade;
            String hexes;

            for (Tag upgradeTag : upgradeTags) {
                ids = upgradeTag.getAttributeAsString("id");
                upgradesString = ids; // TEMPORARY
                List<Upgrade> newUpgrades = new ArrayList<Upgrade>();

                if (ids != null) {
                    idArray = ids.split(",");
                    for (int j = 0; j < idArray.length; j++) {
                        try {
                            id = Integer.parseInt(idArray[j]);
                            upgradeTile = TileManager.get().getTile(id);
                            if (upgradeTile != null) {
                                upgrade = new Upgrade(upgradeTile);
                                upgrades.add(upgrade);
                                newUpgrades.add(upgrade);
                            } else {
                                throw new ConfigurationException(
                                        LocalText.getText("UpgradeNotFound",
                                                new String[] { name,
                                                        String.valueOf(id) }));
                            }
                        } catch (NumberFormatException e) {
                            throw new ConfigurationException(LocalText.getText(
                                    "NonNumericUpgrade", new String[] { name,
                                            idArray[j] }), e);
                        }

                    }

                }

                // Process any included or excluded hexes for the current set of
                // upgrades
                hexes = upgradeTag.getAttributeAsString("hex");
                if (hexes != null) {
                    for (Upgrade newUpgrade : newUpgrades) {
                        newUpgrade.setHexes(hexes);
                    }

                }
            }
        }

    }

    /**
     * @return Returns the colour.
     */
    public String getColourName() {
        return colourName;
    }

    public int getColourNumber() {
        return colourNumber;
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    public int getExternalId() {
        return externalId;
    }

    public int getPictureId() {
        return pictureId;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    private int getPointNumber(String trackEnd) throws ConfigurationException {

        Matcher m;
        if ((m = sidePattern.matcher(trackEnd)).matches()) {
            return (Integer.parseInt(m.group(1)) + 3) % 6;
        } else if ((m = cityPattern.matcher(trackEnd)).matches()) {
            return -Integer.parseInt(m.group(1));
        }
        // Should add some validation!
        throw new ConfigurationException(LocalText.getText("InvalidTrackEnd")
                                         + ": " + trackEnd);
    }

    public boolean hasTracks(int sideNumber) {
        while (sideNumber < 0)
            sideNumber += 6;
        return (tracksPerSide[sideNumber % 6].size() > 0);
    }

    @SuppressWarnings("unchecked")
    public List<Track> getTracksPerSide(int sideNumber) {
        while (sideNumber < 0)
            sideNumber += 6;
        return tracksPerSide[sideNumber % 6];
    }

    /**
     * Is a tile upgradeable at any time (regardles the phase)?
     */
    public boolean isUpgradeable() {
        return colourNumber >= 0;
    }

    /**
     * Get the valid upgrades if this tile on a certain hex (restrictions per
     * hex have not yet been implemented).
     * 
     * @param hex The MapHex to be upgraded.
     * @return A List of valid upgrade TileI objects.
     */
    public List<TileI> getUpgrades(MapHex hex) {
        List<TileI> upgr = new ArrayList<TileI>();
        TileI tile;
        for (Upgrade upgrade : upgrades) {
            tile = upgrade.getTile();
            if (hex == null || upgrade.isAllowedForHex(hex)) upgr.add(tile);
        }
        return upgr;
    }

    public String getUpgradesString(MapHex hex) {
        return upgradesString;
    }

    public List<TileI> getValidUpgrades(MapHex hex, PhaseI phase) {
        List<TileI> valid = new ArrayList<TileI>();
        TileI tile;

        for (Upgrade upgrade : upgrades) {
            tile = upgrade.getTile();
            if (phase.isTileColourAllowed(tile.getColourName())
                && tile.countFreeTiles() != 0 /* -1 means unlimited */
                && upgrade.isAllowedForHex(hex)) {
                valid.add(tile);
            }
        }
        return valid;
    }

    public boolean hasStations() {
        return stations.size() > 0;
    }

    public List<Station> getStations() {
        return stations;
    }

    public Map<Integer, List<Track>> getTracksPerStationMap() {
        return tracksPerStation;
    }

    public List<Track> getTracksPerStation(int stationNumber) {
        return tracksPerStation.get(stationNumber);
    }

    public int getNumStations() {
        return stations.size();
    }

    public boolean lay(MapHex hex) {

        tilesLaid.add(hex);
        update();

        return true;
    }

    public boolean remove(MapHex hex) {

        tilesLaid.remove(hex);
        update();
        return true;
    }

    /** Return the number of free tiles */
    public int countFreeTiles() {
        if (unlimited)
            return UNLIMITED_TILES;
        else
            return quantity - tilesLaid.size();
    }

    /** Return a caption for the Remaining Tiles window */
    @Override
    public String getText() {

        String count = unlimited ? "+" : String.valueOf(countFreeTiles());
        return "#" + externalId + ": " + count;
    }

    public int getQuantity() {
        return quantity;
    }

    protected class Upgrade {

        /** The upgrade tile */
        TileI tile;

        /** Hexes where the upgrade can be executed */
        List<MapHex> allowedHexes = null;
        /**
         * Hexes where the upgrade cannot be executed Only one of allowedHexes
         * and disallowedHexes should be used
         */
        List<MapHex> disallowedHexes = null;

        /**
         * A temporary String holding the in/excluded hexes. This will be
         * processed at the first usage, because Tiles are initialised before
         * the Map.
         * 
         * @author Erik Vos
         */
        String hexes = null;

        protected Upgrade(TileI tile) {
            this.tile = tile;
        }

        protected boolean isAllowedForHex(MapHex hex) {

            if (hexes != null) convertHexString();

            if (allowedHexes != null) {
                return allowedHexes.contains(hex);
            } else if (disallowedHexes != null) {
                return !disallowedHexes.contains(hex);
            } else {
                return true;
            }
        }

        protected TileI getTile() {
            return tile;
        }

        protected void setHexes(String hexes) {
            this.hexes = hexes;
        }

        private void convertHexString() {

            boolean allowed = !hexes.startsWith("-");
            if (!allowed) hexes = hexes.substring(1);
            String[] hexArray = hexes.split(",");
            MapHex hex;
            for (int i = 0; i < hexArray.length; i++) {
                hex = MapManager.getInstance().getHex(hexArray[i]);
                if (hex != null) {
                    if (allowed) {
                        if (allowedHexes == null)
                            allowedHexes = new ArrayList<MapHex>();
                        allowedHexes.add(hex);
                    } else {
                        if (disallowedHexes == null)
                            disallowedHexes = new ArrayList<MapHex>();
                        disallowedHexes.add(hex);
                    }
                }
            }
            hexes = null; // Do this only once
        }
    }
}
