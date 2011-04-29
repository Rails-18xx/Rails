package rails.game.specific._18TN;

import java.util.*;

import rails.game.*;
import rails.game.action.BuyPrivate;
import rails.game.state.ArrayListState;

public class OperatingRound_18TN extends OperatingRound {
    
    private ArrayListState<Player> playersSoldInOR1;
    
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
    
    protected boolean maySellPrivate (Player player) {
        return gameManager.getAbsoluteORNumber() != 1 
               || !hasPlayerSoldInOR1(player);
    }
    
    private boolean hasPlayerSoldInOR1 (Player player) {
        return playersSoldInOR1 != null && playersSoldInOR1.contains(player);
    }

    public boolean buyPrivate(BuyPrivate action) {
        
        Player sellingPlayer = null;
        
        if (gameManager.getAbsoluteORNumber() == 1) {
            sellingPlayer = (Player)((Portfolio)action.getPrivateCompany().getHolder()).getOwner();
        }
        
        boolean result = super.buyPrivate(action);
        
        if (result && gameManager.getAbsoluteORNumber() == 1) {
            if (playersSoldInOR1 == null) playersSoldInOR1 = new ArrayListState<Player>("PlayersSoldPrivateInOR1");
            if (!playersSoldInOR1.contains(sellingPlayer)) {
                playersSoldInOR1.add(sellingPlayer);
            }
        }
        
        return result;
    }
}
