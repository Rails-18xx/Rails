package net.sf.rails.game.specific._1817.action;

import net.sf.rails.game.RailsRoot;
import rails.game.action.SpecialORAction;
import rails.game.action.PossibleORAction;



    public class TakeLoans_1817 extends PossibleORAction implements SpecialORAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;

    public TakeLoans_1817(RailsRoot root, String companyId) {
        super(root);
        this.companyId = companyId;
    }

    public int getLoansToTake() { 
        return 1;
    }

    public String getCompanyId() { return companyId; }

    @Override
    public String getButtonLabel() {
        return "Take 1 Loan (" + companyId + ")";
    }
}