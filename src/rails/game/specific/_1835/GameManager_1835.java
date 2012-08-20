/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1835/GameManager_1835.java,v 1.9 2010/05/15 19:05:39 evos Exp $ */
package rails.game.specific._1835;

import rails.game.*;
import rails.game.state.Item;

public class GameManager_1835 extends GameManager {

	private Round previousRound = null;
	private Player prFormStartingPlayer = null;

    public static String M2_ID = "M2";
    public static String PR_ID = "PR";
    public static String OL_ID = "OL";
    public static String MS_ID = "MS";
    public static String WT_ID = "WT";
    public static String HE_ID = "HE";
    public static String BA_ID = "BA";
    public static String SX_ID = "SX";
    public static String BY_ID = "BY";
    
    public GameManager_1835(Item parent, String id) {
        super(parent, id);
    }
    
    @Override
    public void nextRound(Round round) {

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
        	Phase phase = getCurrentPhase();
        	if ((phase.getName().equals("4") || phase.getName().equals("4+4")
        	                || phase.getName().equals("5"))
        	            && !PrussianFormationRound.prussianIsComplete(this)) {
    			previousRound = round;
    			startPrussianFormationRound (null);
        	} else {
        		super.nextRound(round);
        	}
        }

    }

    public void startPrussianFormationRound(OperatingRound_1835 or) {

        interruptedRound = or;
    	createRound(PrussianFormationRound.class, "PrussianFormationRound").start ();
    }

    public void setPrussianFormationStartingPlayer(Player prFormStartingPlayer) {
		this.prFormStartingPlayer = prFormStartingPlayer;
	}

	public Player getPrussianFormationStartingPlayer() {
    	return prFormStartingPlayer;
    }

    @Override
    public int getPlayerCertificateLimit(Player player) {
        int limit = playerCertificateLimit.value();
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (company.getTypeName().equalsIgnoreCase("Major")
                    && company.getPresident() == player
                    && player.getPortfolioModel().getShare(company) >= 80) limit++;
        }
        return limit;
    }

}
