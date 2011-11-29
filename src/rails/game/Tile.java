package rails.game;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rails.algorithms.RevenueBonusTemplate;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.Stop.Loop;
import rails.game.Stop.RunThrough;
import rails.game.Stop.RunTo;
import rails.game.Stop.Score;
import rails.game.Stop.Type;
import rails.game.state.HashSetState;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.util.Util;

/** Represents a certain tile <i>type</i>, identified by its id (tile number).
 * <p> For each tile number, only one tile object is created.
 * The list <b>tilesLaid</b> records in which hexes a certain tile number has been laid.
 *
 *
 * TODO: A lot could be improved
 */
public final class Tile extends Model implements RailsItem, Comparable<Tile> {

    /** The 'internal id', identifying the tile in the XML files */
    private final int nb;
    /**
     * The 'external id', which is shown in the UI. Usually equal to the
     * internal id, but different in case of duplicates.
     */
    private String externalId;
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
    // Upgrade instances
    //private String upgradesString = "";
    // TODO: Replace side by objects and then keep tracks there
    @SuppressWarnings("rawtypes")
    private final List[] tracksPerSide = new ArrayList[6];
    // N.B. Cannot parametrise collection array

    // TODO: Move that to station class
    private Map<Integer, List<Track>> tracksPerStation = null;
    private final List<Track> tracks = new ArrayList<Track>();
    private final List<Station> stations = new ArrayList<Station>();

    private static final Pattern sidePattern = Pattern.compile("side(\\d+)");
    private static final Pattern cityPattern = Pattern.compile("city(\\d+)");
    
    private int quantity;
    private boolean unlimited = false;
    private boolean allowsMultipleBasesOfOneCompany = false;
    /** Fixed orientation; -1 if free to rotate */
    private int fixedOrientation = -1;

    public static final int UNLIMITED_TILES = -1;

    // Stop properties
    private Type stopType = null;
    private RunTo runToAllowed = null;
    private RunThrough runThroughAllowed = null;
    private Loop loopAllowed = null;
    private Score scoreType = null;

    /**
     * Flag indicating that player must reposition any basetokens during the upgrade.
     */
    private boolean relayBaseTokensOnUpgrade = false;

    // TODO: Replace TileColors as Strings with a TileColor enum
    
    /** Off-board preprinted tiles */
    public static final String RED_COLOUR_NAME = "red";
    public static final int RED_COLOUR_NUMBER = -2;
    /** Non-upgradeable preprinted tiles (colour grey or dark brown) */
    public static final String FIXED_COLOUR_NAME = "fixed";
    public static final int FIXED_COLOUR_NUMBER = -1;
    /** Preprinted pre-yellow tiles */
    public static final String WHITE_COLOUR_NAME = "white";
    public static final int WHITE_COLOUR_NUMBER = 0;
    public static final String YELLOW_COLOUR_NAME = "yellow";
    public static final int YELLOW_COLOUR_NUMBER = 1;
    public static final String GREEN_COLOUR_NAME = "green";
    public static final int GREEN_COLOUR_NUMBER = 2;
    public static final String BROWN_COLOUR_NAME = "brown";
    public static final int BROWN_COLOUR_NUMBER = 3;
    public static final String GREY_COLOUR_NAME = "grey";
    public static final int GREY_COLOUR_NUMBER = 4;

    
    protected static final List<String> VALID_COLOUR_NAMES =
        Arrays.asList(new String[] { RED_COLOUR_NAME, FIXED_COLOUR_NAME,
                WHITE_COLOUR_NAME, YELLOW_COLOUR_NAME, GREEN_COLOUR_NAME,
                BROWN_COLOUR_NAME, GREY_COLOUR_NAME });

    /**
     * The offset to convert tile numbers to tilename index. Colour number 0 and
     * higher are upgradeable.
     */

    private static final int TILE_NUMBER_OFFSET = 2;

    /** Records in which hexes a certain tile number has been laid.
     * The size of the collection indicates the number of tiles laid on the map board.
     */
    private final HashSetState<MapHex> tilesLaid = HashSetState.create(this, "tilesLaid");

    /** Storage of revenueBonus that are bound to the tile */
    private List<RevenueBonusTemplate> revenueBonuses = null;

    private Tile(Item owner, Integer id) {
        super(owner, Integer.toString(id));
        this.nb = id;
        pictureId = id;
        externalId = String.valueOf(id);
        name = "" + this.nb;

        for (int i = 0; i < 6; i++)
            tracksPerSide[i] = new ArrayList<Track>();
    }
    
    public static Tile create(TileManager parent, Integer id) {
        return new Tile(parent, id);
    }
    
    @Override
    public TileManager getParent() {
        return (TileManager)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getParent();
    }
    
    /**
     * @param se &lt;Tile&gt; element from TileSet.xml
     * @param te &lt;Tile&gt; element from Tiles.xml
     */
    // TODO: Warning due to list array 
    @SuppressWarnings("unchecked")
    public void configureFromXML(Tag setTag, Tag defTag)
    throws ConfigurationException {

        if (defTag == null) {
            throw new ConfigurationException(LocalText.getText("TileMissing",
                    String.valueOf(nb)));
        }

        name = defTag.getAttributeAsString("name", name);

        colourName = defTag.getAttributeAsString("colour");
        if (colourName == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileColorMissing", String.valueOf(nb)));
        colourName = colourName.toLowerCase();
        if (colourName.equals("gray")) colourName = "grey";
        colourNumber = VALID_COLOUR_NAMES.indexOf(colourName);
        if (colourNumber < 0) {
            throw new ConfigurationException(LocalText.getText(
                    "InvalidTileColourName",
                    name,
                    colourName ));
        }
        colourNumber -= TILE_NUMBER_OFFSET;

        /* Stations */
        List<Tag> stationTags = defTag.getChildren("Station");
        Map<String, Station> stationMap = new HashMap<String, Station>();
        if (stationTags != null) {
            tracksPerStation = new HashMap<Integer, List<Track>>();
            String sid, type, cityName;
            int number, value, slots, position;
            Station station;
            for (Tag stationTag : stationTags) {
                sid = stationTag.getAttributeAsString("id");
                if (sid == null)
                    throw new ConfigurationException(LocalText.getText(
                            "TileStationHasNoID", String.valueOf(nb)));
                number = -getPointNumber(sid);
                type = stationTag.getAttributeAsString("type");
                if (type == null)
                    throw new ConfigurationException(LocalText.getText(
                            "TileStationHasNoType", String.valueOf(nb)));
                if (!Station.isTypeValid(type)) {
                    throw new ConfigurationException(LocalText.getText(
                            "TileStationHasInvalidType",
                            nb,
                            type ));
                }
                value = stationTag.getAttributeAsInteger("value", 0);
                slots = stationTag.getAttributeAsInteger("slots", 0);
                position = stationTag.getAttributeAsInteger("position", 0);
                cityName = stationTag.getAttributeAsString("city");
                station =
                    new Station(this, number, sid, type, value, slots,
                            position, cityName);
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
                            "FromOrToMissing", String.valueOf(nb)));
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
        externalId = setTag.getAttributeAsString("extId", externalId);
        /* Picture id */
        pictureId = setTag.getAttributeAsInteger("pic", pictureId);

        /* Quantity */
        quantity = setTag.getAttributeAsInteger("quantity", 0);
        /* Value '99' and '-1' mean 'unlimited' */
        /* BR: added option for unlimited plain tiles: tiles with one track and no stations */
        unlimited = (quantity == 99 || quantity == UNLIMITED_TILES
                || "yes".equalsIgnoreCase(setTag.getGameOptions().get("UnlimitedTiles"))
                || ("yellow plain".equalsIgnoreCase(setTag.getGameOptions().get("UnlimitedTiles"))
                        && tracks.size() == 1 && stations.isEmpty()));
        if (unlimited) {
            quantity = UNLIMITED_TILES;
        } else {
            quantity += setTag.getAttributeAsInteger("quantityIncrement", 0);
        }

        /* Multiple base tokens of one company allowed */
        allowsMultipleBasesOfOneCompany = setTag.hasChild(
        "AllowsMultipleBasesOfOneCompany");

        fixedOrientation = setTag.getAttributeAsInteger("orientation", fixedOrientation);

        /* Upgrades */
        List<Tag> upgradeTags = setTag.getChildren("Upgrade");

        if (upgradeTags != null) {
            String ids;
            int id;
            String[] idArray;
            Upgrade upgrade;
            String hexes;

            for (Tag upgradeTag : upgradeTags) {
                ids = upgradeTag.getAttributeAsString("id");
                //upgradesString = ids; // TEMPORARY
                List<Upgrade> newUpgrades = new ArrayList<Upgrade>();

                if (ids != null) {
                    idArray = ids.split(",");
                    for (int j = 0; j < idArray.length; j++) {
                        try {
                            id = Integer.parseInt(idArray[j]);
                            upgrade = new Upgrade(id);
                            upgrades.add(upgrade);
                            newUpgrades.add(upgrade);
                        } catch (NumberFormatException e) {
                            throw new ConfigurationException(LocalText.getText(
                                    "NonNumericUpgrade",
                                    name,
                                    idArray[j] ), e);
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

                // Process any phases in which the upgrade is allowed
                String phases = upgradeTag.getAttributeAsString("phase");
                if (phases != null) {
                    for (Upgrade newUpgrade : newUpgrades) {
                        newUpgrade.setPhases(phases);
                    }

                }

                // Set reposition base tokens flag
                // (valid for all upgrades although attribute of one)
                relayBaseTokensOnUpgrade = upgradeTag.getAttributeAsBoolean(
                        "relayBaseTokens",
                        relayBaseTokensOnUpgrade);
            }
        }

        // revenue bonus
        List<Tag> bonusTags = setTag.getChildren("RevenueBonus");
        if (bonusTags != null) {
            revenueBonuses = new ArrayList<RevenueBonusTemplate>();
            for (Tag bonusTag:bonusTags) {
                RevenueBonusTemplate bonus = new RevenueBonusTemplate();
                bonus.configureFromXML(bonusTag);
                revenueBonuses.add(bonus);
            }
        }

        // Stop properties
        Tag accessTag = setTag.getChild("Access");
        if (accessTag != null) {
            String runThroughString = accessTag.getAttributeAsString("runThrough");
            if (Util.hasValue(runThroughString)) {
                try {
                    runThroughAllowed = RunThrough.valueOf(runThroughString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for Tile"
                            +name+" runThrough property: "+runThroughString, e);
                }
            }

            String runToString = accessTag.getAttributeAsString("runTo");
            if (Util.hasValue(runToString)) {
                try {
                    runToAllowed = RunTo.valueOf(runToString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for Tile "
                            +name+" runTo property: "+runToString, e);
                }
            }

            String loopString = accessTag.getAttributeAsString("loop");
            if (Util.hasValue(loopString)) {
                try {
                    loopAllowed = Loop.valueOf(loopString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for Tile "
                            +name+" loop property: "+loopString, e);
                }
            }

            String typeString = accessTag.getAttributeAsString("type");
            if (Util.hasValue(typeString)) {
                try {
                    stopType = Type.valueOf(typeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for Tile "
                            +name+" stop type property: "+typeString, e);
                }
            }

            String scoreTypeString = accessTag.getAttributeAsString("score");
            if (Util.hasValue(scoreTypeString)) {
                try {
                    scoreType = Score.valueOf(scoreTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for Tile "
                            +name+" sscore type property: "+scoreTypeString, e);
                }
            }
        }
    }

    public void finishConfiguration (TileManager tileManager)
    throws ConfigurationException {

        for (Upgrade upgrade : upgrades) {

            Tile tile = tileManager.getTile(upgrade.getTileId());
            if (tile != null) {
                upgrade.setTile(tile);
            } else {
                throw new ConfigurationException ("Cannot find upgrade tile #"
                        +upgrade.getTileId()+" for tile #"+nb);
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

    public static int getColourNumberForName(String name) {
        if (name.equalsIgnoreCase(WHITE_COLOUR_NAME)) {
            return WHITE_COLOUR_NUMBER;
        } else if (name.equalsIgnoreCase(YELLOW_COLOUR_NAME)) {
            return YELLOW_COLOUR_NUMBER;
        } else if (name.equalsIgnoreCase(GREEN_COLOUR_NAME)) {
            return GREEN_COLOUR_NUMBER;
        } else if (name.equalsIgnoreCase(BROWN_COLOUR_NAME)) {
            return BROWN_COLOUR_NUMBER;
        } else if (name.equalsIgnoreCase(GREY_COLOUR_NAME)) {
            return GREY_COLOUR_NUMBER;
        } else {
            return -1;
        }
    }
    /**
     * @return Returns the id.
     */
    public int getNb() {
        return nb;
    }

    public String getExternalId() {
        return externalId;
    }

    public int getPictureId() {
        return pictureId;
    }

    /**
     * @return Returns the name.
     */
    public String getId() {
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

    public boolean allowsMultipleBasesOfOneCompany() {
        return allowsMultipleBasesOfOneCompany;
    }

    /**
     * Get the valid upgrades if this tile on a certain hex (restrictions per
     * hex have not yet been implemented).
     *
     * @param hex The MapHex to be upgraded.
     * @return A List of valid upgrade Tile objects.
     */
    public List<Tile> getUpgrades(MapHex hex, Phase phase) {
        List<Tile> upgr = new ArrayList<Tile>();
        Tile tile;
        for (Upgrade upgrade : upgrades) {
            tile = upgrade.getTile();
            if (hex == null || upgrade.isAllowedForHex(hex, phase.getName())) upgr.add(tile);
        }
        return upgr;
    }

    /**
     * Get all possible upgrades for a specific tile on a certain hex
     *
     * @return A List of valid upgrade Tile objects.
     */

    public List<Tile> getAllUpgrades(MapHex hex) {
        List<Tile> upgr = new ArrayList<Tile>();
        for (Upgrade upgrade : upgrades) {
            Tile tile = upgrade.getTile();
            if (upgrade.isAllowedForHex(hex)) {
                upgr.add(tile);
            }
        }
        return upgr;
    }

    /** Get a delimited list of all possible upgrades, regardless current phase */
    public String getUpgradesString(MapHex hex) {
        StringBuffer b = new StringBuffer();
        Tile tile;
        for (Upgrade upgrade : upgrades) {
            tile = upgrade.getTile();
            if (upgrade.isAllowedForHex(hex)) {
                if (b.length() > 0) b.append(",");
                b.append(tile.getExternalId());
            }
        }

        return b.toString();
    }

    public List<Tile> getValidUpgrades(MapHex hex, Phase phase) {
        List<Tile> valid = new ArrayList<Tile>();
        Tile tile;

        for (Upgrade upgrade : upgrades) {
            tile = upgrade.getTile();
            if (phase.isTileColourAllowed(tile.getColourName())
                    && tile.countFreeTiles() != 0 /* -1 means unlimited */
                    && upgrade.isAllowedForHex(hex, phase.getName())) {
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

    public List<Track> getTracks() {
        return tracks;
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

    public boolean relayBaseTokensOnUpgrade() {
        return relayBaseTokensOnUpgrade;
    }

    /** Register a tile of this type being laid on the map. */
    public boolean add(MapHex hex) {
        tilesLaid.add(hex);
        return true;
    }

    /** Register a tile of this type being removed from the map. */
    public boolean remove(MapHex hex) {
        return tilesLaid.remove(hex);
    }

    /** Return the number of free tiles */
    public int countFreeTiles() {
        if (unlimited)
            return UNLIMITED_TILES;
        else
            return quantity - tilesLaid.size();
    }

    /** Return a caption for the Remaining Tiles window */
    public String getData() {

        String count = unlimited ? "+" : String.valueOf(countFreeTiles());
        return "#" + externalId + ": " + count;
    }

    // NOT USED
    public int getQuantity() {
        return quantity;
    }

    public int getFixedOrientation () {
        return fixedOrientation;
    }

    public List<RevenueBonusTemplate> getRevenueBonuses() {
        return revenueBonuses;
    }

    @Override
    public String toString() {
        return "Tile Id=" + externalId;
    }

    /** ordering of tiles based first on colour, then on external id */
    public int compareTo(Tile anotherTile) {
        Integer colour = this.getColourNumber();
        int result = colour.compareTo(anotherTile.getColourNumber());
        if (result == 0) {
            //String externalId  = this.getExternalId();
            result = externalId.compareTo(anotherTile.getExternalId());
        }
        return result;
    }


    public Type getStopType() {
        return stopType;
    }

    public RunTo isRunToAllowed() {
        return runToAllowed;
    }

    public RunThrough isRunThroughAllowed() {
        return runThroughAllowed;
    }

    public Loop isLoopAllowed() {
        return loopAllowed;
    }

    public Score getScoreType() {
        return scoreType;
    }

    protected class Upgrade {

        /** The upgrade tile id */
        int tileId;

        /** The upgrade tile */
        Tile tile;

        /** Hexes where the upgrade can be executed */
        List<MapHex> allowedHexes = null;
        /**
         * Hexes where the upgrade cannot be executed Only one of allowedHexes
         * and disallowedHexes should be used
         */
        List<MapHex> disallowedHexes = null;
        /**
         * Phases in which the upgrade can be executed.
         */
        List<String> allowedPhases = null;

        /**
         * A temporary String holding the in/excluded hexes. This will be
         * processed at the first usage, because Tiles are initialised before
         * the Map.
         *
         * @author Erik Vos
         */
        String hexes = null;

        protected Upgrade(int tileId) {
            this.tileId = tileId;
        }

        protected boolean isAllowedForHex(MapHex hex, String phaseName) {

            if (allowedPhases != null
                    && !allowedPhases.contains(phaseName)) {
                return false;
            }

            if (hexes != null) convertHexString(hex.getParent());

            if (allowedHexes != null) {
                return allowedHexes.contains(hex);
            } else if (disallowedHexes != null) {
                return !disallowedHexes.contains(hex);
            } else {
                return true;
            }
        }

        protected boolean isAllowedForHex(MapHex hex) {

            if (hexes != null) convertHexString(hex.getParent());

            if (allowedHexes != null) {
                return allowedHexes.contains(hex);
            } else if (disallowedHexes != null) {
                return !disallowedHexes.contains(hex);
            } else {
                return true;
            }
        }

        public void setTile(Tile tile) {
            this.tile = tile;
        }

        protected Tile getTile() {
            return tile;
        }

        public int getTileId() {
            return tileId;
        }

        protected void setHexes(String hexes) {
            this.hexes = hexes;
        }

        protected void setPhases (String phases) {
            allowedPhases = Arrays.asList(phases.split(","));
        }

        private void convertHexString(MapManager mapManager) {

            boolean allowed = !hexes.startsWith("-");
            if (!allowed) hexes = hexes.substring(1);
            String[] hexArray = hexes.split(",");
            MapHex hex;
            for (int i = 0; i < hexArray.length; i++) {
                hex = mapManager.getHex(hexArray[i]);
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
