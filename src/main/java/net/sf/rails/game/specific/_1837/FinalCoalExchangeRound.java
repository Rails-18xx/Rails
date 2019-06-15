package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.specific._1837.StockRound_1837;

public class FinalCoalExchangeRound extends StockRound_1837 {

    public FinalCoalExchangeRound(GameManager parent, String id) {
        super(parent, id);
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);

        raiseIfSoldOut = false;
    }
    
    public static FinalCoalExchangeRound create(GameManager parent, String id){
        return new FinalCoalExchangeRound(parent, id);
    }

    public void start(Player playerToStartFMCRound) {
        ReportBuffer.add(this, "");
        ReportBuffer.add(this, LocalText.getText("StartFinalMinorExchangeRound"));

        playerManager.setCurrentPlayer(playerToStartFMCRound);
        initPlayer();
        ReportBuffer.add(this, LocalText.getText("HasFirstTurn",
                playerToStartFMCRound.getId() ));
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
        PublicCompany targetCompany = null;
        String type;

        for (PublicCompany comp : comps) {
            type = comp.getType().getId();
             if (type.equals("Coal")) {
                if (comp.isClosed()) continue;
                if (comp.getPresident() == currentPlayer) {
                    minors.add(comp);
                    targetCompany = companyManager.getPublicCompany(comp.getRelatedNationalCompany());
                }
            }
        }

        while (minors.isEmpty()) {
            setNextPlayer();
            for (PublicCompany comp : comps) {
                type = comp.getType().getId();
                if (type.equals("Coal")) {
                    if (comp.isClosed()) continue;
                    if (comp.getPresident() == currentPlayer) {
                        minors.add(comp);
                        targetCompany = companyManager.getPublicCompany(comp.getRelatedNationalCompany());
                    }
                }
            }
        }

        for (PublicCompany minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompany, true));
        }

        return true;
    }

    @Override
    // Autopassing does not apply here
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        // TODO: Here no action is stored
        

        for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
            if ((comp.getType().getId().equals("Coal")) && (!comp.isClosed())) {
                    finishTurn();
                    return true;
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
        return "FinalCoalExchangeRound";
    }
}
