package rails.game.specific._18TN;

import rails.common.parser.ConfigurationException;
import rails.game.GameManagerI;
import rails.game.PublicCompany;
import rails.game.model.ModelObject;
import rails.game.state.BooleanState;

public class PublicCompany_18TN extends PublicCompany {

    private BooleanState civilWar;


    public ModelObject getCivilWar() {
        return civilWar;
    }

    @Override
    public void finishConfiguration(GameManagerI gameManager)
    throws ConfigurationException {
        
        super.finishConfiguration(gameManager);

        civilWar = new BooleanState (name+"_CivilWar", false);
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


}
