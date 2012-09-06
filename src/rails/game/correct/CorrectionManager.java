package rails.game.correct;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.GameManager;
import rails.game.RailsAbstractItem;
import rails.game.ReportBuffer;
import rails.game.state.BooleanState;
import rails.game.state.ChangeStack;

public abstract class CorrectionManager extends RailsAbstractItem {
    
    private final CorrectionType correctionType;
    private final BooleanState active = BooleanState.create(this, "active");
    
    protected static Logger log =
        LoggerFactory.getLogger(CorrectionManager.class);

    protected CorrectionManager(GameManager parent, CorrectionType ct) {
        super(parent, ct.name());
        correctionType = ct; 
    }
    
    @Override
    public GameManager getParent() {
        return (GameManager)super.getParent();
    }

    public CorrectionType getCorrectionType() {
        return correctionType;
    }

    public boolean isActive(){
        return active.value();
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
        
        ChangeStack.start(this, action);
        if (!isActive()) {
            String text = LocalText.getText("CorrectionModeActivate",
                    getParent().getCurrentPlayer().getId(),
                    LocalText.getText(getCorrectionType().name())
            );
            ReportBuffer.add(text);
            DisplayBuffer.add(text);
        }
        else {
            ReportBuffer.add(LocalText.getText("CorrectionModeDeactivate",
                    getParent().getCurrentPlayer().getId(),
                    LocalText.getText(getCorrectionType().name())
            ));
        }
        active.set(!active.value());
     
        return true;
    }

    /* dummy to capture the non-supported actions */
    protected boolean execute(CorrectionAction action) {
        log.debug("The chosen action is not implemented in the registered manager");
        return false;
    }
    

    public boolean equals(CorrectionManager cm) {
        return (this.getParent() == cm.getParent() 
                && this.correctionType == cm.correctionType);
    }
}
