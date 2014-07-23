package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainManager;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class DiscardTrain extends PossibleORAction {

    // Server settings
    transient private Set<Train> ownedTrains = null;
    private String[] ownedTrainsUniqueIds;

    /** True if discarding trains is mandatory */
    boolean forced = false;

    // Client settings
    transient private Train discardedTrain = null;
    private String discardedTrainUniqueId;

    public static final long serialVersionUID = 1L;

    public DiscardTrain(PublicCompany company, Set<Train> trains) {

        super();
        this.ownedTrains = trains;
        this.ownedTrainsUniqueIds = new String[trains.size()];
        int i = 0;
        for (Train train:trains) {
            ownedTrainsUniqueIds[i++] = train.getId();
        }
        this.company = company;
        this.companyName = company.getId();
    }

    public DiscardTrain(PublicCompany company, Set<Train> trainsToDiscardFrom,
            boolean forced) {
        this(company, trainsToDiscardFrom);
        this.forced = forced;
    }

    public Set<Train> getOwnedTrains() {
        return ownedTrains;
    }

    public void setDiscardedTrain(Train train) {
        discardedTrain = train;
        discardedTrainUniqueId = train.getId();
    }

    public Train getDiscardedTrain() {
        return discardedTrain;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        DiscardTrain action = (DiscardTrain)pa; 
        boolean options = Objects.equal(this.ownedTrains, action.ownedTrains)
                && Objects.equal(this.forced, action.forced)
        ;

        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.discardedTrain, action.discardedTrain)
        ;
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
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        TrainManager trainManager = RailsRoot.getInstance().getTrainManager();

        if (discardedTrainUniqueId != null) {
            discardedTrain = trainManager.getTrainByUniqueId(discardedTrainUniqueId);
        }

        if (ownedTrainsUniqueIds != null && ownedTrainsUniqueIds.length > 0) {
            ownedTrains = new HashSet<Train>();
            for (int i = 0; i < ownedTrainsUniqueIds.length; i++) {
                ownedTrains.add(trainManager.getTrainByUniqueId(ownedTrainsUniqueIds[i]));
            }
        }
    }

}
