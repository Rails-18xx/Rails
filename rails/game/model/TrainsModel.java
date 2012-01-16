package rails.game.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;

import rails.game.TrainCertificateType;
import rails.game.Train;
import rails.game.state.Item;
/**
 * A holder model that stores train objects
 * 
 * Currently it can only be created inside a portfolio, due 
 * to restrictions in the BuyTrain actions.
 * @author freystef
 */

public class TrainsModel extends StorageModel<Train> {

    private boolean abbrList = false;

    private TrainsModel() {
        super(Train.class);
    }

    public static TrainsModel create(Owner owner) {
        TrainsModel trainsModel = new TrainsModel().init(owner);
        owner.addStorage(trainsModel, Train.class);
        return trainsModel;
    }
    
    public TrainsModel init(Item parent) {
        super.init(parent);
        return this;
    }
    
    public void setAbbrList(boolean abbrList) {
        this.abbrList = abbrList;
    }
    
    public ImmutableList<Train> getTrains() {
        return this.view();
    }
    
    public Train getTrainOfType(TrainCertificateType type) {
        for (Train train:this) {
            if (train.getCertType() == type) return train;
        }
        return null;
    }
    
    /**
     * Make a full list of trains, like "2 2 3 3", to show in any field
     * describing train possessions, except the IPO.
     */
    private String makeListOfTrains() {
        if (this.isEmpty()) return "";

        StringBuilder b = new StringBuilder();

        // FIXME: trains has to be sorted by traintype
        for (Train train:this) {
            if (b.length() > 0) b.append(" ");
            if (train.isObsolete()) b.append("[");
            b.append(train.getId());
            if (train.isObsolete()) b.append("]");
        }

        return b.toString();
    }

    /**
     * Make an abbreviated list of trains, like "2(6) 3(5)" etc, to show in the
     * IPO.
     */
    public String makeListOfTrainCertificates() {
        if (this.isEmpty()) return "";

        // create a bag with train types
        Multiset<TrainCertificateType> trainCertTypes = HashMultiset.create();
        for (Train train:this) {
            trainCertTypes.add(train.getCertType()); 
        }
        
        StringBuilder b = new StringBuilder();
        
        // FIXME: add sorting
        for (TrainCertificateType certType:trainCertTypes) {
            if (b.length() > 0) b.append(" ");
            b.append(certType.getName()).append("(");
            if (certType.hasInfiniteQuantity()) {
                b.append("+");
            } else {
                b.append(trainCertTypes.count(certType));
            }
            b.append(")");
        }

        return b.toString();
    }
    
    public String getData() {
        if (!abbrList) {
            return makeListOfTrains();
        } else {
            return makeListOfTrainCertificates();
        }
    }
}
