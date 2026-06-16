package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;

public class SelectPurchasingCompany_1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;

    public SelectPurchasingCompany_1817(net.sf.rails.game.RailsRoot root, String companyId) {
        super(root);
        this.companyId = companyId;
    }

    public String getCompanyId() { return companyId; }

@Override
    public String toString() { 
        return "BANK".equals(companyId) ? "Bank liquidates company" : "Select " + companyId + " to purchase"; 
    }
}

