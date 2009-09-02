/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1856/GameManager_1856.java,v 1.2 2009/09/02 21:47:47 evos Exp $ */
package rails.game.specific._1856;

import rails.game.*;

public class GameManager_1856 extends GameManager {

    private Player playerToStartCGRFRound = null;

    public void startCGRFormationRound(OperatingRound_1856 or,
            Player playerToStartCGRFRound) {

        this.playerToStartCGRFRound = playerToStartCGRFRound;
        interruptedRound = or;

        if (this.playerToStartCGRFRound != null) {
            createRound (CGRFormationRound.class).start (this.playerToStartCGRFRound);
            this.playerToStartCGRFRound = null;
        }
    }

    @Override
    public void nextRound(RoundI round) {
        if (round instanceof CGRFormationRound) {
            setRound(interruptedRound);
            ((OperatingRound_1856)interruptedRound).resume(((CGRFormationRound)round).getMergingCompanies());
        } else {
            super.nextRound(round);
        }

    }

}
