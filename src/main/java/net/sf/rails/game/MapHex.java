package net.sf.rails.game;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.rails.algorithms.RevenueBonusTemplate;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.*;
import net.sf.rails.game.TileUpgrade.Rotation;
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.HashBiMapState;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.PortfolioSet;
import net.sf.rails.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.LayTile;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;



// TODO: Rewrite the mechanisms as model


/**
 * Represents a Hex on the Map from the Model side.
 */
public class MapHex extends RailsModel implements RailsOwner, Configurable {
    
    private static final Logger log =
            LoggerFactory.getLogger(MapHex.class);

    public static class Coordinates {

        // externally used coordinates
        private final int row;
        private final int col;
        
        private static final Pattern namePattern = Pattern.compile("(\\D+?)(-?\\d+)");
        
        private Coordinates(int row, int col) {
            this.row = row;
            this.col = col;
        }
        
        public static Coordinates createFromId(String id, MapOrientation mapOrientation) 
                throws ConfigurationException {

            Matcher m = namePattern.matcher(id);
            
            if (!m.matches()) {
                throw new ConfigurationException("Invalid name format: " + id);
            }
            String letters = m.group(1);
            int letter;
            if (letters.length() == 1) {
                letter = letters.charAt(0);
            } else { // for row 'AA' in 1825U1
                letter = 26 + letters.charAt(1);
            }
            // FIXME: Replace with negative numbers instead of > 100
            int number;
            try {
                number = Integer.parseInt(m.group(2));
                if (number > 90) number -= 100;  // For 1825U1 column 99 (= -1)
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid number format: " + m.group(2));
            }

            /*
             * Translate hex names (as on the board) to coordinates used for
             * drawing.
             */
            int row, column;
            if (mapOrientation.lettersGoHorizontal()) {
                row = number;
                column = letter - '@';
            } else { // letters go vertical (normal case)
                row = letter - '@';
                column = number;
            }
            return new Coordinates(row, column);
        }
        
        public static Coordinates maximum(Collection<MapHex> hexes) {
            int maxRow, maxCol; 
            maxRow = maxCol = Integer.MIN_VALUE; 
            for (MapHex hex:hexes) {
                Coordinates coordinates = hex.coordinates;
                maxRow = Math.max(maxRow, coordinates.row);
                maxCol = Math.max(maxCol, coordinates.col);
            }
            return new Coordinates(maxRow, maxCol);
        }

        public static Coordinates minimum(Collection<MapHex> hexes) {
            int minRow, minCol; 
            minRow = minCol = Integer.MAX_VALUE; 
            for (MapHex hex:hexes) {
                Coordinates coordinates = hex.coordinates;
                minRow = Math.min(minRow, coordinates.row);
                minCol = Math.min(minCol, coordinates.col);
            }
            return new Coordinates(minRow, minCol);
        }
        
        public int getRow() {
            return row;
        }
        
        public int getCol() {
            return col;
        }
        
        public Coordinates translate(int deltaRow, int deltaCol) {
            return new Coordinates(row + deltaRow, col + deltaCol);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(row, col);
        }
        
        @Override 
        public boolean equals(Object other) {
            if (!(other instanceof Coordinates)) return false;
            return row == ((Coordinates) other).row 
                    && col == ((Coordinates)other).col;
        }
        
        @Override
        public String toString() {
            return Objects.toStringHelper(this).
                    addValue(row).addValue(col).
                    toString();
        }
    }
    
    
    ////////////////////////
    // static fields
    ////////////////////////

    private final Coordinates coordinates;

    private String preprintedTileId;
    private String preprintedPictureId ;
    private HexSide preprintedTileRotation;

    private List<Integer> tileCost;

    private String cityName;
    private String reservedForCompany = null;

    /** Values if this is an off-board hex */
    private List<Integer> valuesPerPhase = null;

    /*
     * Temporary storage for impassable hexsides. Once neighbours has been set
     * up, this attribute is no longer used. Only the black or blue bars on the
     * map need be specified, and each one only once. Impassable non-track sides
     * of "offboard" (red) and "fixed" (grey or brown) preprinted tiles will be
     * derived and need not be specified.
     */
    private String impassableTemplate = null;
    private final HexSidesSet.Builder impassableBuilder = 
            HexSidesSet.builder();
    private HexSidesSet impassableSides;
    
    private final HexSidesSet.Builder invalidBuilder = 
            HexSidesSet.builder();
    private HexSidesSet invalidSides;

    private List<PublicCompany> destinations = null;

    /** Storage of revenueBonus that are bound to the hex */
    private List<RevenueBonusTemplate> revenueBonuses = null;

    /** 
     * Optional attribute to provide the type of any stops on the hex.
     * Normally the type will be derived from the tile properties.
     */
    private StopType stopType = null;
    
    /**
     * Optional cache for city distances
     */
    private SortedSet<Integer> uniqueCityDistances;
    private Map<MapHex, Integer> cityDistances;
    
    ////////////////////////
    // dynamic fields
    ////////////////////////
    private final GenericState<Tile> currentTile = 
            GenericState.create(this, "currentTile");
    private final GenericState<HexSide> currentTileRotation = 
            GenericState.create(this, "currentTileRotation");

    // Stops (Cities, Towns etc.)
    private final HashBiMapState<Station, Stop> stops = 
            HashBiMapState.create(this, "stops");

    // Homes (in 18EU and others the home is selected later in the game
    // Remark: this was a static field in Rails1.x, causing potential undo problems
    private final HashMapState<PublicCompany, Stop> homes =
            HashMapState.create(this, "homes");

    /*
     * changed to state variable to fix undo bug #2954645
     * null as default implies false - see isBlocked()
     */
    private final BooleanState isBlockedForTileLays = 
            BooleanState.create(this, "isBlockedForTileLays");

    /**
     * Is the hex blocked for home tokens? <p>
     * NOTE:<br>
     * ALWAYS means: Always Blocked, no token lay possible (until attribute is changed)
     * RESERVE_SLOT means: Reserves slots (for multi-cities depending on isHomeBlockedForAllCities<br>
     * NEVER means: Never blocked (unless there is not a single free slot remaining)<br>
     * Remark: The latter is used for 1835 Berlin, which is home to PR, but
     * the absence of a PR token does not block the third slot
     * when the green tile is laid. <br>
     * 
     * Remark: in Rails 1.x it was a static field, causing potential undo problems
     */
    public static enum BlockedToken {ALWAYS, RESERVE_SLOT, NEVER};
    private final GenericState<BlockedToken> isBlockedForTokenLays = 
            GenericState.create(this, "isBlockedForTokenLays");

    /** OffStation BonusTokens */
    private final PortfolioSet<BonusToken> bonusTokens = 
            PortfolioSet.create(this, "bonusTokens", BonusToken.class);
    
    /** OffStation BaseTokens */
    private final PortfolioSet<BaseToken> offStationTokens = 
            PortfolioSet.create(this, "offStationTokens", BaseToken.class);

    private MapHex(MapManager parent, String id, Coordinates coordinates) {
        super(parent, id);
        this.coordinates = coordinates;
    }

    public static MapHex create(MapManager parent, Tag tag) throws ConfigurationException {
        // name serves as id
        String id = tag.getAttributeAsString("name");
        Coordinates coordinates = Coordinates.createFromId(id, parent.getMapOrientation());
        MapHex hex = new MapHex(parent, id, coordinates);
        hex.configureFromXML(tag);
        return hex;
    }
        
    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        preprintedTileId = tag.getAttributeAsString("tile", null);
        preprintedPictureId = tag.getAttributeAsString("pic", preprintedTileId);
        int orientation = tag.getAttributeAsInteger("orientation", 0);
        preprintedTileRotation = HexSide.get(orientation); 

        impassableTemplate = tag.getAttributeAsString("impassable");
        tileCost = tag.getAttributeAsIntegerList("cost");

        // Off-board revenue values
        valuesPerPhase = tag.getAttributeAsIntegerList("value");

        // City name
        cityName = tag.getAttributeAsString("city", "");

        if (tag.getAttributeAsString("unlaidHomeBlocksTokens") == null) {
            // default (undefined) is RESERVE_SLOT
            isBlockedForTokenLays.set(BlockedToken.RESERVE_SLOT);
        } else {
            if (tag.getAttributeAsBoolean("unlaidHomeBlocksTokens", false)) {
                isBlockedForTokenLays.set(BlockedToken.ALWAYS);
            } else {
                isBlockedForTokenLays.set(BlockedToken.NEVER);
            }
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

        // Stop properties
        Tag accessTag = tag.getChild("Access");
        stopType = StopType.parseStop(this, accessTag, getParent().getDefaultStopTypes());
    }

    public void finishConfiguration (RailsRoot root) {
        currentTile.set(root.getTileManager().getTile(preprintedTileId));
        currentTileRotation.set(preprintedTileRotation);

        // We need completely new objects, not just references to the Tile's
        // stations.
        for (Station station : currentTile.value().getStations()) {
            Stop stop = Stop.create(this, station);
            stops.put(station, stop);
        }
        
        impassableSides = impassableBuilder.build();
        invalidSides = invalidBuilder.build();
    }
    
    @Override
    public MapManager getParent() {
        return (MapManager)super.getParent();
    }
    
    public void addImpassableSide(HexSide side) {
        impassableBuilder.set(side);
        log.debug("Added impassable " + side + " to " + this);
    }

    public HexSidesSet getImpassableSides () {
        return impassableSides;
    }
    
    public void addInvalidSide(HexSide side) {
        invalidBuilder.set(side);
    }
    
    public HexSidesSet getInvalidSides() {
        return invalidSides;
    }
    
    public boolean isImpassableNeighbour(MapHex neighbour) {
        return impassableTemplate != null && impassableTemplate.indexOf(neighbour.getId()) > -1;
    }

    public boolean isValidNeighbour(MapHex neighbour, HexSide side) {
        if (isImpassableNeighbour(neighbour)) return false;
        /*
         * The preprinted tile on this hex is offmap or fixed and has no
         * track to this side.
         */
        Tile neighbourTile = neighbour.getCurrentTile();
        if (neighbourTile.isUpgradeable()) return true;
        HexSide rotated = side.opposite().rotate(neighbour.getCurrentTileRotation().negative());
        return neighbourTile.hasTracks(rotated);
    }

    public String getOrientationName(HexSide orientation) {
        return getParent().getMapOrientation().getORNames(orientation);
    }
    
    @Deprecated
    public String getOrientationName(int orientation) {
        return getOrientationName(HexSide.get(orientation));
    }

    /* ----- Instance methods ----- */

    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public boolean isPreprintedTileCurrent() {
        return currentTile.value().getId().equals(preprintedTileId);
    }
    
    /**
     * @return Returns the preprintedTileId.
     */
    public String getPreprintedTileId() {
        return preprintedTileId;
    }

    public HexSide getPreprintedTileRotation() {
        return preprintedTileRotation;
    }

    /** Return the current picture ID (i.e. the tile ID to be displayed, rather than used for route determination).
     * <p> Usually, the picture ID is equal to the tile ID. Different values may be defined per hex or per tile.
     * Restriction: definitions per hex can apply to preprinted tiles only.
     * @return The current picture ID
     */
    public String getPictureId (Tile tile) {
        if (tile.getId().equals(preprintedTileId)) {
            return preprintedPictureId;
        } else {
            return tile.getPictureId();
        }
    }

    public Tile getCurrentTile() {
        return currentTile.value();
    }

    public HexSide getCurrentTileRotation() {
        return currentTileRotation.value();
    }

    public int getTileCost() {
        if (isPreprintedTileCurrent()) {
            return getTileCost(0);
        } else {
            return getTileCost(currentTile.value().getColourNumber());
        }
    }

    // TODO: Replace index by TileColours
    private int getTileCost(int index) {
        try {
            return tileCost.get(index);
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    public List<Integer> getTileCostsList(){
        return tileCost;
    }
    
    public StopType getStopType() {
        return stopType;
    }

    /**
     * new wrapper function for the LayTile action that calls the actual
     * upgrade mehod
     * @param action executed LayTile action
     */
    public void upgrade(LayTile action) {
        Tile newTile = action.getLaidTile();
        HexSide newRotation = HexSide.get(action.getOrientation());
        Map<String, Integer> relaidTokens = action.getRelaidBaseTokens();

        upgrade(newTile, newRotation, relaidTokens);
    }

    public void upgrade(Tile newTile, HexSide newRotation, Map<String, Integer> relaidTokens) {
        
        TileUpgrade upgrade = currentTile.value().getSpecificUpgrade(newTile);
        Rotation rotation = upgrade.getRotation(
                newRotation.rotate(currentTileRotation.value().negative()));
        Map<Station, Station> stationMapping;
        
        if (rotation != null) {
            log.debug("Valid rotation found " + rotation);
            stationMapping = rotation.getStationMapping();
        } else {
            stationMapping = null;
            log.error("No valid rotation was found: newRotation= " + newRotation 
                    + "currentRotation" + currentTileRotation.value() );
        }
        
        BiMap<Stop, Station> stopsToNewStations = HashBiMap.create();
        Set<Stop> droppedStops = Sets.newHashSet();

        if (relaidTokens != null) {
            // Check for manual handling of tokens
            for (String compName : relaidTokens.keySet()) {
                PublicCompany company = getRoot().getCompanyManager().getPublicCompany(compName);
                for (Stop stop : stops) {
                    if (stop.hasTokenOf(company)) {
                        Station newStation = newTile.getStation(relaidTokens.get(compName));
                        stopsToNewStations.put(stop, newStation);
                        log.debug("Mapped by relaid tokens: station " + stop.getRelatedStation() 
                                + " to " + newStation);
                        break;
                    }
                }
            }
            // Map all other stops in sequence to the remaining stations
            SetView<Stop> unassignedStops = 
                    Sets.difference(stops.viewValues(), stopsToNewStations.keySet());
            for (Stop stop:unassignedStops) {
                for (Station newStation:newTile.getStations()) {
                    if (!stopsToNewStations.containsValue(newStation)) {
                        stopsToNewStations.put(stop, newStation);
                        log.debug("Mapped after relaid tokens: station " + stop.getRelatedStation() 
                                + " to " + newStation);
                        break;
                    }
                }
            }
        } else { // default mapping routine
            for (Stop stop: stops) {
                if (stopsToNewStations.containsKey(stop)) continue;
                Station oldStation = stop.getRelatedStation();
                // Search stationMapping for assignments of stops to new stations
                Station newStation = null;
                String debugText = null;
                if (stationMapping == null) {
                    int oldNumber = stop.getRelatedStation().getNumber();
                    newStation = newTile.getStation(oldNumber);
                    debugText = "Mapped by default id"; 
                } else if (stationMapping.containsKey(oldStation)) {
                    // Match found in StationMapping, then assign the new station to the stop
                    newStation = stationMapping.get(oldStation);
                    debugText = "Mapped by stationMapping";
                } 
                if (newStation == null) { // no mapping => log error
                    droppedStops.add(stop);
                    log.debug(debugText + ": station " + oldStation + " is dropped");
                } else {
                    if (stopsToNewStations.containsValue(newStation)) {
                        // new station already assigned a stop, use that
                        // and move tokens between stops
                        Stop otherStop = stopsToNewStations.inverse().get(newStation);
                        moveTokens(stop, otherStop);
                        droppedStops.add(stop);
                        // FIXME: Due to Rails1.x compatibility
                        otherStop.addPreviousNumbers(stop.getLegacyNumber());
                    } else {
                        // otherwise use the existing stop
                        stopsToNewStations.put(stop, newStation);
                    }
                    log.debug(debugText + ": station " + oldStation + " to " + newStation);
                }
            }
        }

        // Check for unassigned Stops
        SetView<Stop> unassignedStops =  Sets.difference(stops.viewValues(), 
                Sets.union(stopsToNewStations.keySet(), droppedStops));
        if (!unassignedStops.isEmpty()) {
            log.error("Unassigned Stops :" + unassignedStops);
        }

        // Check for unassigned Stations
        SetView<Station> unassignedStations = Sets.difference(
                ImmutableSet.copyOf(stopsToNewStations.values()), newTile.getStations());
        if (!unassignedStations.isEmpty()) {
            log.error("Unassigned Stations :" + unassignedStations);
        }
        executeTileLay(newTile, newRotation, stopsToNewStations);
    }

    private void moveTokens(Stop origin, Stop target) {
        for (BaseToken token:origin.getBaseTokens()) {
            PublicCompany company = token.getParent();
            if (target.hasTokenOf(company)) {
                // No duplicate tokens allowed in one city, so move to free tokens
                token.moveTo(company);
                log.debug("Duplicate token "
                        + token.getUniqueId()
                        + " moved from "
                        + origin.getSpecificId() + " to "
                        + company.getId());
                ReportBuffer.add(this, LocalText.getText(
                        "DuplicateTokenRemoved",
                        company.getId(),
                        getId() ));
            } else {
                token.moveTo(target);
                log.debug("Token " + token.getUniqueId()
                        + " moved from " + origin.getSpecificId() + " to "
                        + target.getSpecificId());
            }
        }
    }

    /**
     * Execute a tile replacement. This method should only be called from
     * TileMove objects. It is also used to undo tile lays.
     *
     * @param newTile The new tile to be laid on this hex.
     * @param newOrientation The orientation of the new tile (0-5).
     * @param newStops The new stops used now
     */
    private void executeTileLay(Tile newTile, HexSide newOrientation, BiMap<Stop, Station> newStops) {

        // TODO: Is the check for null still required?
        if (currentTile.value() != null) {
            currentTile.value().remove(this);
        }

        log.debug("On hex " + getId() + " replacing tile " + currentTile.value().getId()
                + "/" + currentTileRotation + " by " + newTile.getId() + "/"
                + newOrientation);

        newTile.add(this);
        currentTile.set(newTile);
        currentTileRotation.set(newOrientation);

        stops.clear();
        if (newStops != null) {
            for (Stop stop:newStops.keySet()) {
                Station station = newStops.get(stop);
                stops.put(station, stop);
                stop.setRelatedStation(station);
                log.debug("Tile #"
                        + newTile.getId()
                        + " station "
                        + station.getNumber()
                        + " has tracks to "
                        + getConnectionString(station));
            }
        }
    }

    public boolean layBaseToken(PublicCompany company, Stop stop) {
        if (stops.isEmpty()) {
            log.error("Tile " + getId()
                    + " has no station for home token of company "
                    + company.getId());
            return false;
        }

        BaseToken token = company.getNextBaseToken();
        if (token == null) {
            log.error("Company " + company.getId() + " has no free token");
            return false;
        } else {
            // transfer token
            token.moveTo(stop);
            if (isHomeFor(company)) {
                // Assume that there is only one home base on such a tile,
                // so we don't need to check for other ones
                isBlockedForTokenLays.set(BlockedToken.NEVER);
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
        Preconditions.checkArgument(token != null, "No token specified");
        bonusTokens.moveInto(token);
        token.prepareForRemoval(phaseManager);
        return true;
    }
    
    public ImmutableSet<BaseToken> getBaseTokens () {
        ImmutableSet.Builder<BaseToken> tokens = ImmutableSet.builder();
        for (Stop stop : stops) {
            tokens.addAll(stop.getBaseTokens());
        }
        return tokens.build();
    }

    public PortfolioSet<BaseToken> getOffStationTokens() {
        return offStationTokens;
    }
    
    public PortfolioSet<BonusToken> getBonusTokens() {
        return bonusTokens;
    }

    public boolean hasTokenSlotsLeft(Station station) {
        // FIXME: Is this still required
        // if (station == 0) station = 1; // Temp. fix for old save files
        return stops.get(station).hasTokenSlotsLeft();
    }

    public boolean hasTokenSlotsLeft() {
        for (Stop stop : stops) {
            if (stop.hasTokenSlotsLeft()) return true;
        }
        return false;
    }

    /** Check if the hex has already a token of the company in any station */
    public boolean hasTokenOfCompany(PublicCompany company) {
        return (getStopOfBaseToken(company) != null);
    }

    /**
        Return the stop that contains the base token of a company
        If no token in the hex, returns null
     */
    public Stop getStopOfBaseToken(PublicCompany company) {
        for (Stop stop : stops) {
            if (stop.hasTokenOf(company)) return stop;
        }
        return null;
    }

    public ImmutableSet<Stop> getStops() {
        return stops.viewValues();
    }

    public Stop getRelatedStop(Station station) {
        return stops.get(station);
    }
    
    // FIXME: Due to Rails1.x compatibility use legacy number or previous numbers
    public Stop getRelatedStop(int stationNb) {
        for (Stop stop:stops) {
            if (stop.getLegacyNumber() == stationNb) return stop;
        }
        for (Stop stop:stops) {
            if (stop.checkPreviousNumbers(stationNb)) return stop;
        }
        return null;
        //return stops.get(getStation(stationNb));
    }
    
    public ImmutableSet<Station> getStations() {
        return stops.viewKeySet();
    }
    
    public Station getStation(int stationNb) {
        return currentTile.value().getStation(stationNb);
    }
    
    public void addHome(PublicCompany company, Stop home) {
        if (stops.isEmpty()) {
            log.error("No cities for home station on hex " + getId());
        } else {
            // not yet decided => create a null stop
            if (home == null) {
                homes.put(company, Stop.create(this, null));
                log.debug("Added home of " + company  + " in hex " + this.toString() +  " city not yet decided");
            } else {
                homes.put(company, home);
                log.debug("Added home of " + company + " set to " + home + " id= " + home.getSpecificId());
            }
        }
    }

    public Map<PublicCompany, Stop> getHomes() {
        return homes.viewMap();
    }

    public boolean isHomeFor(PublicCompany company) {
        return homes.containsKey(company);
    }

    public void addDestination(PublicCompany company) {
        if (destinations == null)
            destinations = new ArrayList<PublicCompany>();
        destinations.add(company);
    }

    public List<PublicCompany> getDestinations() {
        return destinations;
    }

    /**
     * @return Returns false if no tiles may yet be laid on this hex.
     */
    public boolean isBlockedForTileLays() {
        if (isBlockedForTileLays == null)
            return false;
        else
            return isBlockedForTileLays.value();
    }

    /**
     * @param isBlocked The isBlocked to set (state variable)
     */
    public void setBlockedForTileLays(boolean isBlocked) {
        isBlockedForTileLays.set(isBlocked);
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
     * Remark: This was a static field in Rails1.x causing potential undo problems.
     * 
     */
    public boolean isBlockedForTokenLays(PublicCompany company, Stop stopToLay) {

        if (isHomeFor(company)) {
            // Company can always lay a home base
            return false;
        } else if (isBlockedForTokenLays.value() == BlockedToken.ALWAYS) {
            return true;
        } else if (isBlockedForTokenLays.value() == BlockedToken.NEVER) {
            return false;
        } else if (!homes.isEmpty()) {
            // check if the city is potential home for other companies
            int allBlockCompanies = 0;
            int anyBlockCompanies = 0;
            int cityBlockCompanies = 0;
            for (PublicCompany comp : homes.viewKeySet()) {
                if (comp.hasLaidHomeBaseTokens() || comp.isClosed()) continue;
                // home base not laid yet
                Stop homeStop = homes.get(comp);
                if (homeStop == null) {
                    if (comp.isHomeBlockedForAllCities()) {
                        allBlockCompanies ++; // undecided companies that block all cities
                    } else {
                        anyBlockCompanies ++; // undecided companies that block any cities
                    }
                } else if (stopToLay == homeStop) {
                    cityBlockCompanies ++; // companies which are located in the city in question
                } else {
                    anyBlockCompanies ++; // companies which are located somewhere else
                }
            }
            log.debug("IsBlockedForTokenLays: allBlockCompanies = " + allBlockCompanies +
                    ", anyBlockCompanies = " + anyBlockCompanies + " , cityBlockCompanies = " + cityBlockCompanies);
            // check if there are sufficient individual city slots
            if (allBlockCompanies + cityBlockCompanies + 1 > stopToLay.getTokenSlotsLeft()) {
                return true; // the additional token exceeds the number of available slots
            }
            // check if the overall hex slots are sufficient
            int allTokenSlotsLeft = 0;
            for (Stop stop:stops) {
                allTokenSlotsLeft += stop.getTokenSlotsLeft();
            }
            if (allBlockCompanies + anyBlockCompanies  + cityBlockCompanies + 1 > allTokenSlotsLeft) {
                return true; // all located companies plus the additonal token exceeds the available slots
            }
        }
        return false;
    }

    public boolean hasValuesPerPhase() {
        return !valuesPerPhase.isEmpty();
    }

    // FIXME: Replace by Map to Phases
    public List<Integer> getValuesPerPhase() {
        return valuesPerPhase;
    }

    public int getCurrentValueForPhase(Phase phase) {
        if (hasValuesPerPhase() && phase != null) {
            return valuesPerPhase.get(Math.min(
                    valuesPerPhase.size(),
                    phase.getOffBoardRevenueStep()) - 1);
        } else {
            return 0;
        }
    }

    public String getCityName() {
        return cityName;
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

    public String getConnectionString(Station station) {
        return TrackConfig.getConnectionString(this, currentTile.value(), 
                currentTileRotation.value(), station);
    }

    @Override
    public String toText() {
        if (Util.hasValue(cityName)) {
            return getId() + " " + cityName;
        } else {
            return getId();
        }
    }

    @Override
    public String toString() {
        return super.toString() + coordinates;
    }
}
