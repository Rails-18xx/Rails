package net.sf.rails.game;

import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.HashSetState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.PortfolioSet;
import net.sf.rails.util.Util;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


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

    private static final Logger log = LoggerFactory.getLogger(Stop.class);

    private Access.RunTo runTo;
    private List<String> runToTrainCategories;
    private Access.RunThrough runThrough;
    private Access.Score score;
    private String mutexId;

    public enum Type {
        CITY,
        TOWN,
        OFFMAP,
        MINE,
        PORT,
        PASS
    }

    private final IntegerState number = IntegerState.create(this, "legacyNumber", 0);

    // FIXME: Only used for Rails1.x compatibility
    private final HashSetState<Integer> previousNumbers = HashSetState.create(this, "previousNumbers");

    private Stop(MapHex hex, String id, Station station) {
        super(hex, id);

        relatedStation.set(station);
        tokens.addModel(hex);

        if (station != null) {
            number.set(Integer.parseInt(id));
        }
    }

    public static Stop create(MapHex hex, Station station) {
        if (station == null) {
            return new Stop(hex, "0", null);
        } else {
            return new Stop(hex, String.valueOf(station.getNumber()), station);
        }
    }

    public static Stop create (MapHex hex, int stopNumber, Station station) {
        log.debug("Creating new stop on {}: {} - {}", hex, stopNumber, station);
        return new Stop (hex, String.valueOf(stopNumber), station);
    }

    public static Stop getInstance(RailsItem item, String fullURI) {
        return (Stop) item.getRoot().locate(fullURI);
    }

    @Override
    public MapHex getParent() {
        return (MapHex) super.getParent();
    }

    public String getStationComposedId() {
        return getParent().getId() + "/" + this.getRelatedStationNumber();
    }

    public String getStopComposedId() {
        return getParent().getId() + "/" + this.number;
    }

    public Station getRelatedStation() {
        return relatedStation.value();
    }

    public void setRelatedStation(Station station) {
        relatedStation.set(station);
    }

    public int getRelatedStationNumber() {
        return relatedStation.value().getNumber();
    }

    public int getNumber() {
        return number.value();
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
     * @param company Operating company
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

    public void initStopParameters (Station station) {

        boolean complete;

        log.debug("--- For hex {} {}", getParent().getId(), getRelatedStation().toString());

        // Related station on current tile
        // To be used only with different stops (qua properties) properties on the same tile
        complete = updateAccessFields(getRelatedStation().getAccess());
        log.debug("After Station: runTo={} {} runThrough={} mutexId={} score={}",
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
        if (complete) return;

        // Current Tile
        // Possible (in  TileSet.xml, NOT Tiles.xml!) but not yet used, and not recommended.
        complete = updateAccessFields(getParent().getCurrentTile().getAccess());
        log.debug("After Tile ({}.{}): runTo={} {} runThrough={} mutexId={} score={}",
                getParent().getCurrentTile(), station,
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
        if (complete) return;

        // MapHex
        // The recommended place to specify location-specific run and loop specialties.
        complete = updateAccessFields(getParent().getAccess());
        log.debug("After MapHex: runTo={} {} runThrough={} mutexId={} score={}",
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
        if (complete) return;

        // Access fields not yet complete, defaults apply. First we need the stop type name.
        Stop.Type type = getRelatedStation().getType();
        log.debug("Type = {}", type.toString());

        // TileManager defaults
        // Possible, but no yet used, and not recommended.
        complete = updateAccessFields(getParent().getCurrentTile().getParent().getDefaultAccessType(type));
        log.debug("After TileManager defaults: runTo={} {} runThrough={} mutexId={} score={}",
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
        if (complete) return;

        // MapManager defaults
        // The appropriate place to specify defaults.
        complete = updateAccessFields(getParent().getParent().getDefaultAccessType(type));
        log.debug("After MapManager defaults: runTo={} {} runThrough={} mutexId={} score={}",
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
        if (complete) return;

        // Built-in defaults
        // Defined in class Access, not changeable.
        complete = updateAccessFields(Access.getDefault(type));
        log.debug("After built-in defaults: runTo={} {} runThrough={} mutexId={} score={}",
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
        if (complete) return;

         // The ultimate fall-back
        updateAccessFields(Access.getDefault(Stop.Type.CITY));
        log.debug("After last resort default: runTo={} {} runThrough={} mutexId={} score={}",
                runTo, Util.join(runToTrainCategories, ","), runThrough, mutexId, score);
   }

    /**
     * Set those access fields that are still unset.
     * @param access Access parameters of a certain leve
     * @return true if all access parameters have a value
     */
    private boolean updateAccessFields(Access access) {
        if (access == null) return false;
        if (runTo == null) runTo = access.getRunToAllowed();
        if (runToTrainCategories == null || runToTrainCategories.isEmpty())
                runToTrainCategories = access.getRunToTrainCategories();
        if (runThrough == null) runThrough = access.getRunThroughAllowed();
        if (score == null) score = access.getScoreType();
        if (mutexId == null) mutexId = access.getMutexId();
        return runTo != null && runThrough != null && score != null;  // mutexId may stay null
    }
    /**
     * @return true if stop is tokenable, thus it has open token slots and no company token yet
     */
    public boolean isTokenableFor(PublicCompany company) {
        return hasTokenSlotsLeft() && !hasTokenOf(company);
    }

    public Access.RunTo getRunToAllowed() {
       return runTo;
    }

    public Access.RunThrough getRunThroughAllowed() {
      return runThrough;
    }

    public Access.Score getScoreType() {
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
        int fixedValue = relatedStation.value().getValue();
        if (getParent().hasValuesPerPhase() && fixedValue <= 0) {
            // Don't override a fixed value defined on the tile.
            // This matters for 1837 hex J34, which has
            // a fixed town value and a phase-dependent mine value (ZKB).
            return getParent().getCurrentValueForPhase(phase);
        } else {
            return fixedValue;
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

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getParent().getId());
        if (getParent().getStops().size() > 1
                // AFAIK, nulls are seen in the test log only
                && relatedStation != null && relatedStation.value() != null) {
            b.append("/")
                    .append(getRelatedStationNumber())
                    .append("(")
                    .append(getParent().getConnectionString(relatedStation.value()))
                    .append(")");
        }
        return b.toString();
    }
}
