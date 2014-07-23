package net.sf.rails.ui.swing.gamespecific._1835;

import java.util.ArrayList;
import java.util.List;

import rails.game.specific._1835.FoldIntoPrussian;
import net.sf.rails.game.Company;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.elements.*;


public class GameUIManager_1835 extends GameUIManager {

    // Keys of dialogs owned by this class.
    public static final String START_PRUSSIAN_DIALOG = "StartPrussian";
    public static final String MERGE_INTO_PRUSSIAN_DIALOG = "MergeIntoPrussian";

    @Override
    public void dialogActionPerformed() {

        String key = "";
        if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog)currentDialog).getKey();

        // Check for the dialogs that are postprocessed in this class.
        if (START_PRUSSIAN_DIALOG.equals(key)) {

            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            FoldIntoPrussian action = (FoldIntoPrussian) currentDialogAction;
            if (dialog.getAnswer()) {
                action.setFoldedCompanies(action.getFoldableCompanies());
            }

        } else if (MERGE_INTO_PRUSSIAN_DIALOG.equals(key)) {

            CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
            FoldIntoPrussian action = (FoldIntoPrussian) currentDialogAction;
            boolean[] exchanged = dialog.getSelectedOptions();
            String[] options = dialog.getOptions();

            List<Company> foldedCompanies = new ArrayList<Company>();
            for (int index=0; index < options.length; index++) {
                if (exchanged[index]) {
                    foldedCompanies.add(action.getFoldableCompanies().get(index));
                }
            }
            action.setFoldedCompanies(foldedCompanies);

        } else {
            // Current dialog not found yet, try the superclass.
            super.dialogActionPerformed(false);
            return;
        }

        // Dialog action found and processed, let the superclass initiate processing.
        super.dialogActionPerformed(true);
    }
}
