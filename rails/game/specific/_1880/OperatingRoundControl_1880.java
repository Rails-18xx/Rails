package rails.game.specific._1880;

import rails.game.GameDef.OrStep;
import rails.game.PublicCompanyI;

public class OperatingRoundControl_1880 {
    
    private PublicCompanyI lastCompanyToBuyTrain;
    private PublicCompanyI firstCompanyToRun;
    private OrStep nextStep;
    private boolean exitingToStockRound;
    private boolean startedFromStockRound;
    
    public OperatingRoundControl_1880() {
        lastCompanyToBuyTrain = null;
        firstCompanyToRun = null;      
        nextStep = OrStep.INITIAL;
        exitingToStockRound = false;
        startedFromStockRound = false;
    }

    public void orExitToStockRound(PublicCompanyI company, OrStep step) {
        firstCompanyToRun = company;
        nextStep = step;
        exitingToStockRound = true;
    }
    
    public PublicCompanyI lastCompanyToBuyTrain() {
        return lastCompanyToBuyTrain;
    }

    public void trainPurchased(PublicCompanyI company) {
        lastCompanyToBuyTrain = company;
    }
    
    public boolean isExitingToStockRound() {
        return exitingToStockRound;
    }
    
    public void startingStockRound() {
        exitingToStockRound = false;
    }
    
    public PublicCompanyI getFirstCompanyToRun() {
        return firstCompanyToRun;
    }
    
    public OrStep getNextStep() {
        return nextStep;
    }

    public void startNewOR() {
        exitingToStockRound = false;
        nextStep = OrStep.INITIAL;        
    }

    public boolean wasStartedFromStockRound() {
        return startedFromStockRound;
    }

    public void startedFromStockRound() {
        startedFromStockRound = true;        
    }

    public void startedFromOperatingRound() {
        startedFromStockRound = false;        
    }

}
