package net.sf.rails.game.specific._1835;

import net.sf.rails.common.GameOption;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.round.RoundFacade;


public class GameManager_1835 extends GameManager {

	private Round previousRound = null;
    private Player prFormStartingPlayer = null;

    public static final String M2_ID = "M2";
    public static final String PR_ID = "PR";
    public static final String OL_ID = "OL";
    public static final String MS_ID = "MS";
    public static final String WT_ID = "WT";
    public static final String HE_ID = "HE";
    public static final String BA_ID = "BA";
    public static final String SX_ID = "SX";
    public static final String BY_ID = "BY";

    public GameManager_1835(RailsRoot parent, String id) {
        super(parent, id);
    }

    /** In standard 1835, minors can run even if the start packet has not been completely sold,
     * unless the "MinorsRequireFloatedBY" option is in effect and the Bayerische
     * has not yet floated.
     * @return true only if minors can run.
     */
    @Override
    protected boolean runIfStartPacketIsNotCompletelySold() {
        if (GameOption.getAsBoolean(this, "Clemens")
                || GameOption.getAsBoolean(this, "MinorsRequireFloatedBY")) {
            return getRoot().getCompanyManager().getPublicCompany(GameManager_1835.BY_ID).hasFloated();
        }
        return true;
    }

    @Override
    public void nextRound(Round round) {

        if (round instanceof PrussianFormationRound) {
            RoundFacade interruptedRound = getInterruptedRound();
            if (interruptedRound != null) {
                setRound(interruptedRound);
                interruptedRound.resume();
                setInterruptedRound(null);
            } else if (previousRound != null) {
                super.nextRound(previousRound);
                previousRound = null;
            }
        } else {
        	Phase phase = getCurrentPhase();
            if ((phase.getId().equals("4") || phase.getId().equals("4+4")
                    || phase.getId().equals("5"))
                    && !PrussianFormationRound.prussianIsComplete(this)) {
                previousRound = round;
                startPrussianFormationRound (null);
            } else {
                super.nextRound(round);
            }
        }

    }

    public void startPrussianFormationRound(OperatingRound_1835 or) {
        setInterruptedRound(or);
        String roundName;
        if (getInterruptedRound() == null) {
            // after a round
            roundName = "PrussianFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "PrussianFormationRound_in_" + or.getId() + "_after_" + getCurrentPhase().getId();
        }
    	createRound(PrussianFormationRound.class, roundName).start();
    }

    public void setPrussianFormationStartingPlayer(Player prFormStartingPlayer) {
        this.prFormStartingPlayer = prFormStartingPlayer;
    }

    public Player getPrussianFormationStartingPlayer() {
        return prFormStartingPlayer;
    }

    @Override
    public int getPlayerCertificateLimit(Player player) {
        int limit = getRoot().getPlayerManager().getPlayerCertificateLimit(player);
        for (PublicCompany company : getRoot().getCompanyManager().getAllPublicCompanies()) {
            if (company.getType().getId().equalsIgnoreCase("Major")
                    && company.getPresident() == player
                    && player.getPortfolioModel().getShare(company) >= 80) limit++;
        }
        return limit;
    }

    /** Pick a new president for a company of which the president went bankrupt */
    @Override
    protected Player processCompanyAfterPlayerBankruptcy(Player oldPresident, PublicCompany company) {

        // Is there any player with one share?
        Player newPresident = null;
        PlayerManager pm = getRoot().getPlayerManager();
        for (Player player : pm.getNextPlayers(false)) {
            int shares = player.getPortfolioModel().getShares(company);
            if (shares == 1) {
                newPresident = player;
                break;
            }
        }

        // Otherwise, the priority holder will have to do it.
        if (newPresident == null) newPresident = pm.getPriorityPlayer();

        ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                newPresident.getId(),
                company.getId()));

        return newPresident;
    }

    @Override
    public void finishShareSellingRound(boolean resume) {
        int remainingCashToRaise = ((ShareSellingRound)getCurrentRound()).getRemainingCashToRaise();
        OperatingRound_1835 or = (OperatingRound_1835) getInterruptedRound();
        setRound(or);
        guiHints.setCurrentRoundType(getInterruptedRound().getClass());
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        setInterruptedRound(null);
        or.resumeAfterSSR(remainingCashToRaise);
    }


}
