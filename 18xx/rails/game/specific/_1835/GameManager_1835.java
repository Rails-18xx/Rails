/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1835/GameManager_1835.java,v 1.5 2010/03/28 20:14:20 evos Exp $ */
package rails.game.specific._1835;

import rails.game.*;

public class GameManager_1835 extends GameManager {

	public static String PR_NAME = PrussianFormationRound.PR_ID;
	private RoundI previousRound = null;
	private Player prFormStartingPlayer = null;

    public GameManager_1835() {
    	super();
    }

    @Override
    public void nextRound(RoundI round) {

        if (round instanceof PrussianFormationRound) {
            if (interruptedRound != null) {
                setRound(interruptedRound);
                interruptedRound.resume();
                interruptedRound = null;
            } else if (previousRound != null) {
                super.nextRound(previousRound);
                previousRound = null;
            }
        } else {
        	PhaseI phase = getCurrentPhase();
        	if (phase.getName().equals("4") || phase.getName().equals("4+4")
                    || phase.getName().equals("5")) {
        		if (!PrussianFormationRound.prussianIsComplete(this)) {
        			previousRound = round;
        			startPrussianFormationRound (null);
        		}
        	} else {
        		super.nextRound(round);
        	}
        }

    }
    
    public void startPrussianFormationRound(OperatingRound_1835 or) {

        interruptedRound = or;
    	createRound (PrussianFormationRound.class).start ();
    }

    public void setPrussianFormationStartingPlayer(Player prFormStartingPlayer) {
		this.prFormStartingPlayer = prFormStartingPlayer;
	}

	public Player getPrussianFormationStartingPlayer() {
    	return prFormStartingPlayer;
    }
}
