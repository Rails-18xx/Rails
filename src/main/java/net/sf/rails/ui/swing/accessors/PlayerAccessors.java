package net.sf.rails.ui.swing.accessors;

import net.sf.rails.game.Player;
import net.sf.rails.game.state.Accessor;

public class PlayerAccessors {
    
    public static final Accessor<Player> SELF = new Accessor<Player>() {
        public Player access(Player parent) {
            return parent;
        }
    };
    
    

}
