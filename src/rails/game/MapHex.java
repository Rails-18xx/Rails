package rails.game;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import rails.algorithms.RevenueBonusTemplate;
import rails.common.LocalText;
import rails.common.parser.*;
import rails.game.Stop.Loop;
import rails.game.Stop.RunThrough;
import rails.game.Stop.RunTo;
import rails.game.Stop.Score;
import rails.game.Stop.Type;
import rails.game.action.LayTile;
import rails.game.model.CashMoneyModel;
import rails.game.model.PortfolioModel;

import rails.game.state.BooleanState;
import rails.game.state.Observer;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioHolder;
import rails.game.state.PortfolioList;
import rails.game.state.AbstractItem;
import rails.util.*;

// TODO: Rewrite the mechanisms for tokens
// TODO: Rewrite the mechanisms as model
// FIXME: There is a lot to be done here


/**
 * Represents a Hex on the Map from the Model side.
 *
 * <p> The term "rotation" is used to indicate the amount of rotation (in 60
 * degree units) from the standard orientation of the tile (sometimes the term
 * orientation is also used to refer to rotation).
 * <p>Rotation is always relative to the standard orientation, which has the
 * printed tile number on the S edge for {@link TileOrientation#NS}-oriented
 * tiles, or on the SW edge for {@link TileOrientation#EW}-oriented tiles. The
 * rotation numbers are indicated in the below picture for an
 * {@code NS}-oriented tile: <p> <code>
 *
 *       ____3____
 *      /         \
 *     2           4
 *    /     NS      \
 *    \             /
 *     1           5
 *      \____0____/
 * </code> <p> For {@code EW}-oriented
 * tiles the above picture should be rotated 30 degrees clockwise.
 */

// FIXME: MapHex was previous a model
public class MapHex extends AbstractItem implements PortfolioHolder, ConfigurableComponent,
StationHolder {

    private static final String[] ewOrNames =
    { "SW", "W", "NW", "NE", "E", "SE" };
    private static final String[] nsOrNames =
    { "S", "SW", "NW", "N", "NE", "SE" };

    // Coordinates as used in the rails.ui.swing.hexmap package
    protected int x;
    protected int y;

    // Map coordinates as printed on the rails.game board
    protected String name;
    protected int row;
    protected int column;
    protected int letter;
    protected int number;
    protected String tileFileName;
    protected int preprintedTiled;
    protected Tile currentTile;
    protected int currentTileRotation;
    protected int preprintedTileRotation;
    protected int[] tileCost;
    protected String cityName;
    protected String infoText;
    protected String reservedForCompany = null;

    /** Neighbouring hexes <i>to which track may be laid</i>. */
    protected MapHex[] neighbours = new MapHex[6];

    /** Values if this is an off-board hex */
    protected int[] valuesPerPhase = null;

    /*
     * Temporary storage for impassable hexsides. Once neighbours has been set
     * up, this attribute is no longer used. Only the black or blue bars on the
     * map need be specified, and each one only once. Impassable non-track sides
     * of "offboard" (red) and "fixed" (grey or brown) preprinted tiles will be
     * derived and need not be specified.
     */
    protected String impassable = null;
    protected List<Integer> impassableSides;

    protected List<Stop> stops;
    protected Map<Integer, Stop> mStops;

    /*
     * changed to state variable to fix undo bug #2954645
     * null as default implies false - see isBlocked()
     */
    private final BooleanState isBlockedForTileLays = BooleanState.create(this, "isBlockedForTileLays");

    /**
     * Is the hex initially blocked for token lays (e.g. when a home base
     * must first be laid)? <p>
     * NOTE:<br>null means: blocked unless there is more free space than unlaid home bases,<br>
     * false means: blocked unless there is any free space.<br>
     * This makes a difference for 1835 Berlin, which is home to PR, but
     * the absence of a PR token does not block the third slot
     * when the green tile is laid.
     */
    private final BooleanState isBlockedForTokenLays = BooleanState.create(this, "isBlockedForTokenLays");

    protected Map<PublicCompany, Stop> homes;
    protected List<PublicCompany> destinations;

    /** Tokens that are not bound to a Station (City), such as Bonus tokens */
    protected final PortfolioList<Token> offStationTokens = PortfolioList.create(this, "offStationTokens");

    /** Storage of revenueBonus that are bound to the hex */
    protected List<RevenueBonusTemplate> revenueBonuses = null;

    /** Any open sides against which track may be laid even at board edges (1825) */
    protected boolean[] openHexSides;

    /** Run-through status of any stops on the hex (whether visible or not).
     * Indicates whether or not a single train can run through such stops, i.e. both enter and leave it.
     * Has no meaning if no stops exist on this hex.
     * <p>Values (see RunThrough below for definitions):
     * <br>- "yes" (default for all except off-map hexes) means that trains of all companies
     * may run through this station, unless it is completely filled with foreign base tokens.
     * <br>- "tokenOnly" means that trains may only run through the station if it contains a base token
     * of the operating company (applies to the 1830 PRR base).
     * <br>- "no" (default for off-map hexes) means that no train may run through this hex.
     */
    protected RunThrough runThroughAllowed = null;

    /** Run-to status of any stops on the hex (whether visible or not).
     * Indicates whether or not a single train can run from or to such stops, i.e. either enter or leave it.
     * Has no meaning if no stops exist on this hex.
     * <p>Values (see RunTo below for definitions):
     * <br>- "yes" (default) means that trains of all companies may run to/from this station.
     * <br>- "tokenOnly" means that trains may only access the station if it contains a base token
     * of the operating company. Applies to the 18Scan off-map hexes.
     * <br>- "no" would mean that the hex is inaccessible (like 1851 Birmingham in the early game),
     * but this option is not yet useful as there is no provision yet to change this setting
     * in an undoable way (no state variable).
     */
    protected RunTo runToAllowed = null;

    /** Loop: may one train touch this hex twice or more? */
    protected Loop loopAllowed = null;

    /** Type of any stops on the hex.
     * Normally the type will be derived from the tile properties.
     */
    protected Type stopType = null;

    /**
     * Score type: do stops on this hex count as major or minor stops with respect to n+m trains?
     */
    protected Score scoreType = null;

    protected static Logger log =
        LoggerFactory.getLogger(MapHex.class.getPackage().getName());

    private MapHex(MapManager parent, String id) {
        super(parent, id);
    }

    public static MapHex create(MapManager parent, Tag tag) throws ConfigurationException {
        // TODO: Rewrite the creation process of MapHex
        MapHex hex = new MapHex(parent, null);
        hex.configureFromXML(tag);
        return hex;
    }

    /**
     * @see rails.common.parser.ConfigurableComponent#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        Pattern namePattern = Pattern.compile("(\\D+?)(-?\\d+)");

        infoText = name = tag.getAttributeAsString("name");
        Matcher m = namePattern.matcher(name);
        if (!m.matches()) {
            throw new ConfigurationException("Invalid name format: " + name);
        }
        String letters = m.group(1);
        if (letters.length() == 1) {
            letter = letters.charAt(0);
        } else { // for row 'AA' in 1825U1
            letter = 26 + letters.charAt(1);
        }
        try {
            number = Integer.parseInt(m.group(2));
            if (number > 90) number -= 100;  // For 1825U1 column 99 (= -1)
        } catch (NumberFormatException e) {
            // Cannot occur!
        }

        /*
         * Translate hex names (as on the board) to coordinates used for
         * drawing.
         */
        if (lettersGoHorizontal()) {
            row = number;
            column = letter - '@';
            if (getTileOrientation() == TileOrientation.EW) {
                // Tiles with flat EW sides, letters go horizontally.
                // Example: 1841 (NOT TESTED, PROBABLY WRONG).
                x = column;
                y = row / 2;
            } else {
                // Tiles with flat NS sides, letters go horizontally.
                // Tested for 1856.
                x = column;
                y = (row + 1) / 2;
            }
        } else
            // letters go vertical (normal case)
        {
            row = letter - '@';
            column = number;
            if (getTileOrientation() == TileOrientation.EW) {
                // Tiles with flat EW sides, letters go vertically.
                // Most common case.
                // Tested for 1830 and 1870. OK with 1830 Wabash and 1825R2 (negative column numbers)
                x = (column + 8 + (letterAHasEvenNumbers() ? 1 : 0)) / 2 - 4; // Divisor must be >0
                y = row;
            } else {
                // Tiles with flat NS sides, letters go vertically.
                // Tested for 18AL.
                x = column;
                y = (row + 1) / 2;
            }
        }

        preprintedTiled = tag.getAttributeAsInteger("tile", -999);

        preprintedTileRotation = tag.getAttributeAsInteger("orientation", 0);
        currentTileRotation  = preprintedTileRotation;

        impassable = tag.getAttributeAsString("impassable");
        tileCost = tag.getAttributeAsIntegerArray("cost", new int[0]);

        // Off-board revenue values
        valuesPerPhase = tag.getAttributeAsIntegerArray("value", null);

        // City name
        cityName = tag.getAttributeAsString("city", "");
        if (Util.hasValue(cityName)) {
            infoText += " " + cityName;
        }

        if (tag.getAttributeAsString("unlaidHomeBlocksTokens") != null) {
            setBlockedForTokenLays(tag.getAttributeAsBoolean("unlaidHomeBlocksTokens", false));
        }

        reservedForCompany = tag.getAttributeAsString("reserved");

        // revenue bonus
        List<Tag> bonusTags = tag.getChildren("RevenueBonus");
        if (bonusTags != null) {
            revenueBonuses = new ArrayList<RevenueBonusTemplate>();
            for (Tag bonusTag:bonusTags) {
                RevenueBonusTemplate bonus = new RevenueBonusTemplate();
                bonus.configureFromXML(bonusTag);
                revenueBonuses.add(bonus);
            }
        }

        // Open sides (as in 1825, track may be laid against some board edges)
        for (int side : tag.getAttributeAsIntegerArray("open", new int[0])) {
            if (openHexSides == null) openHexSides = new boolean[6];
            openHexSides[side%6] = true;
        }

        // Stop properties
        Tag accessTag = tag.getChild("Access");
        if (accessTag != null) {
            String runThroughString = accessTag.getAttributeAsString("runThrough");
            if (Util.hasValue(runThroughString)) {
                try {
                    runThroughAllowed = RunThrough.valueOf(runThroughString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for MapHex"
                            +name+" runThrough property: "+runThroughString, e);
                }
            }

            String runToString = accessTag.getAttributeAsString("runTo");
            if (Util.hasValue(runToString)) {
                try {
                    runToAllowed = RunTo.valueOf(runToString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for MapHex "
                            +name+" runTo property: "+runToString, e);
                }
            }

            String loopString = accessTag.getAttributeAsString("loop");
            if (Util.hasValue(loopString)) {
                try {
                    loopAllowed = Loop.valueOf(loopString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for MapHex "
                            +name+" loop property: "+loopString, e);
                }
            }

            String typeString = accessTag.getAttributeAsString("type");
            if (Util.hasValue(typeString)) {
                try {
                    stopType = Type.valueOf(typeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for MapHex "
                            +name+" stop type property: "+typeString, e);
                }
            }

            String scoreTypeString = accessTag.getAttributeAsString("score");
            if (Util.hasValue(scoreTypeString)) {
                try {
                    scoreType = Score.valueOf(scoreTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException ("Illegal value for MapHex "
                            +name+" score type property: "+scoreTypeString, e);
                }
            }
        }

    }

    public void finishConfiguration (GameManager gameManager) {
        if(gameManager == null) {
            throw new IllegalArgumentException("gameManager must not be null");
        }

        currentTile = gameManager.getTileManager().getTile(preprintedTiled);
        // We need completely new objects, not just references to the Tile's
        // stations.
        stops = new ArrayList<Stop>(4);
        mStops = new HashMap<Integer, Stop>(4);
        for (Station s : currentTile.getStations()) {
            // sid, type, value, slots
            Stop c = Stop.create(this, s.getNumber(), s);
            stops.add(c);
            mStops.put(c.getNumber(), c);
        }

        /* Superseded by new code in Stop - or do we still need it?
        if (runThroughAllowed == null) {
            runThroughAllowed = currentTile.getColourName().equalsIgnoreCase("red")
            ? RunThrough.NO : RunThrough.YES;
        }
        if (runToAllowed == null) {
            runToAllowed = RunTo.YES;
        }
         */
    }

    public void addImpassableSide (int orientation) {
        if (impassableSides == null) impassableSides = new ArrayList<Integer>(4);
        impassableSides.add(orientation%6);
    }

    public List<Integer> getImpassableSides () {
        return impassableSides;
    }

    public boolean isImpassable (MapHex neighbour) {
        return impassable != null && impassable.indexOf(neighbour.getId()) > -1;
    }

    public boolean isNeighbour(MapHex neighbour, int direction) {
        /*
         * Various reasons why a bordering hex may not be a neighbour in the
         * sense that track may be laid to that border:
         */
        /* 1. The hex side is marked "impassable" */
        if (impassable != null && impassable.indexOf(neighbour.getId()) > -1)
            return false;
        /*
         * 2. The preprinted tile on this hex is offmap or fixed and has no
         * track to this side.
         */
        Tile tile = neighbour.getCurrentTile();
        if (!tile.isUpgradeable()
                && !tile.hasTracks(3 + direction
                        - neighbour.getCurrentTileRotation()))
            return false;

        return true;
    }

    public boolean isOpenSide (int side) {
        return openHexSides != null && openHexSides[side%6];
    }

    public TileOrientation getTileOrientation() {
        return getParent().getTileOrientation();
    }

    /**
     * @return Returns the letterAHasEvenNumbers.
     */
    public boolean letterAHasEvenNumbers() {
        return getParent().letterAHasEvenNumbers();
    }

    /**
     * @return Returns the lettersGoHorizontal.
     */
    public boolean lettersGoHorizontal() {
        return getParent().lettersGoHorizontal();
    }

    public String getOrientationName(int orientation) {

        if (getTileOrientation() == TileOrientation.EW) {
            return ewOrNames[orientation % 6];
        } else {
            return nsOrNames[orientation % 6];
        }
    }

    /* ----- Instance methods ----- */

    /**
     * @return Returns the column.
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return Returns the row.
     */
    public int getRow() {
        return row;
    }

    
    // TODO: Name and Id are a duplication in MapHex
    // However this has to be removed at the rewrite of creation of MapHex
    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** Add an X offset. Required to avoid negative coordinate values, as arise in 1830 Wabash. */
    public void addX (int offset) {
        x += offset;
    }

    /** Add an Y offset. Required to avoid negative coordinate values. */
    public void addY (int offset) {
        y += offset;
    }

    /**
     * @return Returns the preprintedTiled.
     */
    public int getPreprintedTiled() {
        return preprintedTiled;
    }

    public int getPreprintedTileRotation() {
        return preprintedTileRotation;
    }

    /**
     * @return Returns the image file name for the tile.
     */
    public String getTileFileName() {
        return tileFileName;
    }

    public void setNeighbor(int orientation, MapHex neighbour) {
        orientation %= 6;
        neighbours[orientation] = neighbour;
        //log.debug("+++ Hex="+getName()+":"+orientation+"->"+neighbour.getName());
    }

    public MapHex getNeighbor(int orientation) {
        return neighbours[orientation % 6];
    }

    public MapHex[] getNeighbors() {
        return neighbours;
    }

    public boolean hasNeighbour(int orientation) {

        while (orientation < 0)
            orientation += 6;
        return neighbours[orientation % 6] != null;
    }

    public Tile getCurrentTile() {
        return currentTile;
    }

    public int getCurrentTileRotation() {
        return currentTileRotation;
    }

    public int getTileCost() {
        if (currentTile.getNb() == preprintedTiled) {
            return getTileCost(0);
        } else {
            return getTileCost(currentTile.getColourNumber());
        }
    }

    public int getTileCost(int index) {
        if (index >= 0 && index < tileCost.length) {
            return tileCost[index];
        } else {
            return 0;
        }
    }

    public int[] getTileCostAsArray(){
        return tileCost;
    }

    /**
     * new wrapper function for the LayTile action that calls the actual
     * upgrade mehod
     * @param action executed LayTile action
     */
    public void upgrade(LayTile action) {
        Tile newTile = action.getLaidTile();
        int newRotation = action.getOrientation();
        Map<String, Integer> relaidTokens = action.getRelaidBaseTokens();

        upgrade(newTile, newRotation, relaidTokens);
    }

    /**
     * Prepare a tile upgrade. The actual tile replacement is done in
     * replaceTile(), via a TileMove object.
     */
    public void upgrade(Tile newTile, int newRotation, Map<String, Integer> relaidTokens) {

        Stop newCity;
        String newTracks;
        List<Stop> newCities;

        if (relaidTokens == null) relaidTokens = new HashMap<String, Integer>();

        if (currentTile.getNumStations() == newTile.getNumStations()) {
            // If the number of stations does not change,
            // reassign new Stations to existing cities,
            // keeping the original numbers (which therefore
            // may become different from the new tile's
            // station numbers).
            Map<Stop, Station> citiesToStations = new HashMap<Stop, Station>();

            // Check for manual handling of tokens
            for (String compName : relaidTokens.keySet()) {
                for (Stop city : stops) {
                    if (city.hasTokenOf(compName)) {
                        citiesToStations.put(city, newTile.getStations().get(relaidTokens.get(compName)-1));
                    }
                }
            }

            // Scan the old cities/stations,
            // and assign new stations where tracks correspond
            for (Stop city : stops) {
                if (citiesToStations.containsKey(city)) continue;
                Station oldStation = city.getRelatedStation();
                int[] oldTrackEnds =
                    getTrackEndPoints(currentTile, currentTileRotation,
                            oldStation);
                if (oldTrackEnds.length == 0) continue;
                station: for (Station newStation : newTile.getStations()) {
                    int[] newTrackEnds =
                        getTrackEndPoints(newTile, newRotation, newStation);
                    for (int i = 0; i < oldTrackEnds.length; i++) {
                        for (int j = 0; j < newTrackEnds.length; j++) {
                            if (oldTrackEnds[i] == newTrackEnds[j]) {
                                // Match found!
                                citiesToStations.put(city, newStation);
                                continue station;
                            }
                        }
                    }
                }
            }

            // Map any unassigned cities randomly
            city: for (Stop city : stops) {
                if (citiesToStations.containsKey(city)) continue;
                for (Station newStation : newTile.getStations()) {
                    if (citiesToStations.values().contains(newStation)) continue;
                    citiesToStations.put(city, newStation);
                    continue city;
                }
            }


            // Assign the new Stations to the existing cities
            for (Stop city : citiesToStations.keySet()) {
                Station newStation = citiesToStations.get(city);
                Station oldStation = city.getRelatedStation();
                city.setRelatedStation(newStation);
                city.setSlots(newStation.getBaseSlots());
                newTracks =
                    getConnectionString(newTile,
                            newRotation,
                            newStation.getNumber());
                city.setTrackEdges(newTracks);
                log.debug("Assigned "
                        + city.getUniqueId()
                        + " from "
                        + oldStation.getId()
                        + " "
                        + getConnectionString(currentTile,
                                currentTileRotation,
                                oldStation.getNumber())
                                + " to " + newStation.getId() + " "
                                + newTracks);
            }
            newCities = stops;

        } else {
            // If the number of stations does change,
            // create a new set of cities.

            // Build a map from old to new cities,
            // so that we can move tokens at the end.
            newCities = new ArrayList<Stop>(4);
            Map<Integer, Stop> mNewCities = new HashMap<Integer, Stop>(4);
            Map<Stop, Stop> oldToNewCities = new HashMap<Stop, Stop>();
            Map<Station, Stop> newStationsToCities =
                new HashMap<Station, Stop>();

            // Scan the old cities/stations,
            // and assign new stations where tracks correspond
            int newCityNumber = 0;
            for (Stop oldCity : stops) {
                int cityNumber = oldCity.getNumber();
                Station oldStation = oldCity.getRelatedStation();
                int[] oldTrackEnds =
                    getTrackEndPoints(currentTile, currentTileRotation,
                            oldStation);
                log.debug("Old city #"
                        + currentTile.getNb()
                        + " city "
                        + oldCity.getNumber()
                        + ": "
                        + getConnectionString(currentTile,
                                currentTileRotation, oldStation.getNumber()));
                station: for (Station newStation : newTile.getStations()) {
                    int[] newTrackEnds =
                        getTrackEndPoints(newTile, newRotation, newStation);
                    log.debug("New station #"
                            + newTile.getNb()
                            + " station "
                            + newStation.getNumber()
                            + ": "
                            + getConnectionString(newTile, newRotation,
                                    newStation.getNumber()));
                    for (int i = 0; i < oldTrackEnds.length; i++) {
                        for (int j = 0; j < newTrackEnds.length; j++) {
                            if (oldTrackEnds[i] == newTrackEnds[j]) {
                                // Match found!
                                if (!newStationsToCities.containsKey(newStation)) {
                                    newCity = Stop.create(this, ++newCityNumber, newStation);
                                    newCities.add(newCity);
                                    mNewCities.put(cityNumber, newCity);
                                    newStationsToCities.put(newStation, newCity);
                                    newCity.setSlots(newStation.getBaseSlots());
                                } else {
                                    newCity =
                                        newStationsToCities.get(newStation);
                                }
                                oldToNewCities.put(oldCity, newCity);
                                newTracks =
                                    getConnectionString(newTile,
                                            newRotation,
                                            newStation.getNumber());
                                newCity.setTrackEdges(newTracks);
                                log.debug("Assigned from "
                                        + oldCity.getUniqueId()
                                        + " #"
                                        + currentTile.getNb()
                                        + "/"
                                        + currentTileRotation
                                        + " "
                                        + oldStation.getId()
                                        + " "
                                        + getConnectionString(currentTile,
                                                currentTileRotation,
                                                oldStation.getNumber())
                                                + " to " + newCity.getUniqueId()
                                                + " #" + newTile.getNb() + "/"
                                                + newRotation + " "
                                                + newStation.getId() + " "
                                                + newTracks);
                                break station;
                            }
                        }
                    }


                }
            }

            // If an old city is not yet connected, check if was
            // connected to another city it has merged into (1851 Louisville)
            for (Stop oldCity : stops) {
                if (oldToNewCities.containsKey(oldCity)) continue;
                Station oldStation = oldCity.getRelatedStation();
                int[] oldTrackEnds =
                    getTrackEndPoints(currentTile, currentTileRotation,
                            oldStation);
                station: for (int i = 0; i < oldTrackEnds.length; i++) {
                    log.debug("Old track ending at "+oldTrackEnds[i]);
                    if (oldTrackEnds[i] < 0) {
                        int oldStationNumber = -oldTrackEnds[i];
                        // Find the old city that has this number
                        for (Stop oldCity2 : stops) {
                            log.debug("Old city "+oldCity2.getNumber()+" has station "+oldCity2.getRelatedStation().getNumber());
                            log.debug("  and links to new city "+oldToNewCities.get(oldCity2));
                            if (oldCity2.getRelatedStation().getNumber()
                                    == oldStationNumber
                                    && oldToNewCities.containsKey(oldCity2)) {
                                newCity = oldToNewCities.get(oldCity2);
                                oldToNewCities.put(oldCity, newCity);
                                log.debug("Assigned from "
                                        + oldCity.getUniqueId()
                                        + " #"
                                        + currentTile.getNb()
                                        + "/"
                                        + currentTileRotation
                                        + " "
                                        + oldStation.getId()
                                        + " "
                                        + getConnectionString(currentTile,
                                                currentTileRotation,
                                                oldStation.getNumber())
                                                + " to " + newCity.getUniqueId()
                                                + " #" + newTile.getNb() + "/"
                                                + newRotation + " "
                                                + newCity.getRelatedStation().getId() + " "
                                                + newCity.getTrackEdges());
                                break station;


                            }
                        }

                    }
                }
            }

            // Check if there any new stations not corresponding
            // to an old city.
            for (Station newStation : newTile.getStations()) {
                if (newStationsToCities.containsKey(newStation)) continue;

                // Create a new city for such a station.
                int cityNumber;
                for (cityNumber = 1; mNewCities.containsKey(cityNumber); cityNumber++)
                    ;
                newCity = Stop.create(this, ++newCityNumber, newStation);
                newCities.add(newCity);
                mNewCities.put(cityNumber, newCity);
                newStationsToCities.put(newStation, newCity);
                newCity.setSlots(newStation.getBaseSlots());
                newTracks =
                    getConnectionString(newTile, newRotation,
                            newStation.getNumber());
                newCity.setTrackEdges(newTracks);
                log.debug("New city added " + newCity.getUniqueId() + " #"
                        + newTile.getNb() + "/" + newRotation + " "
                        + newStation.getId() + " " + newTracks);
            }

            // Move the tokens
            Map<Token, Portfolio<Token>> tokenDestinations =
                new HashMap<Token, Portfolio<Token>>();

            for (Stop oldCity : stops) {
                newCity = oldToNewCities.get(oldCity);
                if (newCity != null) {
                    oldtoken: for (Token token : oldCity.getTokens()) {
                        if (token instanceof BaseToken) {
                            // Check if the new city already has such a token
                            PublicCompany company =
                                ((BaseToken) token).getParent();
                            for (Token token2 : newCity.getTokens()) {
                                if (token2 instanceof BaseToken
                                        && company == ((BaseToken) token2).getPortfolio().getOwner()) {
                                    // No duplicate tokens in one city, so move to free tokens
                                    tokenDestinations.put(token, company.getBaseTokensModel().getFreeTokens());
                                    log.debug("Duplicate token "
                                            + token.getUniqueId()
                                            + " moved from "
                                            + oldCity.getId() + " to "
                                            + company.getId());
                                    ReportBuffer.add(LocalText.getText(
                                            "DuplicateTokenRemoved",
                                            company.getId(),
                                            getId() ));
                                    continue oldtoken;
                                }
                            }
                        }
                        tokenDestinations.put(token, newCity.getTokens());
                        log.debug("Token " + token.getUniqueId()
                                + " moved from " + oldCity.getId() + " to "
                                + newCity.getId());
                    }
                if (!tokenDestinations.isEmpty()) {
                    for (Token token : tokenDestinations.keySet()) {
                        tokenDestinations.get(token).moveInto(token);
                    }
                }
                } else {
                    log.debug("No new city!?");
                }

            }

        }

        // TODO: Check as the code below was not reachable
        // Replace the tile
//        new TileMove(this, currentTile, currentTileRotation, stops,
//                newTile, newRotation, newCities);

        /* TODO Further consequences to be processed here, e.g. new routes etc. */
    }

    /**
     * Execute a tile replacement. This method should only be called from
     * TileMove objects. It is also used to undo tile lays.
     *
     * @param oldTile The tile to be replaced (only used for validation).
     * @param newTile The new tile to be laid on this hex.
     * @param newTileOrientation The orientation of the new tile (0-5).
     */
    public void replaceTile(Tile oldTile, Tile newTile,
            int newTileOrientation, List<Stop> newCities) {

        if (oldTile != currentTile) {
            new Exception("ERROR! Hex " + name + " wants to replace tile #"
                    + oldTile.getNb() + " but has tile #"
                    + currentTile.getNb() + "!").printStackTrace();
        }
        if (currentTile != null) {
            currentTile.remove(this);
        }

        log.debug("On hex " + name + " replacing tile " + currentTile.getNb()
                + "/" + currentTileRotation + " by " + newTile.getNb() + "/"
                + newTileOrientation);

        newTile.add(this);

        currentTile = newTile;
        currentTileRotation = newTileOrientation;

        stops = newCities;
        mStops.clear();
        if (stops != null) {
            for (Stop city : stops) {
                mStops.put(city.getNumber(), city);
                log.debug("Tile #"
                        + newTile.getNb()
                        + " station "
                        + city.getNumber()
                        + " has tracks to "
                        + getConnectionString(newTile, newTileOrientation,
                                city.getRelatedStation().getNumber()));
            }
        }
        /* TODO: Further consequences to be processed here, e.g. new routes etc. */
        // TODO: is this still required?
        // update(); // To notfiy ViewObject (Observer)

    }

    public boolean layBaseToken(PublicCompany company, int station) {
        if (stops == null || stops.isEmpty()) {
            log.error("Tile " + getId()
                    + " has no station for home token of company "
                    + company.getId());
            return false;
        }
        Stop city = mStops.get(station);

        BaseToken token = company.getFreeToken();
        if (token == null) {
            log.error("Company " + company.getId() + " has no free token");
            return false;
        } else {
            city.getTokens().moveInto(token);
            // TODO: is this still required?
            // update();

            if (isHomeFor(company)
                    && isBlockedForTokenLays != null
                    && isBlockedForTokenLays.booleanValue()) {
                // Assume that there is only one home base on such a tile,
                // so we don't need to check for other ones
                isBlockedForTokenLays.set(false);
            }

            return true;
        }
    }

    /**
     * Lay a bonus token.
     * @param token The bonus token object to place
     * @param phaseManager The PhaseManager is also passed in case the
     * token must register itself for removal when a certain phase starts.
     * @return
     */
    public boolean layBonusToken(BonusToken token, PhaseManager phaseManager) {
        if (token == null) {
            log.error("No token specified");
            return false;
        } else {
            offStationTokens.moveInto(token);
            token.prepareForRemoval (phaseManager);
            return true;
        }
    }

    
    // TODO: This is not called anymore
    public boolean addToken(Token token, int position) {

        if (offStationTokens.containsItem(token)) {
            return false;
        } 

        offStationTokens.moveInto(token);
        return true;
    }

    public List<BaseToken> getBaseTokens () {
        if (stops == null || stops.isEmpty()) return null;
        List<BaseToken> tokens = new ArrayList<BaseToken>();
        for (Stop city : stops) {
            for (Token token : city.getTokens()) {
                if (token instanceof BaseToken) {
                    tokens.add((BaseToken)token);
                }
            }
        }
        return tokens;
    }

    public PortfolioList<Token> getTokens() {
        return offStationTokens;
    }

    public boolean hasTokens() {
        return offStationTokens.size() > 0;
    }

    public boolean hasTokenSlotsLeft(int station) {
        if (station == 0) station = 1; // Temp. fix for old save files
        Stop city = mStops.get(station);
        if (city != null) {
            return city.hasTokenSlotsLeft();
        } else {
            log.error("Invalid station " + station + ", max is "
                    + (stops.size() - 1));
            return false;
        }
    }

    public boolean hasTokenSlotsLeft() {
        for (Stop city : stops) {
            if (city.hasTokenSlotsLeft()) return true;
        }
        return false;
    }

    /** Check if the tile already has a token of a company in any station */
    public boolean hasTokenOfCompany(PublicCompany company) {

        for (Stop city : stops) {
            if (city.hasTokenOf(company)) return true;
        }
        return false;
    }

    public ImmutableList<Token> getTokens(int cityNumber) {
        if (stops.size() > 0 && mStops.get(cityNumber) != null) {
            return (mStops.get(cityNumber)).getTokens().items();
        } else {
            return ImmutableList.of(); // empty List
        }
    }

    /**
     * Return the city number (1,...) where a company has a base token. If none,
     * return zero.
     *
     * @param company
     * @return
     */
    public int getCityOfBaseToken(PublicCompany company) {
        if (stops == null || stops.isEmpty()) return 0;
        for (Stop city : stops) {
            for (Token token : city.getTokens()) {
                if (token instanceof BaseToken
                        && ((BaseToken) token).getParent() == company) {
                    return city.getNumber();
                }
            }
        }
        return 0;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public Stop getStop(int stopNumber) {
        return mStops.get(stopNumber);
    }

    public Stop getRelatedStop(Station station) {
        Stop foundStop = null;
        for (Stop stop:mStops.values()) {
            if (station == stop.getRelatedStation()) {
                foundStop = stop;
            }
        }
        return foundStop;
    }

    public void addHome(PublicCompany company, int stopNumber) throws ConfigurationException {
        if (homes == null) homes = new HashMap<PublicCompany, Stop>();
        if (stops.isEmpty()) {
            log.error("No cities for home station on hex " + name);
        } else {
            // not yet decided
            if (stopNumber == 0) {
                homes.put(company, null);
                log.debug("Added home of " + company  + " in hex " + this.toString() +  " city not yet decided");
            } else if (stopNumber > stops.size()) {
                throw new ConfigurationException ("Invalid city number "+stopNumber+" for hex "+name
                        +" which has "+stops.size()+" cities");
            } else {
                Stop homeCity = stops.get(Math.max(stopNumber - 1, 0));
                homes.put(company, homeCity);
                log.debug("Added home of " + company + " set to " + homeCity + " id= " +homeCity.getUniqueId());
            }
        }
    }

    public Map<PublicCompany, Stop> getHomes() {
        return homes;
    }

    public boolean isHomeFor(PublicCompany company) {
        boolean result = homes != null && homes.containsKey(company);
        return result;
    }

    public void addDestination(PublicCompany company) {
        if (destinations == null)
            destinations = new ArrayList<PublicCompany>();
        destinations.add(company);
    }

    public List<PublicCompany> getDestinations() {
        return destinations;
    }

    public boolean isDestination(PublicCompany company) {
        return destinations != null && destinations.contains(company);
    }

    /**
     * @return Returns false if no tiles may yet be laid on this hex.
     */
    public boolean isBlockedForTileLays() {
        if (isBlockedForTileLays == null)
            return false;
        else
            return isBlockedForTileLays.booleanValue();
    }

    /**
     * @param isBlocked The isBlocked to set (state variable)
     */
    public void setBlockedForTileLays(boolean isBlocked) {
        isBlockedForTileLays.set(isBlocked);
    }

    public boolean isUpgradeableNow() {
        if (isBlockedForTileLays()) {
            log.debug("Hex " + name + " is blocked");
            return false;
        }
        if (currentTile != null) {
            if (currentTile.isUpgradeable()) {
                return true;
            } else {
                log.debug("Hex " + name + " tile #" + currentTile.getNb()
                        + " is not upgradable now");
                return false;
            }
        }
        log.debug("No tile on hex " + name);
        return false;
    }

    public boolean isUpgradeableNow(Phase currentPhase) {
        return (isUpgradeableNow() & !this.getCurrentTile().getValidUpgrades(this,
                currentPhase).isEmpty());
    }

    /**
     * @return Returns false if no base tokens may yet be laid on this hex and station.
     *
     * NOTE: this method currently only checks for prohibitions caused
     * by the presence of unlaid home base tokens.
     * It does NOT (yet) check for free space.
     *
     *
     * There are the following cases to check for each company located there
     *
     * A) City is decided or there is only one city
     *   => check if the city has a free slot or not
     *   (examples: NYNH in 1830 for a two city tile, NYC for a one city tile)
     * B) City is not decided (example: Erie in 1830)
     *   two subcases depending on isHomeBlockedForAllCities
     *   - (true): all cities of the hex have remaining slots available
     *   - (false): no city of the hex has remaining slots available
     * C) Or the company does not block its home city at all (example:Pr in 1835)
     *    then isBlockedForTokenLays attribute is used
     *
     * NOTE: It now deals with more than one company with a home base on the
     * same hex.
     *
     * Previously there was only the variable isBlockedForTokenLays
     * which is set to yes to block the whole hex for the token lays
     * until the (home) company laid their token
     * 
     * 
     * FIXME: There is now the issue that isBlockedForTokenLays is initialised all the time, so null
     * is not a valid condition anymore.
     *
     */
    public boolean isBlockedForTokenLays(PublicCompany company, int cityNumber) {

        if (isHomeFor(company)) {
            // Company can always lay a home base
            return false;
        } else if (isBlockedForTokenLays != null) {
            // Return MapHex attribute if defined
            return isBlockedForTokenLays.booleanValue();
        } else if (homes != null && !homes.isEmpty()) {
            Stop cityToLay = this.getStop(cityNumber);
            if (cityToLay == null) { // city does not exist, this does not block itself
                return false;
            }
            // check if the city is potential home for other companies
            int allBlockCompanies = 0;
            int anyBlockCompanies = 0;
            int cityBlockCompanies = 0;
            for (PublicCompany comp : homes.keySet()) {
                if (comp.hasLaidHomeBaseTokens() || comp.isClosed()) continue;
                // home base not laid yet
                Stop homeCity = homes.get(comp);
                if (homeCity == null) {
                    if (comp.isHomeBlockedForAllCities()) {
                        allBlockCompanies ++; // undecided companies that block all cities
                    } else {
                        anyBlockCompanies ++; // undecided companies that block any cities
                    }
                } else if (cityToLay == homeCity) {
                    cityBlockCompanies ++; // companies which are located in the city in question
                } else {
                    anyBlockCompanies ++; // companies which are located somewhere else
                }
            }
            log.debug("IsBlockedForTokenLays: allBlockCompanies = " + allBlockCompanies +
                    ", anyBlockCompanies = " + anyBlockCompanies + " , cityBlockCompanies = " + cityBlockCompanies);
            // check if there are sufficient individual city slots
            if (allBlockCompanies + cityBlockCompanies + 1 > cityToLay.getTokenSlotsLeft()) {
                return true; // the additional token exceeds the number of available slots
            }
            // check if the overall hex slots are sufficient
            int allTokenSlotsLeft = 0;
            for (Stop city:stops) {
                allTokenSlotsLeft += city.getTokenSlotsLeft();
            }
            if (allBlockCompanies + anyBlockCompanies  + cityBlockCompanies + 1 > allTokenSlotsLeft) {
                return true; // all located companies plus the additonal token exceeds the available slots
            }
        }
        return false;
    }

    /**
     * @param isBlocked The isBlocked to set (state variable)
     */
    public void setBlockedForTokenLays(boolean isBlocked) {
        isBlockedForTokenLays.set(isBlocked);
    }

    public boolean hasValuesPerPhase() {
        return valuesPerPhase != null && valuesPerPhase.length > 0;
    }

    public int[] getValuesPerPhase() {
        return valuesPerPhase;
    }

    public int getCurrentValueForPhase(Phase phase) {
        if (hasValuesPerPhase() && phase != null) {
            return valuesPerPhase[Math.min(
                    valuesPerPhase.length,
                    phase.getOffBoardRevenueStep()) - 1];
        } else {
            return 0;
        }
    }

    public String getCityName() {
        return cityName;
    }

    public String getInfo () {
        return infoText;
    }

    public String getReservedForCompany() {
        return reservedForCompany;
    }

    public boolean isReservedForCompany () {
        return reservedForCompany != null;
    }

    public List<RevenueBonusTemplate> getRevenueBonuses() {
        return revenueBonuses;
    }

    public boolean equals(MapHex hex) {
        if (hex.getId().equals(getId()) && hex.row == row
                && hex.column == column) return true;
        return false;
    }

    public MapManager getParent() {
        return (MapManager)getParent();
    }

    @Override
    public String toString() {
        return name + " (" + row + "," + column + ")";
    }

    /**
     * The string sent to the GUIHex as it is notified. Format is
     * tileId/orientation.
     *
     * @TODO include tokens??
     */
    public String getData() {
        return currentTile.getNb() + "/" + currentTileRotation;
    }

    /**
     * Get a String describing one stations's connection directions of a laid
     * tile, taking into account the current tile rotation.
     *
     * @return
     */
    public String getConnectionString(Tile tile, int rotation,
            int stationNumber) {
        StringBuffer b = new StringBuffer("");
        if (stops != null && stops.size() > 0) {
            Map<Integer, List<Track>> tracks = tile.getTracksPerStationMap();
            if (tracks != null && tracks.get(stationNumber) != null) {
                for (Track track : tracks.get(stationNumber)) {
                    int endPoint = track.getEndPoint(-stationNumber);
                    if (endPoint < 0) continue;
                    int direction = rotation + endPoint;
                    if (b.length() > 0) b.append(",");
                    b.append(getOrientationName(direction));
                }
            }
        }
        return b.toString();
    }

    public String getConnectionString(int cityNumber) {
        int stationNumber =
            mStops.get(cityNumber).getRelatedStation().getNumber();
        return getConnectionString(currentTile, currentTileRotation,
                stationNumber);
    }

    public int[] getTrackEndPoints(Tile tile, int rotation, Station station) {
        List<Track> tracks = tile.getTracksPerStation(station.getNumber());
        if (tracks == null) {
            return new int[0];
        }

        int[] endpoints = new int[tracks.size()];
        int endpoint;
        for (int i = 0; i < tracks.size(); i++) {
            endpoint = tracks.get(i).getEndPoint(-station.getNumber());
            if (endpoint >= 0) {
                endpoints[i] = (rotation + endpoint) % 6;
            } else {
                endpoints[i] = endpoint;
            }
        }
        return endpoints;
    }

    public RunThrough isRunThroughAllowed() {
        return runThroughAllowed;
    }

    public RunTo isRunToAllowed() {
        return runToAllowed;
    }

    public Loop isLoopAllowed () {
        return loopAllowed;
    }

    public Type getStopType() {
        return stopType;
    }

    public Score getScoreType() {
        return scoreType;
    }

    // Owner interface
    
    public boolean hasPortfolio() {
        return false;
    }

    public PortfolioModel getPortfolioModel() {
        return null;
    }

    public boolean hasCash() {
        return false;
    }

    public CashMoneyModel getCash() {
        return null;
    }

    public void update() {
        // TODO Auto-generated method stub
        
    }

    public void addObserver(Observer observer) {
        // TODO Auto-generated method stub
        
    }

    public void removeObserver(Observer observer) {
        // TODO Auto-generated method stub
        
    }
}