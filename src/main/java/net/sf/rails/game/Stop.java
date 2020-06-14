package net.sf.rails.game;

import net.sf.rails.game.StopType.Loop;
import net.sf.rails.game.StopType.RunThrough;
import net.sf.rails.game.StopType.RunTo;
import net.sf.rails.game.StopType.Score;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.HashSetState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.PortfolioSet;
import net.sf.rails.util.Util;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;


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
public class Stop extends RailsAbstractItem implements RailsOwner, Comparable<Stop> {
    private final PortfolioSet<BaseToken> tokens = PortfolioSet.create(this, "tokens", BaseToken.class);
    private final GenericState<Station> relatedStation = new GenericState<>(this, "station");

    private RunTo runTo;
    private RunThrough runThrough;
    private Loop loop;
    private Score score;
    private String mutexId;

    // FIXME: Only used for Rails1.x compatibility
    private final IntegerState legacyNumber = IntegerState.create(this, "legacyNumber", 0);

    // FIXME: Only used for Rails1.x compatibility
    private final HashSetState<Integer> previousNumbers = HashSetState.create(this, "previousNumbers");

    private Stop(MapHex hex, String id, Station station) {
        super(hex, id);

        relatedStation.set(station);
        tokens.addModel(hex);

        if (station != null) {
            legacyNumber.set(station.getNumber());
        }
    }

    public static Stop create(MapHex hex, Station station) {
        if (station == null) {
            return new Stop(hex, "0", null);
        } else {
            return new Stop(hex, String.valueOf(station.getNumber()), station);
        }
    }

    @Override
    public MapHex getParent() {
        return (MapHex) super.getParent();
    }

    // This should not be used for identification reasons
    // It is better to use the getRelatedNumber()
    @Deprecated
    public String getSpecificId() {
        return getParent().getId() + "/" + this.getRelatedNumber();
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

    public int getTokenSlotsLeft() {
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

    public void initStopParameters () {
        // First set the mutexId, which can only be specified in Station or MapHex
        if (getParent().getId().equalsIgnoreCase("F1")) {
            int x = 1;
        }
        mutexId = getRelatedStation().getMutexId();
        if (mutexId == null) mutexId = getParent().getMutexId();

        // Related station on current tile
        if (getAccessFields(getRelatedStation().getStopType())) return;
        // Current Tile
        if (getAccessFields(getParent().getCurrentTile().getStopType())) return;
        // MapHex
        if (getAccessFields(getParent().getStopType())) return;
        // Access fields not yet complete, defaults apply. First we need the stop type name.
        String typeName = getRelatedStation().getStopType().getId();
        // TileManager defaults
        if (getAccessFields(getParent().getCurrentTile().getParent().getDefaultStopTypes().get(typeName))) return;
        // MapManager defaults
        if (getAccessFields(getParent().getParent().getDefaultStopTypes().get(typeName))) return;
        // Built-in defaults
        if (getAccessFields(StopType.Defaults.valueOf(typeName).getStopType())) return;
        // The ultimate fall-back
        getAccessFields(StopType.Defaults.CITY.getStopType());
   }

    /**
     * Set those access fields that are still unset.
     * @param stopType Access parameters of a certain leve
     * @return true if all access parameters have a value
     */
    private boolean getAccessFields(StopType stopType) {
        if (stopType == null) return false;
        if (runTo == null) runTo = stopType.getRunToAllowed();
        if (runThrough == null) runThrough = stopType.getRunThroughAllowed();
        if (loop == null) loop = stopType.getLoopAllowed();
        if (score == null) score = stopType.getScoreType();
        return runTo != null && runThrough != null && loop != null && score != null;
    }
    /**
     * @return true if stop is tokenable, thus it has open token slots and no company token yet
     */
    public boolean isTokenableFor(PublicCompany company) {
        return hasTokenSlotsLeft() && !hasTokenOf(company);
    }

    public RunTo getRunToAllowed() {
       return runTo;
    }

    public RunThrough getRunThroughAllowed() {
      return runThrough;
    }

    public Loop getLoopAllowed() {
        return loop;
    }

    public Score getScoreType() {
        return score;
    }

    public String getMutexId() {
        return mutexId;
    }

    public boolean isRunToAllowedFor(PublicCompany company, boolean running) {

        switch (getRunToAllowed()) {
            case YES:
                return true;
            case NO:
                return false;
            case TOKENONLY:
                // Must return true when preparing tile and token laying,
                // false in all other cases (to show the optimal train routes).
                return !running || hasTokenOf(company);
            default:
                // Dead code, only to satisfy the compiler
                return true;
        }
    }

    public boolean isRunThroughAllowedFor(PublicCompany company) {
        switch (getRunThroughAllowed()) {
            case YES: // either it has no tokens at all, or it has a company tokens or empty token slots
                return !hasTokens() || hasTokenOf(company) || hasTokenSlotsLeft();
            case NO:
                return false;
            case TOKENONLY:
                return hasTokenOf(company);
            default:
                // Dead code, only to satisfy the compiler
                return true;
        }
    }

    public int getValueForPhase(Phase phase) {
        if (getParent().hasValuesPerPhase()) {
            return getParent().getCurrentValueForPhase(phase);
        } else {
            return relatedStation.value().getValue();
        }
    }

    @Override
    public int compareTo(Stop o) {
        return ComparisonChain.start()
                .compare(o.getRelatedStation().getValue(), this.getRelatedStation().getValue())
                .compare(o.getTokenSlotsLeft(), this.getTokenSlotsLeft())
                .compare(this.getId(), o.getId())
                .result()
                ;
    }

    @Override
    public String toText() {
        StringBuilder b = new StringBuilder();
        b.append("Hex ").append(getParent().getId());
        String cityName = getParent().getStopName();
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
