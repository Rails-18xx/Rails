package net.sf.rails.ui.swing.gamespecific._1837;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import rails.game.action.MergeCompanies;
import rails.game.specific._1837.FoldIntoHungary;
import rails.game.specific._1837.FoldIntoKuK;
import rails.game.specific._18EU.StartCompany_18EU;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.Company;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.elements.CheckBoxDialog;
import net.sf.rails.ui.swing.elements.ConfirmationDialog;
import net.sf.rails.ui.swing.elements.NonModalDialog;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;

public class GameUIManager_1837 extends GameUIManager {
    // Keys of dialogs owned by this class.
    public static final String START_KUK_DIALOG = "StartKuK";
    public static final String MERGE_INTO_KUK_DIALOG = "MergeIntoKuK";
    public static final String START_HUNGARY_DIALOG = "StartHungary";
    public static final String MERGE_INTO_HUNGARY_DIALOG = "MergeIntoHungary";
    public static final String SELECT_CONVERTING_MINOR = "SelectConvertingMinor";
    public static final String SELECT_MERGING_MAJOR = "SelectMergingMajor";



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
            else if (SELECT_MERGING_MAJOR.equals(key)) {

                // A major company has been selected (or not) to merge a minor into.
                RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                MergeCompanies action = (MergeCompanies) currentDialogAction;
                PublicCompany minor = action.getMergingCompany();

                int choice = dialog.getSelectedOption();
                if (choice < 0) return;

                PublicCompany major = action.getTargetCompanies().get(choice);
                action.setSelectedTargetCompany(major);

                if (major != null && action.canReplaceToken(choice)) {

                    boolean replaceToken =
                            JOptionPane.showConfirmDialog(statusWindow, LocalText.getText(
                                    "WantToReplaceToken",
                                    minor.getId(),
                                    major.getId() ),
                                    LocalText.getText("PleaseSelect"),
                                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                    action.setReplaceToken(replaceToken);
                }

            } else if (SELECT_CONVERTING_MINOR.equals(key)) {

                // A minor has been selected (or not) to merge into a starting company before phase 6.
                RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                StartCompany_18EU action = (StartCompany_18EU) currentDialogAction;
                int choice = dialog.getSelectedOption();
                if (choice < 0) {
                    currentDialogAction = null;
                } else {
                    PublicCompany minor = action.getMinorsToMerge().get(choice);
                    action.setChosenMinor(minor);
                }

            } else {
                // Current dialog not found yet, try the superclass.
                super.dialogActionPerformed(false);
                return;
            }

        // Dialog action found and processed, let the superclass initiate processing.
        super.dialogActionPerformed(true);
    }
}

