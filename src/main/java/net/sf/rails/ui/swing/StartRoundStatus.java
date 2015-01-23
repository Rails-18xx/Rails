package net.sf.rails.ui.swing;

import java.util.List;

import javax.swing.JPanel;

import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.ui.swing.accessors.PlayerACCs;
import net.sf.rails.ui.swing.accessors.StartItemACCs;
import net.sf.rails.ui.swing.core.GridAxis;
import net.sf.rails.ui.swing.core.GridTable;
import net.sf.rails.ui.swing.core.TableUI;

/**
 * StartRoundStatus creates a status panel for the StartRound
 */
public class StartRoundStatus {
    
    private enum Rows {HEADER, BID, FREE}
    private enum Cols {ITEM, BASE, MINIMUM, INFO}
    
    private final JPanel panel;
    
    public StartRoundStatus(List<StartItem> items, List<Player> players) {
        
        GridAxis rows = GridAxis.builder()
                .add(Rows.HEADER).add(items, StartItem.class).add(Rows.BID).add(Rows.FREE)
                .build();
        
        GridAxis cols = GridAxis.builder()
                .add(Cols.ITEM).add(Cols.BASE).add(Cols.MINIMUM).add(players, Player.class).add(Cols.INFO)
                .build();
        
        GridTable gridTable = GridTable.builder(rows, cols)
                .row().add("Item").add("Base Price").add("Min. Bid").add(PlayerACCs.NAME).add("")
                .row().add(StartItemACCs.NAME).add(StartItemACCs.BASE_PRICE).add(StartItemACCs.MIN_BID).add(StartItemACCs.CUR_BID).add("")
                .row().add("").add("").add("Bid").add(PlayerACCs.BLOCKED).add("")
                .row().add("").add("").add("Free").add(PlayerACCs.FREE).add("")
                .build();
        
        TableUI table = TableUI.from(gridTable);
        
        panel = table.convertToPanel();
    }
    
    public JPanel getPanel() {
        return panel;
    }

}
