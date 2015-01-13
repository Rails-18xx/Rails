package net.sf.rails.ui.swing.accessors;

import net.sf.rails.game.Player;
import net.sf.rails.game.model.MoneyModel;
import net.sf.rails.ui.swing.core.Accessor1D;

public class PlayerACCs {
    
    public static final Accessor1D.AText<Player> NAME = 
            new Accessor1D.AText<Player>(Player.class) {
        @Override
        protected String access(Player player) {
            return player.getNameAndPriority();
        }
    };
    
    public static final Accessor1D.AObservable<Player> BLOCKED =
            new Accessor1D.AObservable<Player>(Player.class) {
        @Override
        protected MoneyModel access(Player player) {
            return player.getBlockedCashModel();
        }
    };

    public static final Accessor1D.AObservable<Player> FREE =
            new Accessor1D.AObservable<Player>(Player.class) {
        @Override
        protected MoneyModel access(Player player) {
            return player.getFreeCashModel();
        }
    };
        
}
