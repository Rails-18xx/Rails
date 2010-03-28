package rails.game.specific._1835;

import rails.game.GameDef;
import rails.game.GameManagerI;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.action.DiscardTrain;
import rails.game.state.BooleanState;

public class OperatingRound_1835 extends OperatingRound {
    
    private BooleanState needPrussianFormationCall 
            = new BooleanState ("NeedPrussianFormationCall", false);

    public OperatingRound_1835 (GameManagerI gameManager) {
        super (gameManager);
    }

    protected void newPhaseChecks() {
        PhaseI phase = getCurrentPhase();
        if (phase.getName().equals("4") || phase.getName().equals("4+4")
                || phase.getName().equals("5")) {
            if (!PrussianFormationRound.prussianIsComplete(gameManager)) {
                if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                    // Postpone until trains are discarded
                    needPrussianFormationCall.set(true);
                } else {
                    // Do it immediately
                    ((GameManager_1835)gameManager).startPrussianFormationRound (this);
                }
            }
        }
    }
    
    public boolean discardTrain(DiscardTrain action) {
        
        boolean result = super.discardTrain(action);
        if (result && getStep() == GameDef.OrStep.BUY_TRAIN 
                && needPrussianFormationCall.booleanValue()) {
            // Do the postponed formation calls 
            ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            needPrussianFormationCall.set(false);
        }
        return result;
    }



}
