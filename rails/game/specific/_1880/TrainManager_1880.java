/**
 * 
 */
package rails.game.specific._1880;

import rails.common.LocalText;
import rails.game.Phase;
import rails.game.Portfolio;
import rails.game.ReportBuffer;
import rails.game.TrainCertificateType;
import rails.game.TrainI;
import rails.game.TrainManager;

/**
 * @author martin
 *
 */
public class TrainManager_1880 extends TrainManager {

    /**
     * 
     */
    public TrainManager_1880() {
        super();
    }

    /* (non-Javadoc)
     * @see rails.game.TrainManager#checkTrainAvailability(rails.game.TrainI, rails.game.Portfolio)
     */
    @Override
    public void checkTrainAvailability(TrainI train, Portfolio from) {
        trainsHaveRusted = false;
        phaseHasChanged = false;
        if (from != ipo) return;

        TrainCertificateType boughtType, nextType;
        boughtType = train.getCertType();
        if (boughtType == (trainCertTypes.get(newTypeIndex.intValue()))
            && ipo.getTrainOfType(boughtType) == null) {
            // Last train bought, make a new type available.
            newTypeIndex.add(1);
            if (newTypeIndex.intValue() < lTrainTypes.size()) {
                nextType = (trainCertTypes.get(newTypeIndex.intValue()));
                if (nextType != null) {
                    String nextTypeName = nextType.getName();
                    if (!nextTypeName.equals("2R")) {
                        if (!nextType.isAvailable()) {
                            makeTrainAvailable(nextType);
                            trainAvailabilityChanged = true;
                            ReportBuffer.add("All " + boughtType.getName()
                                    + "-trains are sold out, "
                                    + nextType.getName() + "-trains now available");
                        }
                    }
                    else {
                        newTypeIndex.add(1);
                        if (newTypeIndex.intValue() < lTrainTypes.size()) {
                            nextType = (trainCertTypes.get(newTypeIndex.intValue()));
                            if (nextType != null) {
                                if (!nextType.isAvailable()) {
                                    makeTrainAvailable(nextType);
                                    trainAvailabilityChanged = true;
                                    ReportBuffer.add("All " + boughtType.getName()
                                            + "-trains are sold out, "
                                            + nextType.getName() + "-trains now available");
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
            ReportBuffer.add(LocalText.getText("FirstTrainBought",
                    boughtType.getName()));
        } 
        
        // New style phase changes, can be triggered by any bought train.
        Phase newPhase;
        if (newPhases.get(boughtType) != null
                && (newPhase = newPhases.get(boughtType).get(trainIndex)) != null) {
            gameManager.getPhaseManager().setPhase(newPhase, train.getHolder());
            phaseHasChanged = true;
        }
    }

}
