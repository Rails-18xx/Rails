/**
 * 
 */
package rails.ui.swing.gamespecific._1880;

import java.awt.BorderLayout;

import rails.game.StockRound;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.StatusWindow;

/**
 * @author Martin
 * @date 07.05.2011
 */
public class StatusWindow_1880 extends StatusWindow {

    private static final long serialVersionUID = 1L;
    private ParSlotsPanel parSlotsPanel;
    
    public StatusWindow_1880() {
        super();
    }
    
    
    public void init(GameUIManager gameUIManager) {
        super.init(gameUIManager);
        
        parSlotsPanel = new ParSlotsPanel();
        parSlotsPanel.init(gameUIManager);
        
        pane.add(parSlotsPanel, BorderLayout.EAST);
        
    }

}
