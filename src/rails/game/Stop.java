package rails.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.state.GenericState;
import rails.game.state.Owner;
import rails.game.state.PortfolioSet;
import rails.util.Util;

/**
 * A Stop object represents any junction on the map that is relevant for
 * establishing train run length and revenue calculation. A Stop object is bound
 * to (1) a MapHex, (2) to a Station object on the current Tile laid on that
 * MapHex, and (3) any tokens laid on that tile and station. <p> Each Stop has a
 * unique ID, that is derived from the MapHex name and the Stop number. The
 * initial Stop numbers are derived from the Station numbers of the preprinted
 * tile of that hex. <p> Please note, that during upgrades the Stop numbers
 * related to a city on a multiple-city hex may change: city 1 on one tile may
 * be numbered 2 on its upgrade, depending on the rotation of the upgrading
 * tile. However, the Stop numbers will not change, unless cities are merged
 * during upgrades; but even then it is attempted to retain the old Stop numbers
 * as much as possible.
 *
 * @author Erik Vos
 */
public class Stop extends RailsAbstractItem implements Owner {
    private final int number;
    private String uniqueId;
    //private Station relatedStation;
    private GenericState<Station> relatedStation;
    private int slots;
    private PortfolioSet<BaseToken> tokens;
    private String trackEdges;


    private Type type = null;
    private RunTo runToAllowed = null;
    private RunThrough runThroughAllowed = null;
    private Loop loopAllowed = null;
    private Score scoreType = null;

    protected static Logger log =
        LoggerFactory.getLogger(Stop.class);

    public enum RunTo {
        YES,
        NO,
        TOKENONLY
    }

    public enum RunThrough {
        YES,
        NO,
        TOKENONLY
    }

    public enum Loop {
        YES,
        NO
    }

    public enum Type {

        CITY (RunTo.YES, RunThrough.YES, Loop.YES, Score.MAJOR),
        TOWN (RunTo.YES, RunThrough.YES, Loop.YES, Score.MINOR),
        OFFMAP (RunTo.YES, RunThrough.NO, Loop.NO, Score.MAJOR);

        private RunTo defaultRunToAllowed;
        private RunThrough defaultRunThroughAllowed;
        private Loop defaultLoopAllowed;
        private Score defaultScoreType;

        Type (RunTo runTo,
                RunThrough runThrough,
                Loop loop,
                Score scoreType) {

            this.defaultRunToAllowed = runTo;
            this.defaultRunThroughAllowed = runThrough;
            this.defaultLoopAllowed = loop;
            this.defaultScoreType = scoreType;
        }

        public RunTo getDefaultRunTo() { return defaultRunToAllowed; }
        public RunThrough getDefaultRunThrough() { return defaultRunThroughAllowed; }
        public Loop getDefaultLoop() { return defaultLoopAllowed; }
        public Score getDefaultScoreType() { return defaultScoreType; }

    }

    public enum Score {
        MAJOR,
        MINOR
    }

    private Stop(MapHex hex, int number) {
        super(hex, String.valueOf(number));
        this.number = number;
        uniqueId = getParent().getId() + "_" + number;
        tokens = PortfolioSet.create(this, "tokens", BaseToken.class);
        relatedStation = GenericState.create(this, "City_"+uniqueId+"_station");
    }

    // TODO: Can this all be simplified, maybe stop <=> station relationship is enough
    /**
     * returns initialized Stop
     */
    public static Stop create(MapHex hex, int number, Station station){
        Stop stop = new Stop(hex, number);
        stop.initStation(station);
        return stop;
    }

    /**
     * @param station that the stop refers to
     */
    public void initStation(Station station) {
        setRelatedStation(station);
        initStopProperties();
    }
    
    @Override
    public MapHex getParent() {
        return (MapHex)super.getParent();
    }

    private void initStopProperties () {

        Station station = relatedStation.value();
        Tile tile = station.getTile();
        MapManager mapManager = getParent().getParent();
        TileManager tileManager = tile.getTileManager();

        // Stop type
        type = getParent().getStopType();
        if (type == null) type = tile.getStopType();
        if (type == null) {
            String stationType = relatedStation.value().getType();
            if (stationType.equals(Station.CITY)) {
                type = Type.CITY;
            } else if (stationType.equals(Station.TOWN)) {
                type = Type.TOWN;
            } else if (stationType.equals(Station.OFF_MAP_AREA)) {
                type = Type.OFFMAP;
            } else if (stationType.equals(Station.PASS)) {
                type = Type.CITY;
            } else {
                // The above four types seem to be all that can be assigned in ConvertTileXML.
                // If all else fails, assume City.
                type = Type.CITY;
            }
        }

        // RunTo
        runToAllowed = getParent().isRunToAllowed();
        if (runToAllowed == null) runToAllowed = tile.isRunToAllowed();
        if (runToAllowed == null) runToAllowed = mapManager.getRunToDefault(type);
        if (runToAllowed == null) runToAllowed = tileManager.getRunToDefault(type);
        if (runToAllowed == null) runToAllowed = mapManager.getRunToDefault(null);
        if (runToAllowed == null) runToAllowed = tileManager.getRunToDefault(null);
        if (runToAllowed == null) runToAllowed = type.getDefaultRunTo();

        // RunThrough
        runThroughAllowed = getParent().isRunThroughAllowed();
        if (runThroughAllowed == null) runThroughAllowed = tile.isRunThroughAllowed();
        if (runThroughAllowed == null) runThroughAllowed = mapManager.getRunThroughDefault(type);
        if (runThroughAllowed == null) runThroughAllowed = tileManager.getRunThroughDefault(type);
        if (runThroughAllowed == null) runThroughAllowed = mapManager.getRunThroughDefault(null);
        if (runThroughAllowed == null) runThroughAllowed = tileManager.getRunThroughDefault(null);
        if (runThroughAllowed == null) runThroughAllowed = type.getDefaultRunThrough();

        // Loop
        loopAllowed = getParent().isLoopAllowed();
        if (loopAllowed == null) loopAllowed = tile.isLoopAllowed();
        if (loopAllowed == null) loopAllowed = mapManager.getLoopDefault(type);
        if (loopAllowed == null) loopAllowed = tileManager.getLoopDefault(type);
        if (loopAllowed == null) loopAllowed = mapManager.getLoopDefault(null);
        if (loopAllowed == null) loopAllowed = tileManager.getLoopDefault(null);
        if (loopAllowed == null) loopAllowed = type.getDefaultLoop();

        // Score type
        scoreType = getParent().getScoreType();
        if (scoreType == null) scoreType = tile.getScoreType();
        if (scoreType == null) scoreType = mapManager.getScoreTypeDefault(type);
        if (scoreType == null) scoreType = tileManager.getScoreTypeDefault(type);
        if (scoreType == null) scoreType = type.getDefaultScoreType();

        log.debug("+++ Hex="+getParent().getId()+" tile="+tile.getNb()+" city="+number
                +": stopType="+type+" runTo="+runToAllowed+" runThrough="+runThroughAllowed
                +" loop="+loopAllowed+" scoreType="+scoreType);
    }

    public String getId() {
        return getParent().getId() + "/" + number;

    }

    /**
     * @return Returns the holder.
     */
    public MapHex getHolder() {
        return getParent();
    }

    public int getNumber() {
        return number;
    }

    public Station getRelatedStation() {
        return relatedStation.value();
    }

    public void setRelatedStation(Station relatedStation) {
        this.relatedStation.set(relatedStation);
        slots = relatedStation.getBaseSlots();
        trackEdges =
            getParent().getConnectionString(getParent().getCurrentTile(),
                    getParent().getCurrentTileRotation(),
                    relatedStation.getNumber());
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    /**
     * @return Returns the id.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    public PortfolioSet<BaseToken> getBaseTokens() {
        return tokens;
    }

    public boolean hasTokens() {
        return tokens.size() > 0;
    }

    public int getSlots() {
        return slots;
    }

    public boolean hasTokenSlotsLeft() {
        return tokens.size() < slots;
    }

    public int getTokenSlotsLeft () {
        return slots - tokens.size();
    }

    /**
     * @param company
     * @return true if this Stop already contains an instance of the specified
     * company's token. Do this by calling the hasTokenOf with Company Name.
     * Using a tokens.contains(company) fails since the tokens are a ArrayList
     * of Token not a ArrayList of PublicCompany.
     */
    public boolean hasTokenOf(PublicCompany company) {
        return hasTokenOf (company.getId());
    }

    public boolean hasTokenOf (String companyName) {
        for (BaseToken token : tokens) {
            if (token.getParent().getId().equals(companyName)) {
                return true;
            }
        }
        return false;
    }

    public String getTrackEdges() {
        return trackEdges;
    }

    public void setTrackEdges(String trackEdges) {
        this.trackEdges = trackEdges;
    }

    public Type getType() {
        return type;
    }

    public Score getScoreType () {
        return scoreType;
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

    public boolean isRunToAllowedFor (PublicCompany company) {
        switch (runToAllowed) {
        case YES:
            return true;
        case NO:
            return false;
        case TOKENONLY:
            return hasTokenOf (company);
        default:
            // Dead code, only to satisfy the compiler
            return true;
        }
    }

    public boolean isRunThroughAllowedFor (PublicCompany company) {
        switch (runThroughAllowed) {
        case YES: // either it has no tokens at all, or it has a company tokens or empty token slots
            return !hasTokens() || hasTokenOf (company) || hasTokenSlotsLeft() ;
        case NO:
            return false;
        case TOKENONLY:
            return hasTokenOf (company);
        default:
            // Dead code, only to satisfy the compiler
            return true;
        }
    }

    public int getValueForPhase (Phase phase) {
        if (getParent().hasValuesPerPhase()) {
            return getParent().getCurrentValueForPhase(phase);
        } else {
            return relatedStation.value().getValue();
        }
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Hex ").append(getParent().getId());
        String cityName = getParent().getCityName();
        b.append(" (");
        if (Util.hasValue(cityName)) {
            b.append(cityName);
        }
        if (getParent().getStops().size() > 1) {
            b.append(" ").append(trackEdges);
        }
        b.append(")");
        return b.toString();
    }

}
