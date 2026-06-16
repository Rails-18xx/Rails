package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;

public class Short1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private String companyId;

    public Short1817(net.sf.rails.game.RailsRoot root, String companyId) {
        super(root);
        this.companyId = companyId;
    }

    public String getCompanyId() { return companyId; }

    @Override
    public String toString() {
        return "Sell Short " + companyId;
    }
}