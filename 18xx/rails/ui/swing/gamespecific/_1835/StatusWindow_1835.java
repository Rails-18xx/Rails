package rails.ui.swing.gamespecific._1835;

import java.util.List;

import javax.swing.JDialog;

import rails.game.*;
import rails.game.specific._1835.FoldIntoPrussian;
import rails.game.specific._1835.PrussianFormationRound;
import rails.game.specific._1835.StockRound_1835;
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
    
    public void init (GameUIManager gameUIManager) {
        super.init(gameUIManager);
        prussian = gameUIManager.getGameManager().getCompanyManager().getCompanyByName(StockRound_1835.PR_ID);
    }

    @Override
    public void updateStatus() {
        RoundI currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof PrussianFormationRound)) {
            super.updateStatus();
        } else if (possibleActions.contains(FoldIntoPrussian.class)) {
            immediateAction = possibleActions.getType(FoldIntoPrussian.class).get(0);
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
                            "M2", StockRound_1835.PR_ID),
                    "Yes",
                    "No" );
        } else {
            // Ask if any other prePrussians should be folded
            String[] options = new String[foldables.size()];
            for (int i=0; i<=options.length; i++) {
                options[i] = foldables.get(i).getName();
            }
            currentDialog = new CheckBoxDialog (gameUIManager,
                    LocalText.getText("Select"),
                    LocalText.getText("SelectCompaniesToFold", prussian.getLongName()),
                    options);
        }
        gameUIManager.setCurrentDialog (currentDialog, action);
    }

}
