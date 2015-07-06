package net.sf.rails.ui.swing.gamespecific._1880;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;

import net.sf.rails.game.model.ParSlotModel;
import net.sf.rails.game.specific._1880.GameManager_1880;
import net.sf.rails.game.specific._1880.ParSlotManager_1880;
import net.sf.rails.game.state.StringState;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.GridPanel;
import net.sf.rails.ui.swing.elements.Caption;
import net.sf.rails.ui.swing.elements.Field;

//public class ParSlotsPanel extends GridPanel {
//    private static final long serialVersionUID = 1L;
//
//    
//    
//    public void actionPerformed(ActionEvent e) {
//        
//    }
//
//    public void init(GameUIManager gameUIManager) {
//        gridPanel = this;
//        
//        gb = new GridBagLayout();
//        this.setLayout(gb);
//        
//        gbc = new GridBagConstraints();
//        setSize(10, 10);
//        setBorder(BorderFactory.createEtchedBorder());
//
//        addField(new Caption("    Par Slots    "), 0, 0, 2, 1, WIDE_BOTTOM, true);
//
//        addField(new Caption("<html>Last<br>Train</html>"), 2, 0, 1, 1, WIDE_BOTTOM, true);
//        
//        ParSlotManager_1880 parSlotManager = ((GameManager_1880) gameUIManager.getGameManager()).getParSlotManager();
//        for (int i = 0; i < 4; i++) {
//            addField(new Caption("$" + (100 - (10 * i))), 0, (1 + 4*i), 1, 4, WIDE_BOTTOM, true);
//            for (int j = 0; j < 4; j++) {
//                ParSlotModel parSlotModel = parSlotManager.getSlotAtIndex((i*4)+j);
//                addField(new Field(parSlotModel), 1, (i * 4) + j + 1, 1, 1, WIDE_LEFT + WIDE_RIGHT, true);
//                StringState lastTrainState = parSlotManager.getLastTrainStateAtSlot((i*4)+j);
//                addField(new Field(lastTrainState), 2, (i * 4) + j + 1, 1, 1, WIDE_LEFT + WIDE_RIGHT, true);
//            }
//        }
//    }
//
//}
