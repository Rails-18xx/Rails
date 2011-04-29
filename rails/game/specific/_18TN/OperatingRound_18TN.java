package rails.game.specific._18TN;

import java.util.List;

import rails.game.*;

public class OperatingRound_18TN extends OperatingRound {
    
    public OperatingRound_18TN (GameManagerI gameManager) {
        super (gameManager);
    }

    protected boolean isPrivateSellingAllowed() {
        return super.isPrivateSellingAllowed() 
                // 18TN special
            || gameManager.getAbsoluteORNumber() == 1 
                && !ownsPrivate(operatingCompany.get());
    }
    
    protected int getPrivateMinimumPrice (PrivateCompanyI privComp) {
        if (gameManager.getAbsoluteORNumber() == 1
                && !getCurrentPhase().isPrivateSellingAllowed()) {
            // 18TN special
            return privComp.getBasePrice();
        } else {
            return super.getPrivateMinimumPrice(privComp);
        }
    }
    
    protected int getPrivateMaximumPrice (PrivateCompanyI privComp) {
        if (gameManager.getAbsoluteORNumber() == 1
                && !getCurrentPhase().isPrivateSellingAllowed()) {
            // 18TN special
            return privComp.getBasePrice();
        } else {
            return super.getPrivateMaximumPrice(privComp);
        }
    }
    
    private boolean ownsPrivate (PublicCompanyI company) {
        List<PrivateCompanyI> privates = company.getPortfolio().getPrivateCompanies();
        return privates != null && !privates.isEmpty();
    }

}
