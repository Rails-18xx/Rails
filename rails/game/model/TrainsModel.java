/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/TrainsModel.java,v 1.8 2010/01/31 22:22:29 macfreek Exp $*/
package rails.game.model;

import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;

import rails.game.Portfolio;
import rails.game.TrainCertificateType;
import rails.game.TrainI;

public class TrainsModel extends HolderModel<TrainI> {

    public boolean abbrList = false;
    
    public TrainsModel(Portfolio portfolio) {
        super(portfolio, "TrainsModel");
    }

    public void setAbbrList(boolean abbrList) {
        this.abbrList = abbrList;
    }
    
    public ImmutableList<TrainI> getTrains() {
        return this.viewList();
    }
    
    public TrainI getTrainOfType(TrainCertificateType type) {
        for (TrainI train:this.viewList()) {
            if (train.getCertType() == type) return train;
        }
        return null;
    }
    
    /**
     * Make a full list of trains, like "2 2 3 3", to show in any field
     * describing train possessions, except the IPO.
     */
    private String makeListOfTrains() {
        List<TrainI> trains = this.viewList();
        if (trains == null || trains.isEmpty()) return "";

        StringBuilder b = new StringBuilder();

        // FIXME: trains has to be sorted by traintype
        for (TrainI train :trains) {
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
        List<TrainI> trains = this.viewList();
        if (trains == null || trains.isEmpty()) return "";

        // create a bag with train types
        Multiset<TrainCertificateType> trainCertTypes = HashMultiset.create();
        for (TrainI train:trains) {
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
