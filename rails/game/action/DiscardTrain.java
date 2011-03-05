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
    transient private List<TrainI> ownedTrains = null;
    private String[] ownedTrainsUniqueIds;

    /** True if discarding trains is mandatory */
    boolean forced = false;

    // Client settings
    transient private TrainI discardedTrain = null;
    private String discardedTrainUniqueId;

    public static final long serialVersionUID = 1L;

    public DiscardTrain(PublicCompanyI company, List<TrainI> trains) {

        super();
        this.ownedTrains = trains;
        this.ownedTrainsUniqueIds = new String[trains.size()];
        for (int i = 0; i < trains.size(); i++) {
            ownedTrainsUniqueIds[i] = trains.get(i).getUniqueId();
        }
        this.company = company;
        this.companyName = company.getName();
    }

    public DiscardTrain(PublicCompanyI company, List<TrainI> trains,
            boolean forced) {
        this(company, trains);
        this.forced = forced;
    }

    public List<TrainI> getOwnedTrains() {
        return ownedTrains;
    }

    public void setDiscardedTrain(TrainI train) {
        discardedTrain = train;
        discardedTrainUniqueId = train.getUniqueId();
    }

    public TrainI getDiscardedTrain() {
        return discardedTrain;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public String toString() {

        StringBuffer b = new StringBuffer();
        b.append("Discard train: ").append(company.getName());
        b.append(" one of");
        for (TrainI train : ownedTrains) {
            b.append(" ").append(train.getName());
        }
        b.append(forced ? "" : ", not").append (" forced");
        if (discardedTrain != null) {
            b.append(", discards ").append(discardedTrain.getName());
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
            ownedTrains = new ArrayList<TrainI>();
            for (int i = 0; i < ownedTrainsUniqueIds.length; i++) {
                ownedTrains.add(trainManager.getTrainByUniqueId(ownedTrainsUniqueIds[i]));
            }
        }
    }

}
