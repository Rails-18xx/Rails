package rails.game.specific._18TN;

import java.util.List;

import rails.game.*;

public class OperatingRound_18TN extends OperatingRound {
    
    public OperatingRound_18TN (GameManagerI gameManager) {
        super (gameManager);
    }

    protected boolean isPrivateSellingAllowed() {
        return getCurrentPhase().isPrivateSellingAllowed() 
                // 18TN special
            || !operatingCompany.get().hasOperated() && !ownsPrivate(operatingCompany.get());
    }
    
    protected int getPrivateMinimumPrice (PrivateCompanyI privComp) {
        int minPrice = privComp.getLowerPrice();
        if (minPrice == PrivateCompanyI.NO_PRICE_LIMIT) {
            minPrice = 0;
        } else if (!operatingCompany.get().hasOperated()) {
            // 18TN special
            minPrice = privComp.getBasePrice();
        }
        return minPrice;
    }
    
    protected int getPrivateMaximumPrice (PrivateCompanyI privComp) {
        int maxPrice = privComp.getUpperPrice();
        if (maxPrice == PrivateCompanyI.NO_PRICE_LIMIT) {
            maxPrice = operatingCompany.get().getCash();
        } else if (!operatingCompany.get().hasOperated()) {
            // 18TN special
            maxPrice = privComp.getBasePrice();
        }
        return maxPrice;
    }
    
    private boolean ownsPrivate (PublicCompanyI company) {
        List<PrivateCompanyI> privates = company.getPortfolio().getPrivateCompanies();
        return privates != null && !privates.isEmpty();
    }

}
