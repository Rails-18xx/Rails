package net.sf.rails.game.specific._1870.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.PublicCompany;

public class RedeemShare_1870 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;

    public RedeemShare_1870(PublicCompany company) {
        super(company.getRoot());
        this.companyId = company.getId();
        setButtonLabel("Redeem " + companyId);
    }

    public String getCompanyId() {
        return companyId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RedeemShare_1870 other = (RedeemShare_1870) obj;
        return companyId.equals(other.companyId);
    }

    @Override
    public int hashCode() {
        return companyId.hashCode();
    }
}