package net.sf.rails.ui.swing.gamespecific._1835;

import java.util.List;

import rails.game.action.DiscardTrain;
import rails.game.specific._1835.FoldIntoPrussian;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.specific._1835.*;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.StatusWindow;
import net.sf.rails.ui.swing.elements.*;

import com.google.common.collect.Iterables;


public class StatusWindow_1835 extends StatusWindow {

    PublicCompany prussian;

    private static final long serialVersionUID = 1L;

    public StatusWindow_1835() {
        super();
    }

    @Override
    public void init (GameUIManager gameUIManager) {
        super.init(gameUIManager);
        prussian = gameUIManager.getRoot().getCompanyManager().getPublicCompany(GameManager_1835.PR_ID);
    }

    @Override
    public void updateStatus(boolean myTurn) {
        RoundFacade currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof PrussianFormationRound)) {
            super.updateStatus(myTurn);
        } else if (possibleActions.contains(FoldIntoPrussian.class)) {
            immediateAction = possibleActions.getType(FoldIntoPrussian.class).get(0);
        } else if (possibleActions.contains(DiscardTrain.class)) {
            immediateAction = possibleActions.getType(DiscardTrain.class).get(0);
        }
    }


    @Override
    public boolean processImmediateAction() {

        if (immediateAction == null) {
            return false;
        } else if (immediateAction instanceof FoldIntoPrussian) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            FoldIntoPrussian nextAction = (FoldIntoPrussian) immediateAction;
            immediateAction = null;
            fold (nextAction);
            return true;
        } else {
            return super.processImmediateAction();
        }
    }

    // Code partly copied from ORUIManager
    protected void fold (FoldIntoPrussian action) {

        List<Company> foldables = action.getFoldableCompanies();
        NonModalDialog currentDialog;

        if (foldables.get(0).getId().equals("M2")) {
            // Ask if the Prussian should be started
            currentDialog = new ConfirmationDialog (GameUIManager_1835.START_PRUSSIAN_DIALOG,
                    gameUIManager, this,
                    LocalText.getText("Select"),
                    LocalText.getText("MergeMinorConfirm",
                            getCurrentPlayer().getId(),
                            GameManager_1835.M2_ID, GameManager_1835.PR_ID),
                            "Yes",
                            "No"
            );
        } else {
            // Ask if any other prePrussians should be folded
            String[] options = new String[foldables.size()];
            Company company;
            for (int i=0; i<options.length; i++) {
                company = foldables.get(i);
                options[i] = LocalText.getText("MergeOption",
            			company.getId(),
                        company.getLongName(),
            			prussian.getId(),
            			((ExchangeForShare)(Iterables.get(company.getSpecialProperties(),0))).getShare()
                );
            }
            currentDialog = new CheckBoxDialog (GameUIManager_1835.MERGE_INTO_PRUSSIAN_DIALOG,
                    gameUIManager,
                    this,
                    LocalText.getText("Select"),
                    LocalText.getText("SelectCompaniesToFold",
                            getCurrentPlayer().getId(),
                            prussian.getLongName()),
                            options);
        }
        gameUIManager.setCurrentDialog (currentDialog, action);
        disableButtons();
    }

}
