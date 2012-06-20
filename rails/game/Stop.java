/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Stop.java,v 1.12 2010/04/18 15:08:57 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.move.Moveable;
import rails.game.state.GenericState;
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
public class Stop implements TokenHolder {
    private int number;
    private String uniqueId;
    //private Station relatedStation;
    private GenericState<Station> relatedStation;
    private int slots;
    private ArrayList<TokenI> tokens;
    private MapHex mapHex;
    private String trackEdges;


    private Type type = null;
    private RunTo runToAllowed = null;
    private RunThrough runThroughAllowed = null;
    private Loop loopAllowed = null;
    private Score scoreType = null;

    protected static Logger log =
        Logger.getLogger(Stop.class.getPackage().getName());

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

    public Stop(MapHex mapHex, int number, Station station) {
        this.mapHex = mapHex;
        this.number = number;

        uniqueId = mapHex.getName() + "_" + number;
        relatedStation = new GenericState<Station>("City_"+uniqueId+"_station", station);
        setRelatedStation(station);

        tokens = new ArrayList<TokenI>(4);

        initStopProperties();
    }

    private void initStopProperties () {

        Station station = relatedStation.get();
        TileI tile = station.getTile();
        MapManager mapManager = mapHex.getMapManager();
        TileManager tileManager = tile.getTileManager();

        // Stop type
        type = mapHex.getStopType();
        if (type == null) type = tile.getStopType();
        if (type == null) {
            String stationType = relatedStation.get().getType();
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
        runToAllowed = mapHex.isRunToAllowed();
        if (runToAllowed == null) runToAllowed = tile.isRunToAllowed();
        if (runToAllowed == null) runToAllowed = mapManager.getRunToDefault(type);
        if (runToAllowed == null) runToAllowed = tileManager.getRunToDefault(type);
        if (runToAllowed == null) runToAllowed = mapManager.getRunToDefault(null);
        if (runToAllowed == null) runToAllowed = tileManager.getRunToDefault(null);
        if (runToAllowed == null) runToAllowed = type.getDefaultRunTo();

        // RunThrough
        runThroughAllowed = mapHex.isRunThroughAllowed();
        if (runThroughAllowed == null) runThroughAllowed = tile.isRunThroughAllowed();
        if (runThroughAllowed == null) runThroughAllowed = mapManager.getRunThroughDefault(type);
        if (runThroughAllowed == null) runThroughAllowed = tileManager.getRunThroughDefault(type);
        if (runThroughAllowed == null) runThroughAllowed = mapManager.getRunThroughDefault(null);
        if (runThroughAllowed == null) runThroughAllowed = tileManager.getRunThroughDefault(null);
        if (runThroughAllowed == null) runThroughAllowed = type.getDefaultRunThrough();

        // Loop
        loopAllowed = mapHex.isLoopAllowed();
        if (loopAllowed == null) loopAllowed = tile.isLoopAllowed();
        if (loopAllowed == null) loopAllowed = mapManager.getLoopDefault(type);
        if (loopAllowed == null) loopAllowed = tileManager.getLoopDefault(type);
        if (loopAllowed == null) loopAllowed = mapManager.getLoopDefault(null);
        if (loopAllowed == null) loopAllowed = tileManager.getLoopDefault(null);
        if (loopAllowed == null) loopAllowed = type.getDefaultLoop();

        // Score type
        scoreType = mapHex.getScoreType();
        if (scoreType == null) scoreType = tile.getScoreType();
        if (scoreType == null) scoreType = mapManager.getScoreTypeDefault(type);
        if (scoreType == null) scoreType = tileManager.getScoreTypeDefault(type);
        if (scoreType == null) scoreType = type.getDefaultScoreType();

        //log.debug("+++ Hex="+mapHex.getName()+" tile="+tile.getId()+" city="+number
        //        +": stopType="+type+" runTo="+runToAllowed+" runThrough="+runThroughAllowed
        //        +" loop="+loopAllowed+" scoreType="+scoreType);
    }

    public String getName() {
        return mapHex.getName() + "/" + number;

    }

    /**
     * @return Returns the holder.
     */
    public MapHex getHolder() {
        return mapHex;
    }

    public int getNumber() {
        return number;
    }

    public Station getRelatedStation() {
        return relatedStation.get();
    }

    public void setRelatedStation(Station relatedStation) {
        this.relatedStation.set(relatedStation);
        slots = relatedStation.getBaseSlots();
        trackEdges =
            mapHex.getConnectionString(mapHex.getCurrentTile(),
                    mapHex.getCurrentTileRotation(),
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

    public boolean addToken(TokenI token, int position) {

        if (tokens.contains(token)) return false;

        boolean result = Util.addToList(tokens, token, position);
        if (result) token.setHolder(this);
        return result;
    }

    public boolean addObject(Moveable object, int[] position) {
        if (object instanceof TokenI) {
            mapHex.update();
            return addToken((TokenI) object, position == null ? -1 : position[0]);
        } else {
            return false;
        }
    }

    public boolean removeObject(Moveable object) {
        if (object instanceof TokenI) {
            mapHex.update();
            return removeToken((TokenI) object);
        } else {
            return false;
        }
    }

    public List<TokenI> getTokens() {
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

    public boolean removeToken(TokenI token) {

        boolean result = tokens.remove(token);
        return result;
    }

    /**
     * @param company
     * @return true if this Stop already contains an instance of the specified
     * company's token. Do this by calling the hasTokenOf with Company Name.
     * Using a tokens.contains(company) fails since the tokens are a ArrayList
     * of TokenI not a ArrayList of PublicCompanyI.
     */
    public boolean hasTokenOf(PublicCompanyI company) {
        return hasTokenOf (company.getName());
    }

    public boolean hasTokenOf (String companyName) {
        for (TokenI token : tokens) {
            if (token instanceof BaseToken
                    && ((BaseToken)token).getCompany().getName().equals(companyName)) {
                return true;
            }
        }
        return false;
    }

    public int[] getListIndex (Moveable object) {
        if (object instanceof BaseToken) {
            return new int[] {tokens.indexOf(object)};
        } else {
            return Moveable.AT_END;
        }
    }

    public void setTokens(ArrayList<TokenI> tokens) {
        this.tokens = tokens;
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

    public boolean isRunToAllowedFor (PublicCompanyI company) {
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

    public boolean isRunThroughAllowedFor (PublicCompanyI company) {
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

    public int getValueForPhase (PhaseI phase) {
        if (mapHex.hasValuesPerPhase()) {
            return mapHex.getCurrentValueForPhase(phase);
        } else {
            return relatedStation.get().getValue();
        }
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Hex ").append(mapHex.getName());
        String cityName = mapHex.getCityName();
        b.append(" (");
        if (Util.hasValue(cityName)) {
            b.append(cityName);
        }
        if (mapHex.getStops().size() > 1) {
            b.append(" ").append(trackEdges);
        }
        b.append(")");
        return b.toString();
    }
}
