package net.sf.rails.game.specific._1880;

import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameDef.OrStep;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;

public class OperatingRoundControl_1880 extends RailsAbstractItem {
    
    private final GenericState<PublicCompany> lastCompanyToBuyTrain = GenericState.create(this, "lastCompanyToBuyTrain");
    private final GenericState<PublicCompany> firstCompanyToRun = GenericState.create(this, "firstCompanyToRun");
    private final GenericState<PublicCompany> lastCompanyToOperate = GenericState.create(this, "lastCompanyToOperate");
    private final GenericState<GameDef.OrStep> nextStep;
    private final BooleanState exitingToStockRound = BooleanState.create(this, "ExitedToStockRound", false);
    private final BooleanState startedFromStockRound = BooleanState.create (this,"StartingFromStockRound",false);
    private final BooleanState finalOperatingRoundSequence = BooleanState.create (this,"FinalOperatingRoundSequence",false);
    private final BooleanState noTrainsToDiscard = BooleanState.create(this,"NoMoreTrainsToDiscard", false);
    private final IntegerState finalOperatingRoundSequenceNumber = IntegerState.create(this,"FinalOperatingRoundNumber",0);

    
    public OperatingRoundControl_1880(RailsRoot parent, String string) {
        super(parent,string);
        nextStep = GenericState.create(this, "ORStep",
                        GameDef.OrStep.INITIAL);
    }

    public void orExitToStockRound(PublicCompany company, OrStep step) {
        firstCompanyToRun.set(company);
        nextStep.set(step);
        exitingToStockRound.set(true);
    }
    
    public PublicCompany lastCompanyToBuyTrain() {
        return lastCompanyToBuyTrain.value();
    }

    public void trainPurchased(PublicCompany company) {
        lastCompanyToBuyTrain.set(company);
    }
    
    public boolean isExitingToStockRound() {
        return exitingToStockRound.value();
    }
    
    public void startingStockRound() {
        exitingToStockRound.set(false);
    }
    
    public PublicCompany getFirstCompanyToRun() {
        return firstCompanyToRun.value();
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
        finalOperatingRoundSequenceNumber.set(1); 
        }
    }
    
    public PublicCompany lastCompanyToOperate() {
        return lastCompanyToOperate.value();
    }
    
    public void setLastCompanyToOperate(PublicCompany company) {
        lastCompanyToOperate.set(company);
    }

    public boolean noTrainsToDiscard() {
        return noTrainsToDiscard.value();
    }

    public void setNoTrainsToDiscard(boolean maybe) {
        noTrainsToDiscard.set(maybe);
    }

    public int getFinalOperatingRoundSequenceNumber() {
        return finalOperatingRoundSequenceNumber.value();
    }

    public void setFinalOperatingRoundSequenceNumber(
            IntegerState finalOperatingRoundSequenceNumber) {
        this.finalOperatingRoundSequenceNumber.set(finalOperatingRoundSequenceNumber.value());
    }
    
    public void addFinalOperatingRoundSequenceNumber(int value) {
        this.finalOperatingRoundSequenceNumber.add(value);
    }
}
