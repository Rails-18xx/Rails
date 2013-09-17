package rails.game.specific._1880;

import rails.game.Company;
import rails.game.GameDef.OrStep;
import rails.game.PublicCompanyI;

public class OperatingRoundControl_1880 {
    
    private PublicCompanyI lastCompanyToBuyTrain;
    private PublicCompanyI firstCompanyToRun;
    private OrStep nextStep;
    private boolean skipFirstCompany;
    
    
    public OperatingRoundControl_1880() {
        reset();
    }
    
    public PublicCompanyI getLastCompanyToBuyTrain() {
        return lastCompanyToBuyTrain;
    }
    
    public void trainPurchased(PublicCompanyI company) {
        lastCompanyToBuyTrain = company;
    }
    
    public void orEndedNoTrainPurchased(PublicCompanyI company) {
        firstCompanyToRun = company;
        skipFirstCompany = true;
        nextStep = OrStep.INITIAL;
        lastCompanyToBuyTrain = null;
    }
    
    public void orEndedLastTrainPurchased(PublicCompanyI company) {
        firstCompanyToRun = company;
        skipFirstCompany = false;
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
    
    public boolean getSkipFirstCompany() {
        return skipFirstCompany;
    }
    
    public void reset() {
        firstCompanyToRun = null;
        nextStep = OrStep.INITIAL;
        skipFirstCompany = false;
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
