package rails.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rails.game.RoundI;
import rails.game.move.StateChange;
import rails.game.state.EnumState;
import rails.game.state.GenericState;

/**
 * This class contains hints from the server (game engine) to the client (GUI)
 * about the preferred visibility of the various window types.
 * It is up to the GUI (and its user) to decide what to do with these hints,
 * but the current implementation should exactly follow these hints.
 * @author VosE
 *
 */
public class GuiHints implements Serializable {

    public static final long serialVersionUID = 1L;

    /** What round type is currently active in the engine? */
    private GenericState<Class<? extends RoundI>> currentRoundType = null;

    /** Which windows should be visible? */
    private List<VisibilityHint> visibilityHints;

    /** Which window type is active and should be on top? */
    private EnumState<GuiDef.Panel> activePanel = null;

    public Class<? extends RoundI> getCurrentRoundType() {
        return currentRoundType.get();
    }

    public void setCurrentRoundType(Class<? extends RoundI> currentRoundType) {
        if (this.currentRoundType == null)
            this.currentRoundType = new GenericState<Class<? extends RoundI>>
                        ("CurrentRoundType",  currentRoundType);
        else
            new StateChange(this.currentRoundType, currentRoundType);
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
        return (GuiDef.Panel)activePanel.get();
    }

    public void setActivePanel(GuiDef.Panel activePanel) {
        if (this.activePanel == null)
            this.activePanel = new EnumState<GuiDef.Panel>("ActivePanel",  activePanel);
        else
            new StateChange(this.activePanel, activePanel);
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
