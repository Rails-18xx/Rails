package rails.game.specific._18TN;

import java.util.List;

import rails.game.*;
import rails.game.action.BuyPrivate;
import rails.game.action.SetDividend;
import rails.game.state.ArrayListState;

public class OperatingRound_18TN extends OperatingRound {

    private ArrayListState<Player> playersSoldInOR1;

    public OperatingRound_18TN (GameManager gameManager) {
        super (gameManager);
    }

    @Override
    protected boolean isPrivateSellingAllowed() {
        return super.isPrivateSellingAllowed()
        // 18TN special
        || gameManager.getAbsoluteORNumber() == 1
        && !ownsPrivate(operatingCompany.get());
    }

    @Override
    protected int getPrivateMinimumPrice (PrivateCompany privComp) {
        if (gameManager.getAbsoluteORNumber() == 1
                && !getCurrentPhase().isPrivateSellingAllowed()) {
            // 18TN special
            return privComp.getBasePrice();
        } else {
            return super.getPrivateMinimumPrice(privComp);
        }
    }

    @Override
    protected int getPrivateMaximumPrice (PrivateCompany privComp) {
        if (gameManager.getAbsoluteORNumber() == 1
                && !getCurrentPhase().isPrivateSellingAllowed()) {
            // 18TN special
            return privComp.getBasePrice();
        } else {
            return super.getPrivateMaximumPrice(privComp);
        }
    }

    private boolean ownsPrivate (PublicCompany company) {
        List<PrivateCompany> privates = company.getPortfolio().getPrivateCompanies();
        return privates != null && !privates.isEmpty();
    }

    @Override
    protected boolean maySellPrivate (Player player) {
        return gameManager.getAbsoluteORNumber() != 1
        || !hasPlayerSoldInOR1(player);
    }

    private boolean hasPlayerSoldInOR1 (Player player) {
        return playersSoldInOR1 != null && playersSoldInOR1.contains(player);
    }

    @Override
    public boolean buyPrivate(BuyPrivate action) {

        Player sellingPlayer = null;

        if (gameManager.getAbsoluteORNumber() == 1) {
            sellingPlayer = (Player)action.getPrivateCompany().getPortfolio().getOwner();
        }

        boolean result = super.buyPrivate(action);

        if (result && gameManager.getAbsoluteORNumber() == 1) {
            if (playersSoldInOR1 == null) playersSoldInOR1 = ArrayListState.create(this, "PlayersSoldPrivateInOR1");
            if (!playersSoldInOR1.contains(sellingPlayer)) {
                playersSoldInOR1.add(sellingPlayer);
            }
        }

        return result;
    }

    @Override
    public void processPhaseAction (String name, String value) {
        if (name.equalsIgnoreCase("CivilWar")) {
            for (PublicCompany company : getOperatingCompanies()) {
                if (company.hasFloated() && company.getPortfolio().getNumberOfTrains() > 0
                        && company.hasRoute()) {
                    ((PublicCompany_18TN)company).setCivilWar(true);
                }
            }
        }
    }


    @Override
    protected void executeSetRevenueAndDividend (SetDividend action) {

        // Save operating company (it may change)
        PublicCompany_18TN company = (PublicCompany_18TN) operatingCompany.get();

        super.executeSetRevenueAndDividend(action);

        // Reset Civil War condition
        if (company.isCivilWar()) company.setCivilWar(false);
    }
}
