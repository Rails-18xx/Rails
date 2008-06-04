package rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;
import rails.game.move.MoveSet;
import rails.util.LocalText;

/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */
public class FinalMinorExchangeRound extends StockRound_18EU {
    OperatingRound_18EU lastOR;

    /**
     * The constructor.
     */
    public FinalMinorExchangeRound() {}

    public void start(OperatingRound_18EU lastOR) {
        ReportBuffer.add("\n"
                         + LocalText.getText("StartFinalMinorExchangeRound"));

        this.lastOR = lastOR;
        Player firstPlayer = lastOR.getPlayerToStartExchangeRound();
        GameManager.setCurrentPlayerIndex(firstPlayer.getIndex());
        initPlayer();
        ReportBuffer.add(LocalText.getText("HasFirstTurn",
                new String[] { currentPlayer.getName() }));
    }

    /*----- General methods -----*/

    @Override
    public boolean setPossibleActions() {

        if (discardingTrains.booleanValue()) {
            return setTrainDiscardActions();
        } else {
            return setMinorMergeActions();
        }

    }

    private boolean setMinorMergeActions() {

        if (hasActed.booleanValue()) {
            possibleActions.add(new NullAction(NullAction.DONE));
            return true;
        }

        List<PublicCompanyI> comps =
                Game.getCompanyManager().getAllPublicCompanies();
        List<PublicCompanyI> minors = new ArrayList<PublicCompanyI>();
        List<PublicCompanyI> targetCompanies = new ArrayList<PublicCompanyI>();
        String type;

        for (PublicCompanyI comp : comps) {
            type = comp.getTypeName();
            if (type.equals("Major") && comp.hasStarted() && !comp.isClosed()) {
                targetCompanies.add(comp);
            } else if (type.equals("Minor")) {
                if (comp.isClosed()) continue;
                if (comp.getPresident() == currentPlayer) {
                    minors.add(comp);
                }
            }
        }

        while (minors.isEmpty()) {
            setNextPlayer();
            for (PublicCompanyI comp : comps) {
                type = comp.getTypeName();
                if (type.equals("Minor")) {
                    if (comp.isClosed()) continue;
                    if (comp.getPresident() == currentPlayer) {
                        minors.add(comp);
                    }
                }
            }
        }
        // Use null target to indicate minor closing is an option
        targetCompanies.add(null);

        for (PublicCompanyI minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies));
        }

        return true;
    }

    @Override
    public boolean done(String playerName) {

        MoveSet.start(false);

        for (PublicCompanyI comp : Game.getCompanyManager().getAllPublicCompanies()) {
            if (comp.getTypeName().equals("Minor")) {
                if (!comp.isClosed()) {
                    finishTurn();
                    return true;
                }
            }
        }

        finishRound();
        return true;
    }

    /*
     * @Override protected void setNextPlayer() { if
     * (!discardingTrains.booleanValue()) { // Check if any player has a minor
     * left for (int i=0; i<GameManager.getNumberOfPlayers(); i++) {
     * GameManager.setNextPlayer(); currentPlayer =
     * GameManager.getCurrentPlayer(); for (PublicCertificateI cert :
     * currentPlayer.getPortfolio().getCertificates()) { if
     * (cert.getCompany().getTypeName().equals("Minor")) { initPlayer(); return; } } } //
     * No more minors, get rid of any excess trains discardingTrains.set(true);
     * 
     * if (compWithExcessTrains.isEmpty()) { gameMgr.nextRound (this); return; } //
     * Make up a list of train discarding companies in sequence of the last OR //
     * TODO: this disregards any changes in the operating sequence // during the
     * last OR. This is probably wrong. PublicCompanyI[] operatingCompanies =
     * lastOR.getOperatingCompanies(); discardingCompanies = new
     * PublicCompanyI[compWithExcessTrains.size()]; for (int i=0, j=0; i<operatingCompanies.length;
     * i++) { if (compWithExcessTrains.contains(operatingCompanies[i])) {
     * discardingCompanies[j++] = operatingCompanies[i]; } }
     * 
     * discardingCompanyIndex = new IntegerState ("DiscardingCompanyIndex", 0); }
     * else { PublicCompanyI comp =
     * discardingCompanies[discardingCompanyIndex.intValue()]; if
     * (comp.getNumberOfTrains() <= comp.getCurrentTrainLimit()) {
     * discardingCompanyIndex.add(1); } }
     * 
     * if (discardingCompanyIndex.intValue() >= discardingCompanies.length) { //
     * All excess trains have been discarded gameMgr.nextRound (this); return; } }
     */

    @Override
    protected void initPlayer() {

        currentPlayer = GameManager.getCurrentPlayer();
        hasActed.set(false);

    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    @Override
    public String toString() {
        return "FinalMinorExchangeRound";
    }
}
