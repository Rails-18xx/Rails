package net.sf.rails.ui.swing;

import java.util.List;

import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.state.Accessor1D;
import net.sf.rails.ui.swing.accessors.PlayerAccessors;
import net.sf.rails.ui.swing.core.TableAxis;

/**
 * StartRoundStatus creates a status panel for the StartRound
 */
public class StartRoundStatus {
    
    private enum Cols {ITEM, BASE, MINIMUM, INFO}
    
    public void create(List<Player> players, List<StartItem> items) {
        TableAxis cols = TableAxis.builder()
                .add(Cols.ITEM).add(Cols.BASE).add(Cols.MINIMUM).add(players,PlayerAccessors.NAME).add(Cols.INFO)
                .build();
        
        
        
    }

}
