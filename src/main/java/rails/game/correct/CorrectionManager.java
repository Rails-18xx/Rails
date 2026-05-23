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
        if (action instanceof CorrectionModeAction)
            return execute((CorrectionModeAction) action);
        else {
            log.debug("This correction action is not registered.");
            return false;
        }
    }

    private boolean execute(CorrectionModeAction action) {

        GameManager gm = getParent(); 

        if (!isActive()) {
            // 1. Pause Timer & Set Correction Mode Flag
            gm.setGamePaused(true);
            gm.getCorrectionModeActiveModel().set(true);

            // [MODIFIED] User requested removal of alerts
            // String text = LocalText.getText("CorrectionModeActivate",
            //         getRoot().getPlayerManager().getCurrentPlayer().getId(),
            //         LocalText.getText(getCorrectionType().name())
            // );
            // ReportBuffer.add(this, text);
            // DisplayBuffer.add(this, text);

        } else {
            // [MODIFIED] User requested removal of alerts
            // String text = LocalText.getText("CorrectionModeDeactivate",
            //         getRoot().getPlayerManager().getCurrentPlayer().getId(),
            //         LocalText.getText(getCorrectionType().name())
            // );
            // ReportBuffer.add(this, text);

            // 2. Resume Timer & Clear Correction Mode Flag
            gm.getCorrectionModeActiveModel().set(false);
            gm.setGamePaused(false);
        }

        active.set(!active.value());

        // 3. CRITICAL: Force a full UI refresh to clear any stale graphics/data bindings.
        if (gm.getGameUIManager() != null) {
             gm.getGameUIManager().forceFullUIRefresh();
        }

        return true;
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