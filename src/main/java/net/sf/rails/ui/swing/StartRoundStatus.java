package net.sf.rails.ui.swing;

import java.util.List;

import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.ui.swing.accessors.PlayerAccessors;
import net.sf.rails.ui.swing.core.GridAxis;
import net.sf.rails.ui.swing.core.GridTable;

/**
 * StartRoundStatus creates a status panel for the StartRound
 */
public class StartRoundStatus {
    
    private enum Rows {HEADER, BID, FREE, FOOTER}
    private enum Cols {ITEM, BASE, MINIMUM, INFO}
    
    
    public void create(List<StartItem> items, List<Player> players) {
        
        GridAxis rows = GridAxis.builder()
                .add(Rows.HEADER).add(items, StartItem.class).add(Rows.BID).add(Rows.FREE).add(Rows.FOOTER)
                .build();
        
        GridAxis cols = GridAxis.builder()
                .add(Cols.ITEM).add(Cols.BASE).add(Cols.MINIMUM).add(players, Player.class).add(Cols.INFO)
                .build();
        
        GridTable table = GridTable.builder(rows, cols)
                //.add(PlayerAccessors.NAME)
                .build();
    }

}
