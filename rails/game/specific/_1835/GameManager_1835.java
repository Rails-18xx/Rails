/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1835/GameManager_1835.java,v 1.7 2010/04/09 21:26:11 evos Exp $ */
package rails.game.specific._1835;

import rails.game.*;

public class GameManager_1835 extends GameManager {

	public static String PR_NAME = PrussianFormationRound.PR_ID;
	private RoundI previousRound = null;
	private Player prFormStartingPlayer = null;
    
    public static String M2_ID = "M2";
    public static String PR_ID = "Pr";
    public static String OL_ID = "Old";
    public static String MS_ID = "MS";
    public static String WT_ID = "Wrt";
    public static String HE_ID = "Hes";
    public static String BA_ID = "Bad";
    public static String SX_ID = "Sax";
    public static String BY_ID = "Bay";

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
    
    public int getPlayerCertificateLimit(Player player) {
        int limit = playerCertificateLimit.intValue();
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            if (company.getTypeName().equalsIgnoreCase("Major")
                    && company.getPresident() == player 
                    && player.getPortfolio().getShare(company) >= 80) limit++;
        }
        return limit;
    }

}
