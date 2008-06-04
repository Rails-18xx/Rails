/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/GameManager_18EU.java,v 1.2 2008/06/04 19:00:36 evos Exp $ */
package rails.game.specific._18EU;

import rails.game.GameManager;
import rails.game.RoundI;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    private OperatingRound_18EU lastOperatingRound = null;

    @Override
    public void nextRound(RoundI round) {
        if (round instanceof OperatingRound_18EU
            && ((OperatingRound_18EU) round).getPlayerToStartExchangeRound() != null) {
            lastOperatingRound = (OperatingRound_18EU) round;
            startFinalMinorExchangeRound(lastOperatingRound);
        } else if (round instanceof FinalMinorExchangeRound) {
            startStockRound();
        } else {
            super.nextRound(round);
        }

    }

    private void startFinalMinorExchangeRound(OperatingRound_18EU or) {

        FinalMinorExchangeRound sr = new FinalMinorExchangeRound();
        sr.start(or);
    }

}
