package net.sf.rails.game.model;

import net.sf.rails.game.Player;
import net.sf.rails.game.state.Model;

/**
 * PlayerNameModel contains both the name of the player
 * and information if the player has priority, thus it has to be observable
 *
 */
public class PlayerNameModel extends Model {
    
    public static final String ID = "PresidentModel";  
    
    private PlayerNameModel(Player player, String id) {
        super(player, id);
        // add dependency on the playerOrder
        player.getParent().getPlayerOrderModel().addModel(this);
    }

    public static PlayerNameModel create(Player player){
        return new PlayerNameModel(player, ID);
    }
    
    /**
     * @return restricted to Player
     */
    @Override
    public Player getParent() {
        return (Player)super.getParent();
    }
    
    @Override
    public String toText() {
        return getParent().getNameAndPriority();
    }

}
