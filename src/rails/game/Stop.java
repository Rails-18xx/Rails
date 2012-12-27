package rails.game;

import com.google.common.collect.ImmutableSet;

import rails.game.StopType.Loop;
import rails.game.StopType.RunThrough;
import rails.game.StopType.RunTo;
import rails.game.StopType.Score;
import rails.game.state.GenericState;
import rails.game.state.HashSetState;
import rails.game.state.IntegerState;
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
 */
public class Stop extends RailsAbstractItem implements RailsOwner {
    private final PortfolioSet<BaseToken> tokens = 
            PortfolioSet.create(this, "tokens", BaseToken.class);
    private final GenericState<Station> relatedStation = 
            GenericState.create(this, "station");
    // FIXME: Only used for Rails1.x compatibility
    private final IntegerState legacyNumber = 
            IntegerState.create(this, "legacyNumber", 0);
    // FIXME: Only used for Rails1.x compatibility
    private final HashSetState<Integer> previousNumbers = 
            HashSetState.create(this, "previousNumbers");

    private Stop(MapHex hex, String id, Station station) {
        super(hex, id);
        relatedStation.set(station);
        if (station != null) {
            legacyNumber.set(station.getNumber());
        }
    }

    public static Stop create(MapHex hex, Station station){
        if (station == null) {
            return new Stop(hex, "0", null);
        } else {
            return new Stop(hex, String.valueOf(station.getNumber()), station);
        }
    }
    
    @Override
    public MapHex getParent() {
        return (MapHex)super.getParent();
    }

    // This should not be used for identification reasons
    // It is better to use the getRelatedNumber()
    @Deprecated
    public String getSpecificId() {
        return getParent().getId() + "/" + this.getId();
    }

    public Station getRelatedStation() {
        return relatedStation.value();
    }
    
    public void setRelatedStation(Station station) {
        relatedStation.set(station);
    }
    
    // FIMXE: Due to Rails1.x compatibility use the legacy number 
    public int getRelatedNumber() {
        // return relatedStation.value().getNumber();
        return getLegacyNumber();
    }

    // FIMXE: Due to Rails1.x compatibility
    @Deprecated
    public int getLegacyNumber() {
        return legacyNumber.value();
    }

    // FIMXE: Due to Rails1.x compatibility
    @Deprecated
    public boolean checkPreviousNumbers(int number) {
        return previousNumbers.contains(number);
    }
    
    // FIMXE: Due to Rails1.x compatibility
    @Deprecated
    public void addPreviousNumbers(int number) {
        previousNumbers.add(number);
    }
    
    public ImmutableSet<BaseToken> getBaseTokens() {
        return tokens.items();
    }

    public boolean hasTokens() {
        return tokens.size() > 0;
    }

    public int getSlots() {
        return relatedStation.value().getBaseSlots();
    }

    public boolean hasTokenSlotsLeft() {
        return tokens.size() < getSlots();
    }

    public int getTokenSlotsLeft () {
        return getSlots() - tokens.size();
    }

    /**
     * @param company
     * @return true if this Stop already contains an instance of the specified
     * company's token. Do this by calling the hasTokenOf with Company Name.
     * Using a tokens.contains(company) fails since the tokens are a ArrayList
     * of Token not a ArrayList of PublicCompany.
     */
    public boolean hasTokenOf(PublicCompany company) {
        for (BaseToken token : tokens) {
            if (token.getParent() == company) {
                return true;
            }
        }
        return false;
    }

    public RunTo getRunToAllowed() {
        RunTo runTo = getParent().getStopType().getRunToAllowed();
        if (runTo == null) runTo = getParent().getCurrentTile().getStopType().getRunToAllowed();
        if (runTo == null) runTo = getRelatedStation().getStopType().getRunToAllowed();
        return runTo;
    }

    public RunThrough getRunThroughAllowed() {
        RunThrough runThrough = getParent().getStopType().getRunThroughAllowed();
        if (runThrough == null) runThrough = getParent().getCurrentTile().getStopType().getRunThroughAllowed();
        if (runThrough == null) runThrough = getRelatedStation().getStopType().getRunThroughAllowed();
        return runThrough;
    }

    public Loop getLoopAllowed() {
        Loop loopAllowed = getParent().getStopType().getLoopAllowed();
        if (loopAllowed == null) loopAllowed = getParent().getCurrentTile().getStopType().getLoopAllowed();
        if (loopAllowed == null) loopAllowed = getRelatedStation().getStopType().getLoopAllowed();
        return loopAllowed;
    }

    public Score getScoreType () {
        Score scoreType = getParent().getStopType().getScoreType();
        if (scoreType == null) scoreType = getParent().getCurrentTile().getStopType().getScoreType();
        if (scoreType == null) scoreType = getRelatedStation().getStopType().getScoreType();
        return scoreType;
    }

    public boolean isRunToAllowedFor (PublicCompany company) {
        switch (getRunToAllowed()) {
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
        switch (getRunThroughAllowed()) {
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
    public String toText() {
        StringBuffer b = new StringBuffer();
        b.append("Hex ").append(getParent().getId());
        String cityName = getParent().getCityName();
        b.append(" (");
        if (Util.hasValue(cityName)) {
            b.append(cityName);
        }
        if (getParent().getStops().size() > 1) {
            b.append(" ").append(getParent().getConnectionString(relatedStation.value()));
        }
        b.append(")");
        return b.toString();
    }

}
