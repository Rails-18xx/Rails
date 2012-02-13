package rails.game.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;

import rails.game.TrainCertificateType;
import rails.game.Train;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioMap;

public class TrainsModel extends Model {

    public static final String id = "TrainsModel";
    
    private final PortfolioMap<Train> trains = PortfolioMap.create();
    
    private boolean abbrList = false;

    private TrainsModel() {}
    
    public static TrainsModel create(Item parent) {
        return new TrainsModel().init(parent, id);
    }
    
    @Override
    public TrainsModel init(Item parent, String id){
        super.init(parent, id);
        trains.init(this, "Trains");
        return this;
    }
    
    public Portfolio<Train> getPortfolio() {
        return trains;
    }
    
    public void setAbbrList(boolean abbrList) {
        this.abbrList = abbrList;
    }
    
    public ImmutableList<Train> getTrains() {
        return trains.items();
    }
    
    public Train getTrainOfType(TrainCertificateType type) {
        for (Train train:trains) {
            if (train.getCertType() == type) return train;
        }
        return null;
    }
    
    /**
     * Make a full list of trains, like "2 2 3 3", to show in any field
     * describing train possessions, except the IPO.
     */
    private String makeListOfTrains() {
        if (trains.isEmpty()) return "";

        StringBuilder b = new StringBuilder();

        // FIXME: trains has to be sorted by traintype
        for (Train train:trains) {
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
        if (trains.isEmpty()) return "";

        // create a bag with train types
        Multiset<TrainCertificateType> trainCertTypes = HashMultiset.create();
        for (Train train:trains) {
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
