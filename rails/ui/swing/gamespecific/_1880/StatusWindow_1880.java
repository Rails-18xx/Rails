/**
 * 
 */
package rails.ui.swing.gamespecific._1880;

import rails.game.StockRound;
import rails.ui.swing.StatusWindow;

/**
 * @author Martin
 * @date 07.05.2011
 */
public class StatusWindow_1880 extends StatusWindow {

    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    public StatusWindow_1880() {
        super();
    }

    @Override
    public boolean updateGameSpecificSettings(){
        if (currentRound instanceof StockRound){
            if (((StockRound) currentRound).getStockRoundNumber() ==1){
                gameStatus.init((StatusWindow) getParent(), gameUIManager);
            }
        }
        return false;
        
    }
    
}
