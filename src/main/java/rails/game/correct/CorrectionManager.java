package rails.game.correct;

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

    protected static final Logger log = LoggerFactory.getLogger(CorrectionManager.class);

    private final BooleanState active = new BooleanState(this, "active");

    private final CorrectionType correctionType;

    protected CorrectionManager(GameManager parent, CorrectionType ct) {
        super(parent, ct.name());

        this.correctionType = ct;
    }

    @Override
    public GameManager getParent() {
        return (GameManager) super.getParent();
    }

    public CorrectionType getCorrectionType() {
        return correctionType;
    }

    public boolean isActive() {
        return active.value();
    }

    public List<CorrectionAction> createCorrections() {

        List<CorrectionAction> actions = new ArrayList<CorrectionAction>();
        actions.add(new CorrectionModeAction(getRoot(), getCorrectionType(), isActive()));

        return actions;
    }

    /**
     * calls all executeAction
     */
   public boolean executeCorrection(CorrectionAction action) {
        if (action instanceof CorrectionModeAction) {
            return execute((CorrectionModeAction) action);
        } else {
            // Execute the concrete subclass correction payload (e.g., Cash or Map adjustment)
            boolean result = execute(action);
            
            // Explicitly clear the correction tracking flags immediately upon successful 
            // completion so the state machine drops out of the choice block instantly
            if (result) {
                active.set(false);
                getParent().getCorrectionModeActiveModel().set(false);
            }
            return result;
        }
    }




private boolean execute(CorrectionModeAction action) {
        GameManager gm = getParent(); 

        // Open the targeting UI. During live play, it forces open. During reloads, it follows the log.
        boolean targetState = gm.isReloading() ? action.isActive() : true;
        
        active.set(targetState);
        gm.getCorrectionModeActiveModel().set(targetState);

        // Force a full UI refresh to clear and redraw components immediately
        if (gm.getGameUIManager() != null) {
             gm.getGameUIManager().forceFullUIRefresh();
        }

        return true;
    }

    /**
     * Called by GameManager to auto-close this correction state after a payload succeeds.
     */
    public void deactivate() {
        active.set(false);
    }

    /* dummy to capture the non-supported actions */
    protected boolean execute(CorrectionAction action) {
        log.debug("The chosen action is not implemented in the registered manager");

        return false;
    }


    public boolean equals(Object object) {
        if ( ! (object instanceof CorrectionManager) ) {
            return false;
        }
        CorrectionManager cm = (CorrectionManager) object;
        return this.getParent() == cm.getParent() && this.correctionType == cm.correctionType;
    }

    
}