package net.sf.rails.ui.swing.gamespecific._1880;

import net.sf.rails.game.specific._1880.GameManager_1880;
import net.sf.rails.game.specific._1880.ParSlotManager;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.StatusWindow;

import java.awt.*;

public class StatusWindow_1880 extends StatusWindow {

    private static final long serialVersionUID = 1L;
    private ParSlotPanel parSlotsPanel;
    private InvestorPanel investorPanel;
    
/*    public StatusWindow_1880() {
        super();
    }*/
    
    
    public void init(GameUIManager gameUIManager) {
        super.init(gameUIManager);

        ParSlotManager parSlotManager = ((GameManager_1880) gameUIManager.getGameManager()).getParSlotManager();
        parSlotsPanel = new ParSlotPanel(parSlotManager);

        pane.add(parSlotsPanel.getPanel(), BorderLayout.EAST);

        investorPanel = new InvestorPanel((GameManager_1880) gameUIManager.getGameManager());

        pane.add(investorPanel.getPanel(), BorderLayout.EAST);

    }

}
