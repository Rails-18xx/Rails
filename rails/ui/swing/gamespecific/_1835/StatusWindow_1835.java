package rails.ui.swing.gamespecific._1835;

import java.util.List;

import javax.swing.JDialog;

import rails.game.*;
import rails.game.action.DiscardTrain;
import rails.game.special.ExchangeForShare;
import rails.game.specific._1835.*;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.StatusWindow;
import rails.ui.swing.elements.CheckBoxDialog;
import rails.ui.swing.elements.ConfirmationDialog;
import rails.util.LocalText;

public class StatusWindow_1835 extends StatusWindow {

    PublicCompanyI prussian;

    private static final long serialVersionUID = 1L;

    public StatusWindow_1835() {
        super();
    }

    @Override
	public void init (GameUIManager gameUIManager) {
        super.init(gameUIManager);
        prussian = gameUIManager.getGameManager().getCompanyManager().getPublicCompany(GameManager_1835.PR_ID);
    }

    @Override
    public void updateStatus(boolean myTurn) {
        RoundI currentRound = gameUIManager.getCurrentRound();
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

        List<CompanyI> foldables = action.getFoldableCompanies();
        JDialog currentDialog;

        if (foldables.get(0).getName().equals("M2")) {
            // Ask if the Prussian should be started
            currentDialog = new ConfirmationDialog (gameUIManager,
                    LocalText.getText("Select"),
                    LocalText.getText("MergeMinorConfirm",
                            getCurrentPlayer().getName(),
                            GameManager_1835.M2_ID, GameManager_1835.PR_ID),
                    "Yes",
                    "No");
        } else {
            // Ask if any other prePrussians should be folded
            String[] options = new String[foldables.size()];
            CompanyI company;
            for (int i=0; i<options.length; i++) {
            	company = foldables.get(i);
            	options[i] = LocalText.getText("MergeOption",
            			company.getName(),
            			company.getLongName(),
            			prussian.getName(),
            			((ExchangeForShare)(company.getSpecialProperties().get(0))).getShare()
            			);
            }
            currentDialog = new CheckBoxDialog (gameUIManager,
                    this,
                    LocalText.getText("Select"),
                    LocalText.getText("SelectCompaniesToFold", 
                            getCurrentPlayer().getName(),
                            prussian.getLongName()),
                    options);
        }
        gameUIManager.setCurrentDialog (currentDialog, action);
    }

}
