package rails.game.specific._1880;

import rails.game.GameDef.OrStep;
import rails.game.state.BooleanState;
import rails.game.state.EnumState;
import rails.game.GameDef;
import rails.game.PublicCompanyI;

public class OperatingRoundControl_1880 {
    
    private PublicCompanyI lastCompanyToBuyTrain;
    private PublicCompanyI firstCompanyToRun;
    private EnumState<GameDef.OrStep> nextStep;
    private BooleanState exitingToStockRound = new BooleanState ("ExitedToStockRound",false);
    private BooleanState startedFromStockRound = new BooleanState ("StartingFromStockRound",false);

    
    public OperatingRoundControl_1880() {
        lastCompanyToBuyTrain = null;
        firstCompanyToRun = null;   
        
        if (nextStep == null) {
            nextStep =
                    new EnumState<GameDef.OrStep>("ORStep",
                            GameDef.OrStep.INITIAL);
        }
        exitingToStockRound.set(false);
        startedFromStockRound.set(false);
    }

    public void orExitToStockRound(PublicCompanyI company, OrStep step) {
        firstCompanyToRun = company;
        nextStep.set(step);
        exitingToStockRound.set(true);
    }
    
    public PublicCompanyI lastCompanyToBuyTrain() {
        return lastCompanyToBuyTrain;
    }

    public void trainPurchased(PublicCompanyI company) {
        lastCompanyToBuyTrain = company;
    }
    
    public boolean isExitingToStockRound() {
        return exitingToStockRound.booleanValue();
    }
    
    public void startingStockRound() {
        exitingToStockRound.set(false);
    }
    
    public PublicCompanyI getFirstCompanyToRun() {
        return firstCompanyToRun;
    }
    
    public OrStep getNextStep() {
        return nextStep.value();
    }

    public void startNewOR() {
        exitingToStockRound.set(false);
        nextStep.set(OrStep.INITIAL);        
    }

    public boolean wasStartedFromStockRound() {
        return startedFromStockRound.booleanValue();
    }

    public void startedFromStockRound() {
        startedFromStockRound.set(true);        
    }

    public void startedFromOperatingRound() {
        startedFromStockRound.set(false);        
    }

}
