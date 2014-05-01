package net.sf.rails.game.specific._1862;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;


public class GameManager_1862 extends GameManager {

    private Player playerToStartCGRFRound = null;

    public GameManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    }

    public void startCGRFormationRound(OperatingRound_1862 or,
            Player playerToStartCGRFRound) {

        this.playerToStartCGRFRound = playerToStartCGRFRound;
        interruptedRound = or;

        if (this.playerToStartCGRFRound != null) {
            // TODO: this id will not work
            createRound (CGRFormationRound.class, "CGRFormationRound").start (this.playerToStartCGRFRound);
            this.playerToStartCGRFRound = null;
        }
    }

    @Override
    public void nextRound(Round round) {
        if (round instanceof CGRFormationRound) {
            setRound(interruptedRound);
            ((OperatingRound_1862)interruptedRound).resume(((CGRFormationRound)round).getMergingCompanies());
        } else {
            super.nextRound(round);
        }

    }

}
