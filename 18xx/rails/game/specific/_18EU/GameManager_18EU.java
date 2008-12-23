/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/GameManager_18EU.java,v 1.4 2008/12/23 20:02:18 evos Exp $ */
package rails.game.specific._18EU;

import rails.game.GameManager;
import rails.game.Player;
import rails.game.RoundI;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    private Player playerToStartFMERound = null;

    @Override
    public void nextRound(RoundI round) {
        if (round instanceof OperatingRound_18EU) {
            if (((OperatingRound_18EU) round).getPlayerToStartExchangeRound() != null) {
                playerToStartFMERound = ((OperatingRound_18EU) round).getPlayerToStartExchangeRound();
            }
            if (playerToStartFMERound != null
                    && relativeORNumber.intValue() == numOfORs) {
                createRound (FinalMinorExchangeRound.class).start (playerToStartFMERound);
                playerToStartFMERound = null;
            } else {
                super.nextRound(round);
            }
        } else if (round instanceof FinalMinorExchangeRound) {
            startStockRound();
        } else {
            super.nextRound(round);
        }

    }

}
