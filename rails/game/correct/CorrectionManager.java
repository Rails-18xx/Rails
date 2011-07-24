package rails.game.correct;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.GameManager;
import rails.game.ReportBuffer;
import rails.game.state.AbstractItem;
import rails.game.state.BooleanState;

public abstract class CorrectionManager extends AbstractItem implements CorrectionManagerI {
    
    protected GameManager gameManager;
    
    private CorrectionType correctionType;

    private BooleanState active;
    
    protected static Logger log =
        Logger.getLogger(CorrectionManager.class.getPackage().getName());


    protected CorrectionManager(GameManager gm, CorrectionType ct) {
        gameManager = gm;
        correctionType = ct; 
        active = new BooleanState(this, ct.name(),false); 
    }
    
    public CorrectionType getCorrectionType() {
        return correctionType;
    }

    public boolean isActive(){
        return active.booleanValue();
    }
    
    public List<CorrectionAction> createCorrections() {

        List<CorrectionAction> actions = new ArrayList<CorrectionAction>();
        actions.add(new CorrectionModeAction(getCorrectionType(), isActive()));

        return actions;
    }
   
    /** calls all executeAction */
    public boolean executeCorrection(CorrectionAction action){
        if (action instanceof CorrectionModeAction)  
            return execute((CorrectionModeAction) action);
        else {
            log.debug("This correction action is not registered.");
            return false;
        }
    }
    
    private boolean execute(CorrectionModeAction action) {
        
        gameManager.getChangeStack().start(false);
        if (!isActive()) {
            String text = LocalText.getText("CorrectionModeActivate",
                    gameManager.getCurrentPlayer().getId(),
                    LocalText.getText(getCorrectionType().name())
            );
            ReportBuffer.add(text);
            DisplayBuffer.add(text);
        }
        else {
            ReportBuffer.add(LocalText.getText("CorrectionModeDeactivate",
                    gameManager.getCurrentPlayer().getId(),
                    LocalText.getText(getCorrectionType().name())
            ));
        }
        active.set(!active.booleanValue());
     
        return true;
    }

    /* dummy to capture the non-supported actions */
    protected boolean execute(CorrectionAction action) {
        log.debug("The chosen action is not implemented in the registered manager");
        return false;
    }
    

    public boolean equals(CorrectionManager cm) {
        return (this.gameManager == cm.gameManager 
                && this.correctionType == cm.correctionType);
    }
}
