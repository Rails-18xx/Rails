package net.sf.rails.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
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
 *
 * @author VosE
 */
public final class GuiHints extends RailsAbstractItem implements Serializable {

    public static final long serialVersionUID = 1L;

    /**
     * What round type is currently active in the engine?
     */
    private GenericState<Class<? extends RoundFacade>> currentRoundType = GenericState.create(this, "currentRoundType");

    /**
     * Which windows should be visible?
     */
    @Getter
    private List<VisibilityHint> visibilityHints;

    /**
     * Which window type is active and should be on top?
     */
    private GenericState<GuiDef.Panel> activePanel = GenericState.create(this, "activePanel");

    public GuiHints(RailsItem parent, String id) {
        super(parent, id);
    }

    public Class<? extends RoundFacade> getCurrentRoundType() {
        return this.currentRoundType.value();
    }

    public void setCurrentRoundType(Class<? extends RoundFacade> currentRoundType) {
        this.currentRoundType.set(currentRoundType);
    }

    public void setVisibilityHint(GuiDef.Panel type, boolean visibility) {
        if (visibilityHints == null) {
            visibilityHints = new ArrayList<VisibilityHint>(4);
        }
        visibilityHints.add(new VisibilityHint(type, visibility));
    }

    public void clearVisibilityHints() {
        if (visibilityHints == null) {
            visibilityHints = new ArrayList<VisibilityHint>(4);
        } else {
            visibilityHints.clear();
        }
    }

    public GuiDef.Panel getActivePanel() {
        return this.activePanel.value();
    }

    public void setActivePanel(GuiDef.Panel activePanel) {
        this.activePanel.set(activePanel);
    }

    @Data
    public static class VisibilityHint {
        private final GuiDef.Panel type;
        private final boolean visible;
    }

}
