package net.sf.rails.ui.swing.gamespecific._1880;

import java.awt.BorderLayout;

import net.sf.rails.game.specific._1880.GameManager_1880;
import net.sf.rails.game.specific._1880.ParSlotManagerNG;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.StatusWindow;

public class StatusWindow_1880 extends StatusWindow {

    private static final long serialVersionUID = 1L;
    private ParSlotPanelNG parSlotsPanel;
    
/*    public StatusWindow_1880() {
        super();
    }*/
    
    
    public void init(GameUIManager gameUIManager) {
        super.init(gameUIManager);
        
        ParSlotManagerNG parSlotManager = ((GameManager_1880)gameUIManager.getGameManager()).getParSlotManager(); 
        parSlotsPanel = new ParSlotPanelNG(parSlotManager);
        
        pane.add(parSlotsPanel.getPanel(), BorderLayout.EAST);
        
    }

}
