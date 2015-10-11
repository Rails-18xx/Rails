
package net.sf.rails.game.specific._1844;

import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainCertificateType;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;

public class TrainManager_1844 extends TrainManager {

    
    /*
     * In 1844 the plain trains are converted to a Hex based Train if new Phases are reached first.
     * The H-Trains then rust as usual.
     */
    
    private boolean trainsHaveConverted = false;
    protected final IntegerState convertedTypeIndex = IntegerState.create(this, "convertedTypeIndex", 0);
    
    public TrainManager_1844(RailsRoot parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

/*    
     * (non-Javadoc)
     * @see net.sf.rails.game.TrainManager#rustTrainType(net.sf.rails.game.TrainCertificateType, net.sf.rails.game.state.Owner)
     * In 1844 the plain trains dont rust but are converted to H-Trains. The H-Trains rust.
     * After discussion with Erik Voss:
     *      - traincertificates can handle more than one traintype, so on the certificate the type must be changed 
     *        from the old to the new type
     *      - old train objects must be discarded
     *      - new train objects must be create
     
    protected void rustTrainType (TrainCertificateType type, Owner lastBuyingCompany) {
        List<Train> convertedTrains;
        TrainType oldTrainType;
        TrainCertificateType newTrainType;
        
        oldTrainType=type.;
        for (int i=0 ; i < lTrainTypes.size(); i++) {
            if (lTrainTypes.get(i).getName().equals(oldTrainType.getName())) {
                newTrainType=lTrainTypes.get(i+1);
                break;
            }
        for (Train train : trainsPerCertType.get(type)) {
            Owner owner = train.getOwner();
            // check condition for train rusting
            if (type.getId().endsWith("H")) {
                log.debug("Train " + train.getId() + " (owned by "
                        + owner.getId() + ") rusted");
                train.setRusted();
            }
            else // a Plain Train has to be converted to an H-Train...
            {
              
             //Find the old Train Type in the List of Train Types, the new one is the next in line at least in 1844

             }
                     
            }
            trainsPerCertType.put(newTrainType, convertedTrains);
        }
        // report about event
        if (type.hasRusted()) {
            ReportBuffer.add(this, LocalText.getText("TrainsRusted",type.getId()));
        } else {
            ReportBuffer.add(this, LocalText.getText("TrainsConverted",type.getId()));
        }
    }*/
}
