package net.sf.rails.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.Round;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.GenericState;


/**
 * This class contains hints from the server (game engine) to the client (GUI)
 * about the preferred visibility of the various window types.
 * It is up to the GUI (and its user) to decide what to do with these hints,
 * but the current implementation should exactly follow these hints.
 * @author VosE
 *
 */
public final class GuiHints extends RailsAbstractItem implements Serializable{

    public static final long serialVersionUID = 1L;

    /** What round type is currently active in the engine? */
    private GenericState<Class<? extends RoundFacade>> currentRoundType = GenericState.create(this, "currentRoundType");

    /** Which windows should be visible? */
    private List<VisibilityHint> visibilityHints;

    /** Which window type is active and should be on top? */
    private GenericState<GuiDef.Panel> activePanel = GenericState.create(this, "activePanel");

    private GuiHints(RailsItem parent, String id) {
        super(parent, id);
    }
    
    public static GuiHints create(RailsItem parent, String id){
        return new GuiHints(parent, id);
    }
    
    public Class<? extends RoundFacade> getCurrentRoundType() {
        return currentRoundType.value();
    }

    public void setCurrentRoundType(Class<? extends RoundFacade> currentRoundType) {
        this.currentRoundType.set(currentRoundType);
    }

    public List<VisibilityHint> getVisibilityHints() {
        return visibilityHints;
    }

    public void setVisibilityHint(GuiDef.Panel type, boolean visibility) {
        if (visibilityHints == null) {
            visibilityHints = new ArrayList<VisibilityHint>(4);
        }
        visibilityHints.add (new VisibilityHint(type, visibility));
    }

    public void clearVisibilityHints () {
        if (visibilityHints == null) {
            visibilityHints = new ArrayList<VisibilityHint>(4);
        } else {
            visibilityHints.clear();
        }
    }

    public GuiDef.Panel getActivePanel() {
        return (GuiDef.Panel)activePanel.value();
    }

    public void setActivePanel(GuiDef.Panel activePanel) {
        this.activePanel.set(activePanel);
    }

    public class VisibilityHint {

        GuiDef.Panel type;
        boolean visibility;

        VisibilityHint (GuiDef.Panel type, boolean visibility) {
            this.type = type;
            this.visibility = visibility;
        }

        public GuiDef.Panel getType() {
            return type;
        }

        public boolean getVisibility() {
            return visibility;
        }
    }

}
