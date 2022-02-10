package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.state.AbstractItem;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Objects;

import net.sf.rails.util.RailsObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rails 2.0: Updated equals and toString methods
 *
 * Update of 6 oct 2021 by Erik Vos:
 * It turns out that loading saved files of runs that include TrainDiscard objects
 * may erratically succeed or fail. This also can break test runs of such files.
 * This is due to either or both of:
 * - different train sequences in the set of owned trains,
 * - different train ids between load or test runs.
 *
 * I suspect that this is caused by the (IMHO) ill-advised replacements
 * of List by Set collections. Almost always, order matters in Rails!
 *
 * To fix this, the following changes have been applied to the DiscardTrain class:
 *
 * - equalsAs has been modified to compare *sorted* train type sets (TreeSet objects),
 * - getDiscardedTrain has been modified to return the train in ownedTrains
 *   that has the same train *type* as the given discardedTrain.
 *   (so the actual train id may be different).
 */
public class DiscardTrain extends PossibleORAction {

    private static final Logger log = LoggerFactory.getLogger(DiscardTrain.class);

    // Server settings
    transient private SortedSet<Train> ownedTrains;
    private String[] ownedTrainsUniqueIds;

    /**
     * True if discarding trains is mandatory
     */
    private boolean forced = false;

    // Client settings
    transient private Train discardedTrain = null;
    private String discardedTrainUniqueId;

    public static final long serialVersionUID = 1L;

    public DiscardTrain(PublicCompany company, @NotNull Set<Train> ownedTrains) {
        super(company.getRoot());
        this.ownedTrains = new TreeSet<>(Comparator.comparing(AbstractItem::getId));
        setOwnedTrains(ownedTrains);
        this.company = company;
        this.companyName = company.getId();
    }

    public DiscardTrain(PublicCompany company, Set<Train> trainsToDiscardFrom,
                        boolean forced) {
        this(company, trainsToDiscardFrom);
        this.forced = forced;
    }

    // Also used in ListAndFixSavedFiles
    public void setOwnedTrains(Set<Train> trains) {

        ownedTrains.clear();
        ownedTrains.addAll(trains);
        ownedTrainsUniqueIds = new String[trains.size()];
        int i = 0;
        for (Train train : ownedTrains) {
            ownedTrainsUniqueIds[i++] = train.getId();
        }
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public SortedSet<Train> getOwnedTrains() {
        return ownedTrains;
    }

    public void setDiscardedTrain(Train train) {
        if (train != null) {
            discardedTrain = train;
            discardedTrainUniqueId = train.getId();
        }
    }

    public Train getDiscardedTrain() {
        return discardedTrain;
    }

    public boolean isForced() {
        return forced;
    }

    /**
     * This method replaces various (at least 4)
     * almost identical discardTrain() methods.
     * Only the follow-up actions (finishTurn() etc.) are different
     * and must be done in the calling round.
     *
     * @param round The current round
     * @return True if train discarding finishes successfully
     */
    public boolean process (Round round) {

        // NOTE: not sure if the !forced part applies to all use cases
        if (discardedTrain == null && !forced) return true;

        /*--- Validation ---*/
        String errMsg = null;

        // Dummy loop
        while (true) {
            if (round instanceof OperatingRound) {
                // Must be in the correct step
                GameDef.OrStep step = ((OperatingRound) round).getStep();
                if (step != GameDef.OrStep.BUY_TRAIN && step != GameDef.OrStep.DISCARD_TRAINS) {
                    errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                    break;
                }
            }
            // Must specify a train if discard in mandatory
            if (discardedTrain == null && forced) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }
            // Does company own the specified train?
            if (!company.getPortfolioModel().getTrainList().contains(discardedTrain)) {
                errMsg = LocalText.getText("CompanyDoesNotOwnTrain",
                        companyName, discardedTrain.toText());
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add (round, LocalText.getText("CannotDiscardTrain",
                    companyName,
                    (discardedTrain != null ? discardedTrain.toText() : "?"),
                    errMsg));
            return false;
        }

        /*--- Execution ---*/
        discardedTrain.getCard().discard();

        // NOTE: any follow-up actions to be specified in the calling round
        return true;
    }

    /**
     * Check if an incoming action exists in the list of possible actions.
     * This method version additionally corrects the action to be processed
     * for any differences in the actual train ids.
     * NOTE: 'this' is one of the list of allowed ('possible') actions
     * @param pa The action to be validated
     * @param asOption True if only the preset items should be compared.
     * @return True if this instance is equal to the argument action
     */
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // 'action' is the possible action against which this object is checked.
        DiscardTrain executedAction = (DiscardTrain) pa;
        boolean idsChanged = !areTrainListsEqual(ownedTrains, executedAction.ownedTrains);

        // check asOption attributes
        // Only the train types have to be identical
        boolean options = Objects.equal(getOrderedTypes(ownedTrains), getOrderedTypes(executedAction.ownedTrains));

        // finish if asOptions check
        // Note: only reloaded actions are checked as action.
        options = options && forced == executedAction.forced;

        // Now we have checked correctness, we need to fix action trains order and actual ids.
        // See the top Javadoc.
        if (idsChanged) {
            executedAction.fixIds(this);
            log.info("+++ Action corrected to {}", executedAction);
        }

        return options;
    }

    /**
     * Fix the incoming train ids in case the owned train ids differ in value or sequence.
     * The validity (occurrence in possible actions) has already been checked.
     * @param from The possible action that has the new values.
     */
    private void fixIds (DiscardTrain from) {
        setOwnedTrains(from.getOwnedTrains());
        if (discardedTrain != null) {
            for (Train train : ownedTrains) {
                if (train.getType().equals(discardedTrain.getType())) {
                    setDiscardedTrain(train);
                    return;
                }
            }
        }
    }

    private boolean areTrainListsEqual (Set<Train> a, Set<Train> b) {
        /* Used in checking equality of train lists.
         * See the top Javadoc.
         */
        for (Train train : a) {
            if (!b.contains(train)) return false;
        }
        return true;
    }

    private SortedSet<TrainType> getOrderedTypes (Set<Train> trains) {
        SortedSet<TrainType> orderedTypes = new TreeSet<>(Comparator.comparing(TrainType::getName));
        for (Train train : trains) {
            orderedTypes.add(train.getType());
        }
        return orderedTypes;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                        .addToString("ownedTrains", ownedTrains)
                        .addToString("forced", forced)
                        .addToStringOnlyActed("discardedTrain", discardedTrain)
                        .toString()
                ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        TrainManager trainManager = root.getTrainManager();

        this.ownedTrains = new TreeSet<>(Comparator.comparing(AbstractItem::getId));
        if ( ownedTrainsUniqueIds != null && ownedTrainsUniqueIds.length > 0 ) {
            for ( String ownedTrainsUniqueId : ownedTrainsUniqueIds ) {
                ownedTrains.add(trainManager.getTrainByUniqueId(ownedTrainsUniqueId));
            }
        }
        if ( discardedTrainUniqueId != null ) {
            discardedTrain = trainManager.getTrainByUniqueId(discardedTrainUniqueId);
        }
    }

}
