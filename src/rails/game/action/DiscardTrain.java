package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import rails.game.*;

/**
 * @author Erik Vos
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
    public String toString() {

        StringBuffer b = new StringBuffer();
        b.append("Discard train: ").append(company.getId());
        b.append(" one of");
        for (Train train : ownedTrains) {
            b.append(" ").append(train.getId());
        }
        b.append(forced ? "" : ", not").append (" forced");
        if (discardedTrain != null) {
            b.append(", discards ").append(discardedTrain.getId());
        } else if ("".equalsIgnoreCase(getPlayerName())) {
            b.append(", discards nothing");
        }
        return b.toString();
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof DiscardTrain)) return false;
        DiscardTrain a = (DiscardTrain) action;
        return a.ownedTrains == ownedTrains && a.company == company;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof DiscardTrain)) return false;
        DiscardTrain a = (DiscardTrain) action;
        return a.discardedTrain == discardedTrain && a.company == company;
    }
    
    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        TrainManager trainManager = GameManager.getInstance().getTrainManager();

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
