package rails.ui.swing.gamespecific._1880;

import rails.game.specific._1880.CloseInvestor_1880;
import rails.game.specific._1880.ExchangeForCash;
import rails.ui.swing.ORUIManager;

public class ORUIManager_1880 extends ORUIManager {

    protected void checkForGameSpecificActions() {
        if (possibleActions.contains(CloseInvestor_1880.class)) {
            ((GameUIManager_1880) gameUIManager).closeInvestor(possibleActions.getType(
                    CloseInvestor_1880.class).get(0));
        } else if (possibleActions.contains(ExchangeForCash.class)) {
            ((GameUIManager_1880) gameUIManager).exchangeForCash(possibleActions.getType(
                    ExchangeForCash.class).get(0));
        }
    }
    
}