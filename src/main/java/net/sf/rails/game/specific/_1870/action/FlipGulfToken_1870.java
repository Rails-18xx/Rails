package net.sf.rails.game.specific._1870.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class FlipGulfToken_1870 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;

    public FlipGulfToken_1870(RailsRoot root, String companyId) {
        super(root);
        this.companyId = companyId;
    }

    public String getCompanyId() { return companyId; }

@Override
    public String toString() {
        return "Flip port";
    }

    @Override
    public String getButtonLabel() {
        return toString();
    }
}