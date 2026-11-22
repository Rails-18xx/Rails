package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class DeclineCoalToken_1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;

    public DeclineCoalToken_1817(RailsRoot root) {
        super(root);
    }

    public String getText() {
        return "Decline Coal Mine";
    }

    @Override
    public String toString() {
        return getText();
    }
}