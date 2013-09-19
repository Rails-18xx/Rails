package rails.game.specific._1880;

import rails.game.GameDef.OrStep;
import rails.game.PublicCompanyI;

public class OperatingRoundControl_1880 {
    
    private PublicCompanyI lastCompanyToBuyTrain;
    private PublicCompanyI firstCompanyToRun;
    private OrStep nextStep;    
    
    public OperatingRoundControl_1880() {
        reset();
    }
    
    public PublicCompanyI getLastCompanyToBuyTrain() {
        return lastCompanyToBuyTrain;
    }
    
    public void trainPurchased(PublicCompanyI company) {
        lastCompanyToBuyTrain = company;
    }
    
    public void orEnded(PublicCompanyI company) {
        firstCompanyToRun = company;
        nextStep = OrStep.BUY_TRAIN;
        lastCompanyToBuyTrain = null;
    }
        
    public boolean startingAtTopOfOrder() {
        if (firstCompanyToRun == null) {
            return true;
        }
        return false;
    }
    
    public PublicCompanyI getFirstCompanyToRun() {
        return firstCompanyToRun;
    }
    
    public void reset() {
        firstCompanyToRun = null;
        nextStep = OrStep.INITIAL;
    }

    public OrStep getNextPhase() {
        return nextStep;
    }
    
    public boolean orEnded() {
        if (firstCompanyToRun == null) {
            return false;
        }
        return true;
    }
    
    
}
