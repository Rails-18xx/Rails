package rails.game.specific._18TN;

import java.util.Collections;
import java.util.List;

import rails.algorithms.NetworkTrain;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueStaticModifier;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.game.GameManager;
import rails.game.PublicCompany;
import rails.game.state.BooleanState;

public class PublicCompany_18TN extends PublicCompany implements RevenueStaticModifier {

    private BooleanState civilWar;

    public BooleanState getCivilWar() {
        return civilWar;
    }

    @Override
    public void finishConfiguration(GameManager gameManager)
    throws ConfigurationException {
        
        super.finishConfiguration(gameManager);

        civilWar = BooleanState.create(this, name+"_CivilWar", false);
        
        gameManager.getRevenueManager().addStaticModifier(this);
    }

    public boolean isCivilWar() {
        return civilWar.booleanValue();
    }

    public void setCivilWar(boolean value) {
        civilWar.set(value);
    }

    /** Don't move the space if the company has one train in the civil war
     * (the revenue amount must then be zero)
     */
    @Override
    public void withhold(int amount) {
        if (isCivilWar() && portfolio.getNumberOfTrains() == 1) return;
        stockMarket.withhold(this);
    }

    /**
     * Modify the revenue calculation for the civil war by removing the shortest train
     */
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        
        // check first if it is the company for the revenue calculation
        if (revenueAdapter.getCompany() != this) return false;
        
        // check if it is civil war, otherwise no effect
        if (!isCivilWar()) return false;
        
        List<NetworkTrain> trains = revenueAdapter.getTrains();
        if (trains.size() == 0) return false; // no train, no effect
        
        // sort trains in ascending order (by domination which is equal to length for TN)
        Collections.sort(trains);
        
        // and remove the first train (shortest)
        trains.remove(0);
        
        return true;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return LocalText.getText("CivilWarActive");
    }

}
