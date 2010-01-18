/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/GameManager_18EU.java,v 1.5 2010/01/18 22:51:47 evos Exp $ */
package rails.game.specific._18EU;

import rails.game.GameManager;
import rails.game.Player;
import rails.game.RoundI;
import rails.game.state.State;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    protected State playerToStartFMERound = 
        new State("playerToStartFMERound", Player.class);

    @Override
    public void nextRound(RoundI round) {
        if (round instanceof OperatingRound_18EU) {
            if (playerToStartFMERound.getObject() != null
                    && relativeORNumber.intValue() == numOfORs) {
                createRound (FinalMinorExchangeRound.class).start 
                        ((Player)playerToStartFMERound.getObject());
                playerToStartFMERound.set(null);
            } else {
                super.nextRound(round);
            }
        } else if (round instanceof FinalMinorExchangeRound) {
            startStockRound();
        } else {
            super.nextRound(round);
        }

    }

    public void setPlayerToStartFMERound(Player playerToStartFMERound) {
        this.playerToStartFMERound.set(playerToStartFMERound);
    }

    public Player getPlayerToStartFMERound() {
        return (Player) playerToStartFMERound.getObject();
    }
    
    

}
