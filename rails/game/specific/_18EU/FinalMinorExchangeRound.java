package rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.common.GuiDef;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;


/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */
public class FinalMinorExchangeRound extends StockRound_18EU {

    public FinalMinorExchangeRound(GameManager aGameManager) {
        super (aGameManager);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);
    }

    public void start(Player playerToStartFMERound) {
        ReportBuffer.add("");
        ReportBuffer.add(LocalText.getText("StartFinalMinorExchangeRound"));

        setCurrentPlayerIndex(playerToStartFMERound.getIndex());
        initPlayer();
        ReportBuffer.add(LocalText.getText("HasFirstTurn",
                playerToStartFMERound.getId() ));
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

        List<PublicCompany> comps =
                companyManager.getAllPublicCompanies();
        List<PublicCompany> minors = new ArrayList<PublicCompany>();
        List<PublicCompany> targetCompanies = new ArrayList<PublicCompany>();
        String type;

        for (PublicCompany comp : comps) {
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
            for (PublicCompany comp : comps) {
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

        for (PublicCompany minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies));
        }

        return true;
    }

    @Override
    // Autopassing does not apply here
    public boolean done(String playerName, boolean hasAutopassed) {

        // TODO: changeStack.start(false);

        for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
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

    @Override
    protected void initPlayer() {

        currentPlayer = getCurrentPlayer();
        hasActed.set(false);

    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    @Override
    public String toString() {
        return "FinalMinorExchangeRound";
    }
}