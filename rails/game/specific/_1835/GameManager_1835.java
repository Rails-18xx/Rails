/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1835/GameManager_1835.java,v 1.9 2010/05/15 19:05:39 evos Exp $ */
package rails.game.specific._1835;

import rails.common.parser.GameOption;
import rails.game.*;

public class GameManager_1835 extends GameManager {

    private RoundI previousRound = null;
    private Player prFormStartingPlayer = null;

    public static String M1_ID = "M1";
    public static String M2_ID = "M2";
    public static String PR_ID = "PR";
    public static String OL_ID = "OL";
    public static String MS_ID = "MS";
    public static String WT_ID = "WT";
    public static String HE_ID = "HE";
    public static String BA_ID = "BA";
    public static String SX_ID = "SX";
    public static String BY_ID = "BY";

    public GameManager_1835() {
        super();
    }

    /** In standard 1835, minors can run even if the start packet has not been completely sold,
     * unless the "MinorsRequireFloatedBY" option is in effect and the Bayerische
     * has not yet floated.
     * @return true only if minors can run.
     */
    @Override
    protected boolean runIfStartPacketIsNotCompletelySold() {
        if (getGameOption(GameOption.VARIANT).equalsIgnoreCase("Clemens")
                || getGameOption("MinorsRequireFloatedBY").equalsIgnoreCase("yes")) {
            return companyManager.getPublicCompany(GameManager_1835.BY_ID).hasFloated();
        }
        return true;
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
        createRound (PrussianFormationRound.class).start ();
    }

    public void setPrussianFormationStartingPlayer(Player prFormStartingPlayer) {
        this.prFormStartingPlayer = prFormStartingPlayer;
    }

    public Player getPrussianFormationStartingPlayer() {
        return prFormStartingPlayer;
    }

    @Override
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
