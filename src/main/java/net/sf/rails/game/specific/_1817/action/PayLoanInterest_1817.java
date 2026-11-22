package net.sf.rails.game.specific._1817.action;

import net.sf.rails.game.RailsRoot;
import rails.game.action.PossibleORAction;

public class PayLoanInterest_1817 extends PossibleORAction {
    private static final long serialVersionUID = 1L;
    private final String companyName;
    private final int interestDue;

    public PayLoanInterest_1817(RailsRoot root, String companyName, int interestDue) {
        super(root);
        this.companyName = companyName;
        this.interestDue = interestDue;
    }

    public String getCompanyName() { return companyName; }
    public int getInterestDue() { return interestDue; }

    @Override
    public String getButtonLabel() {
        return "Pay Interest ($" + interestDue + ")";
    }
}