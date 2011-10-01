/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/DiscardTrain.java,v 1.11 2009/12/27 18:30:11 evos Exp $
 *
 * Created on 20-May-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class DiscardTrain extends PossibleORAction {

    // Server settings
    transient private List<Train> ownedTrains = null;
    private String[] ownedTrainsUniqueIds;

    /** True if discarding trains is mandatory */
    boolean forced = false;

    // Client settings
    transient private Train discardedTrain = null;
    private String discardedTrainUniqueId;

    public static final long serialVersionUID = 1L;

    public DiscardTrain(PublicCompany company, List<Train> trains) {

        super();
        this.ownedTrains = trains;
        this.ownedTrainsUniqueIds = new String[trains.size()];
        for (int i = 0; i < trains.size(); i++) {
            ownedTrainsUniqueIds[i] = trains.get(i).getUniqueId();
        }
        this.company = company;
        this.companyName = company.getId();
    }

    public DiscardTrain(PublicCompany company, List<Train> trains,
            boolean forced) {
        this(company, trains);
        this.forced = forced;
    }

    public List<Train> getOwnedTrains() {
        return ownedTrains;
    }

    public void setDiscardedTrain(Train train) {
        discardedTrain = train;
        discardedTrainUniqueId = train.getUniqueId();
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
            ownedTrains = new ArrayList<Train>();
            for (int i = 0; i < ownedTrainsUniqueIds.length; i++) {
                ownedTrains.add(trainManager.getTrainByUniqueId(ownedTrainsUniqueIds[i]));
            }
        }
    }

}
