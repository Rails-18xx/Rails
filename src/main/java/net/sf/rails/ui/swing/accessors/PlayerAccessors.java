package net.sf.rails.ui.swing.accessors;

import net.sf.rails.game.Player;
import net.sf.rails.game.model.MoneyModel;
import net.sf.rails.game.state.Accessor1D;

public class PlayerAccessors {
    
    public static final Accessor1D<String, Player> NAME = new Accessor1D<String, Player>() {
        @Override
        public String access(Player player) {
            return player.getId();
        }
    };
    
    public static final Accessor1D<MoneyModel, Player> BLOCKED_CASH = new Accessor1D<MoneyModel, Player>() {
        @Override
        public MoneyModel access(Player player) {
            return player.getBlockedCashModel();
        }
    };
    

}
