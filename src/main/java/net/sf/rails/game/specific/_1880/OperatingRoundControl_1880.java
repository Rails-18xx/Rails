package net.sf.rails.game.specific._1880;

import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameDef.OrStep;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.GenericState;

public class OperatingRoundControl_1880 extends RailsAbstractItem {
    
    private PublicCompany lastCompanyToBuyTrain;
    private PublicCompany firstCompanyToRun;
    private PublicCompany lastCompanyToOperate;
    private GenericState<GameDef.OrStep> nextStep;
    private BooleanState exitingToStockRound = BooleanState.create(this, "ExitedToStockRound", false);
    private BooleanState startedFromStockRound = BooleanState.create (this,"StartingFromStockRound",false);
    private BooleanState finalOperatingRoundSequence = BooleanState.create (this,"FinalOperatingRoundSequence",false);
    private BooleanState noTrainsToDiscard = BooleanState.create(this, "NoMoreTrainsToDiscard", false);

    
    public OperatingRoundControl_1880(RailsRoot parent, String string) {
        super(parent,string);
        lastCompanyToBuyTrain = null;
        firstCompanyToRun = null;   
        lastCompanyToOperate = null;
        
        if (nextStep == null) {
            nextStep =
                    GenericState.create(this, "ORStep",
                            GameDef.OrStep.INITIAL);
        }
        exitingToStockRound.set(false);
        startedFromStockRound.set(false);
    }

    public void orExitToStockRound(PublicCompany company, OrStep step) {
        firstCompanyToRun = company;
        nextStep.set(step);
        exitingToStockRound.set(true);
    }
    
    public PublicCompany lastCompanyToBuyTrain() {
        return lastCompanyToBuyTrain;
    }

    public void trainPurchased(PublicCompany company) {
        lastCompanyToBuyTrain = company;
    }
    
    public boolean isExitingToStockRound() {
        return exitingToStockRound.value();
    }
    
    public void startingStockRound() {
        exitingToStockRound.set(false);
    }
    
    public PublicCompany getFirstCompanyToRun() {
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
        return startedFromStockRound.value();
    }

    public void startedFromStockRound() {
        startedFromStockRound.set(true);        
    }

    public void startedFromOperatingRound() {
        startedFromStockRound.set(false);        
    }

    public boolean isFinalOperatingRoundSequence() {
        return finalOperatingRoundSequence.value();
    }
    
    public void setFinalOperatingRoundSequence(boolean maybe) {
        if (maybe == true) {
        finalOperatingRoundSequence.set(true);
        }
    }
    
    public PublicCompany lastCompanyToOperate() {
        return lastCompanyToOperate;
    }
    
    public void setLastCompanyToOperate(PublicCompany company) {
        lastCompanyToOperate = company;
    }

    public boolean noTrainsToDiscard() {
        return noTrainsToDiscard.value();
    }

    public void setNoTrainsToDiscard(boolean maybe) {
        noTrainsToDiscard.set(maybe);
    }
}
