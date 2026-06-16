package net.sf.rails.game.specific._1817.action;

import net.sf.rails.game.RailsRoot;
import rails.game.action.PossibleORAction;

public class RepayLoans_1817 extends PossibleORAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;
    private final int maxRepayable;
    private int loansToRepay;

    public RepayLoans_1817(RailsRoot root, String companyId, int maxRepayable) {
        super(root);
        this.companyId = companyId;
        this.maxRepayable = maxRepayable;
    }

    public String getCompanyId() { return companyId; }
    public int getMaxRepayable() { return maxRepayable; }
    public int getLoansToRepay() { return loansToRepay; }
    public void setLoansToRepay(int count) { this.loansToRepay = count; }

    @Override
    public String getButtonLabel() {
        return "Repay Loans";
    }
}