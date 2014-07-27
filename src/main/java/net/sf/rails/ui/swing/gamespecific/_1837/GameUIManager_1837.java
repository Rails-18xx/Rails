package net.sf.rails.ui.swing.gamespecific._1837;

import java.util.ArrayList;
import java.util.List;

import rails.game.specific._1837.FoldIntoHungary;
import rails.game.specific._1837.FoldIntoKuK;
import rails.game.specific._1837.FoldIntoSuedbahn;
import net.sf.rails.game.Company;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.elements.CheckBoxDialog;
import net.sf.rails.ui.swing.elements.ConfirmationDialog;
import net.sf.rails.ui.swing.elements.NonModalDialog;

public class GameUIManager_1837 extends GameUIManager {
    // Keys of dialogs owned by this class.
    public static final String START_KUK_DIALOG = "StartKuK";
    public static final String MERGE_INTO_KUK_DIALOG = "MergeIntoKuK";
    public static final String START_HUNGARY_DIALOG = "StartHungary";
    public static final String MERGE_INTO_HUNGARY_DIALOG = "MergeIntoHungary";
    public static final String START_S5_DIALOG = "StartS5";


    public GameUIManager_1837() {
        // TODO Auto-generated constructor stub
    }
    /* (non-Javadoc)
     * @see net.sf.rails.ui.swing.GameUIManager#updateUI()
     */
    @Override
    public void updateUI() {
        // TODO Auto-generated method stub
        super.updateUI();
    }

  
    @Override
    public void dialogActionPerformed() {

        String key = "";
        if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog)currentDialog).getKey();

        // Check for the dialogs that are postprocessed in this class.
        if (START_HUNGARY_DIALOG.equals(key)) {

            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            FoldIntoHungary action = (FoldIntoHungary) currentDialogAction;
            if (dialog.getAnswer()) {
                action.setFoldedCompanies(action.getFoldableCompanies());
            }

        } else if (MERGE_INTO_HUNGARY_DIALOG.equals(key)) {

            CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
            FoldIntoHungary action = (FoldIntoHungary) currentDialogAction;
            boolean[] exchanged = dialog.getSelectedOptions();
            String[] options = dialog.getOptions();

            List<Company> foldedCompanies = new ArrayList<Company>();
            for (int index=0; index < options.length; index++) {
                if (exchanged[index]) {
                    foldedCompanies.add(action.getFoldableCompanies().get(index));
                }
            }
            action.setFoldedCompanies(foldedCompanies);

        } else 
            if (START_KUK_DIALOG.equals(key)) {

                ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
                FoldIntoKuK action = (FoldIntoKuK) currentDialogAction;
                if (dialog.getAnswer()) {
                    action.setFoldedCompanies(action.getFoldableCompanies());
                }

            } else if (MERGE_INTO_KUK_DIALOG.equals(key)) {

                CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
                FoldIntoKuK action = (FoldIntoKuK) currentDialogAction;
                boolean[] exchanged = dialog.getSelectedOptions();
                String[] options = dialog.getOptions();

                List<Company> foldedCompanies = new ArrayList<Company>();
                for (int index=0; index < options.length; index++) {
                    if (exchanged[index]) {
                        foldedCompanies.add(action.getFoldableCompanies().get(index));
                    }
                }
                action.setFoldedCompanies(foldedCompanies);
            }
            else {
            
            // Current dialog not found yet, try the superclass.
            super.dialogActionPerformed(false);
            return;
        }

        // Dialog action found and processed, let the superclass initiate processing.
        super.dialogActionPerformed(true);
    }
}

