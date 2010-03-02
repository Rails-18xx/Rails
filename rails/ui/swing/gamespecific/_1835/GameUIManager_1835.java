package rails.ui.swing.gamespecific._1835;

import java.util.ArrayList;
import java.util.List;

import rails.game.CompanyI;
import rails.game.specific._1835.FoldIntoPrussian;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.elements.CheckBoxDialog;
import rails.ui.swing.elements.ConfirmationDialog;

public class GameUIManager_1835 extends GameUIManager {

    protected boolean checkGameSpecificDialogAction() {
        
        if (currentDialog instanceof ConfirmationDialog
                && currentDialogAction instanceof FoldIntoPrussian) {
            
            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            FoldIntoPrussian action = (FoldIntoPrussian) currentDialogAction;
            if (dialog.getAnswer()) {
                action.setFoldedCompanies(action.getFoldableCompanies());
            }
            
            return true;
            
        } else if (currentDialog instanceof CheckBoxDialog
                && currentDialogAction instanceof FoldIntoPrussian) {
            
            CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
            FoldIntoPrussian action = (FoldIntoPrussian) currentDialogAction;
            boolean[] exchanged = dialog.getSelectedOptions();
            String[] options = dialog.getOptions();
            
            List<CompanyI> foldedCompanies = new ArrayList<CompanyI>();
            for (int index=0; index < options.length; index++) {
                if (exchanged[index]) {
                    foldedCompanies.add(action.getFoldableCompanies().get(index));
               }
            }
            action.setFoldedCompanies(foldedCompanies);
            
            return true;
            
        } else {
            return false;
        }
    }

}
