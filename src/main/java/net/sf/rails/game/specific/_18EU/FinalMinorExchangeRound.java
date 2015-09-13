package net.sf.rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;

import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;


/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */

// requires: Seems like a Round with only one activity MergeCompanies
// Check rules how to best re-implement that
public final class FinalMinorExchangeRound extends StockRound_18EU {

    public FinalMinorExchangeRound(GameManager parent, String id) {
        super(parent, id);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);

        raiseIfSoldOut = false;
    }
    
    public static FinalMinorExchangeRound create(GameManager parent, String id){
        return new FinalMinorExchangeRound(parent, id);
    }

    public void start(Player playerToStartFMERound) {
        ReportBuffer.add(this, "");
        ReportBuffer.add(this, LocalText.getText("StartFinalMinorExchangeRound"));

        playerManager.setCurrentPlayer(playerToStartFMERound);
        initPlayer();
        ReportBuffer.add(this, LocalText.getText("HasFirstTurn",
                playerToStartFMERound.getId() ));
    }

    /*----- General methods -----*/

    @Override
    public boolean setPossibleActions() {

        if (discardingTrains.value()) {
            return setTrainDiscardActions();
        } else {
            return setMinorMergeActions();
        }

    }

    private boolean setMinorMergeActions() {

        if (hasActed.value()) {
            possibleActions.add(new NullAction(NullAction.Mode.DONE));
            return true;
        }

        List<PublicCompany> comps =
            companyManager.getAllPublicCompanies();
        List<PublicCompany> minors = new ArrayList<PublicCompany>();
        List<PublicCompany> targetCompanies = new ArrayList<PublicCompany>();
        String type;

        for (PublicCompany comp : comps) {
            type = comp.getType().getId();
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
            for (PublicCompany comp : comps) {
                type = comp.getType().getId();
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

        for (PublicCompany minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies, false));
        }

        return true;
    }

    @Override
    // Autopassing does not apply here
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        // TODO: Here no action is stored
        

        for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
            if (comp.getType().getId().equals("Minor")) {
                if (!comp.isClosed()) {
                    finishTurn();
                    return true;
                }
            }
        }

        finishRound();
        return true;
    }

    @Override
    protected void initPlayer() {

        currentPlayer = playerManager.getCurrentPlayer();
        hasActed.set(false);

    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    @Override
    public String toString() {
        return "FinalMinorExchangeRound";
    }
}