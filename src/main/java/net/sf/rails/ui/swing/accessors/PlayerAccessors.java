package net.sf.rails.ui.swing.accessors;

import net.sf.rails.game.Player;
import net.sf.rails.game.model.MoneyModel;
import net.sf.rails.ui.swing.core.Accessor1D;

public class PlayerAccessors {
    
    public static final Accessor1D.AText<Player> NAME = 
            new Accessor1D.AText<Player>(Player.class) {
                @Override
                public String access(Player player) {
                    return player.getNameAndPriority();
                }
        };
 
    public static final Accessor1D.AObservable<Player> FREE =
            new Accessor1D.AObservable<Player>(Player.class) {
        @Override
        public MoneyModel access(Player player) {
            return player.getFreeCashModel();
        }
};
            
        
        
}
