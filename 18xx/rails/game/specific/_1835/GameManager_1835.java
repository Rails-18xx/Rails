/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1835/GameManager_1835.java,v 1.2 2010/03/04 22:08:23 evos Exp $ */
package rails.game.specific._1835;

import rails.game.*;

public class GameManager_1835 extends GameManager {

	public static String PR_NAME = PrussianFormationRound.PR_ID;
	private RoundI previousRound;
	private Player prFormStartingPlayer = null;

    public GameManager_1835() {
    	super();
    }

    @Override
    public void nextRound(RoundI round) {

        if (round instanceof PrussianFormationRound) {
        	super.nextRound(previousRound);
        } else {
        	PhaseI phase = getCurrentPhase();
        	if (phase.getName().equals("4") || phase.getName().equals("4+4")
                    || phase.getName().equals("5")) {
        		if (!PrussianFormationRound.prussianIsComplete(this)) {
        			previousRound = round;
        			startPrussianFormationRound ();
        		}
        	} else {
        		super.nextRound(round);
        	}
        }

    }

    private void startPrussianFormationRound() {

    	createRound (PrussianFormationRound.class).start ();
    }

    public void setPrussianFormationStartingPlayer(Player prFormStartingPlayer) {
		this.prFormStartingPlayer = prFormStartingPlayer;
	}

	public Player getPrussianFormationStartingPlayer() {
    	return prFormStartingPlayer;
    }
}
