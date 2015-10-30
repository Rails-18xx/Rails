package net.sf.rails.game.specific._1880;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Phase;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainCertificateType;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.Owner;


/**
 * @author martin
 *
 */
public class TrainManager_1880 extends TrainManager {

     public TrainManager_1880(RailsRoot parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }


    /* (non-Javadoc)
     * @see rails.game.TrainManager#checkTrainAvailability(rails.game.TrainI, rails.game.Portfolio)
     */
    @Override
    public void checkTrainAvailability(Train train, Owner from) {
        phaseHasChanged.set(false);;
        if (from != Bank.getIpo(this)) return;

        TrainCertificateType boughtType, nextType;
        boughtType = train.getCertType();
        if (boughtType == (trainCertTypes.get(newTypeIndex.value()))
            && Bank.getIpo(this).getPortfolioModel().getTrainOfType(boughtType) == null) {
            // Last train bought, make a new type available.
            newTypeIndex.add(1);
            if (newTypeIndex.value() < lTrainTypes.size()) {
                nextType = (trainCertTypes.get(newTypeIndex.value()));
                if (nextType != null) {
                    String nextTypeName = nextType.getId();
                    if (!nextTypeName.equals("2R")) {
                        if (!nextType.isAvailable()) {
                            makeTrainAvailable(nextType);
                            trainAvailabilityChanged.set(true);
                            ReportBuffer.add(this, "All " + boughtType.getId()
                                    + "-trains are sold out, "
                                    + nextType.getId() + "-trains now available");
                        }
                    }
                    else {
                        newTypeIndex.add(1);
                        if (newTypeIndex.value() < lTrainTypes.size()) {
                            nextType = (trainCertTypes.get(newTypeIndex.value()));
                            if (nextType != null) {
                                if (!nextType.isAvailable()) {
                                    makeTrainAvailable(nextType);
                                    trainAvailabilityChanged.set(true);
                                    ReportBuffer.add(this, "All " + boughtType.getId()
                                            + "-trains are sold out, "
                                            + nextType.getId() + "-trains now available");
                                }
                            }
                        }
                    }                        
                }
            }
        }
        
        int trainIndex = boughtType.getNumberBoughtFromIPO();
        if (trainIndex == 0) {
            trainIndex = 1; // Workaround - if all the trains were trashed, we still need to change phase.
        }
        if (trainIndex == 1) {
            // First train of a new type bought
            ReportBuffer.add(this, LocalText.getText("FirstTrainBought",
                    boughtType.getId()));
        } 
        
        // New style phase changes, can be triggered by any bought train.
        Phase newPhase;
        if (newPhases.get(boughtType) != null
                && (newPhase = newPhases.get(boughtType).get(trainIndex)) != null) {
            getRoot().getPhaseManager().setPhase(newPhase, from);
            phaseHasChanged.set(true);
        }
    }

}