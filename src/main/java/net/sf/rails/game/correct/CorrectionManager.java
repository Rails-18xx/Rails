package net.sf.rails.game.correct;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.state.BooleanState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        
        
        if (!isActive()) {
            String text = LocalText.getText("CorrectionModeActivate",
                    getRoot().getPlayerManager().getCurrentPlayer().getId(),
                    LocalText.getText(getCorrectionType().name())
            );
            ReportBuffer.add(this, text);
            DisplayBuffer.add(this, text);
        }
        else {
            ReportBuffer.add(this, LocalText.getText("CorrectionModeDeactivate",
                    getRoot().getPlayerManager().getCurrentPlayer().getId(),
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
