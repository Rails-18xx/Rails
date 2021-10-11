package net.sf.rails.ui.swing.gamespecific._1837;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.DiscardTrain;
import rails.game.action.FoldIntoNational;
import rails.game.action.MergeCompanies;
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
    public static final String START_NATIONAL_DIALOG = "StartNational";
    public static final String MERGE_INTO_NATIONAL_DIALOG = "MergeIntoNational";
    public static final String SELECT_CONVERTING_MINOR = "SelectConvertingMinor";
    public static final String SELECT_MERGING_MAJOR = "SelectMergingMajor"; // Not used??
    public static final String SELECT_MERGING_MINOR = "SelectMergingMinor";

    private static final Logger log = LoggerFactory.getLogger(GameUIManager_1837.class);

    public GameUIManager_1837() {
    }

    @Override
    public void updateUI() {
        super.updateUI();
    }


    @Override
    public void dialogActionPerformed() {

        String key = "";
        if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog)currentDialog).getKey();

        // Check for the dialogs that are postprocessed in this class.
        if ("MergeCoalCompany".equalsIgnoreCase(key)) {

            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            MergeCompanies action = (MergeCompanies) currentDialogAction;
            if (dialog.getAnswer()) {
                // Always only one target present under the current 1837 rules
                action.setSelectedTargetCompany(action.getTargetCompanies().get(0));
            }

        } else if (START_NATIONAL_DIALOG.equals(key)) {

            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            FoldIntoNational action = (FoldIntoNational) currentDialogAction;
            if (dialog.getAnswer()) {
                action.setFoldedCompanies(action.getFoldableCompanies());
            }

        } else if (MERGE_INTO_NATIONAL_DIALOG.equals(key)) {

            CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
            FoldIntoNational action = (FoldIntoNational) currentDialogAction;
            boolean[] exchanged = dialog.getSelectedOptions();
            String[] options = dialog.getOptions();

            List<Company> foldedCompanies = new ArrayList<>();
            for (int index=0; index < options.length; index++) {
                if (exchanged[index]) {
                    foldedCompanies.add(action.getFoldableCompanies().get(index));
                }
            }
            action.setFoldedCompanies(foldedCompanies);

        } else if (SELECT_MERGING_MINOR.equals(key)) {

            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            MergeCompanies action = (MergeCompanies) currentDialogAction;
            PublicCompany minor = action.getMergingCompany();
            PublicCompany major = action.getTargetCompanies().get(0); // Always one

            if (!dialog.getAnswer()) return; // Answer is "no"

            action.setSelectedTargetCompany(major);
            if (major != null && action.canReplaceToken(0)) {

                boolean replaceToken =
                        JOptionPane.showConfirmDialog(statusWindow, LocalText.getText(
                                "WantToReplaceToken",
                                minor.getId(),
                                major.getId()),
                                LocalText.getText("PleaseSelect"),
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                action.setReplaceToken(replaceToken);
            }

        } else if (SELECT_MERGING_MAJOR.equals(key)) {

            // *** Should no longer be used
            log.warn("Not expected here: {}", SELECT_MERGING_MAJOR);

            // A major company has been selected (or not) to merge a minor into.
            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            MergeCompanies action = (MergeCompanies) currentDialogAction;
            PublicCompany minor = action.getMergingCompany();

            int choice = dialog.getSelectedOption();
            if (choice < 0) return;

            PublicCompany major = action.getTargetCompanies().get(choice);
            action.setSelectedTargetCompany(major);

        } else if (SELECT_CONVERTING_MINOR.equals(key)) {

            // **** No longer used
            log.warn("Not expected here: {}", SELECT_CONVERTING_MINOR);

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

    /* (non-Javadoc)
     * @see net.sf.rails.ui.swing.GameUIManager#discardTrains(rails.game.action.DiscardTrain)
     */
    @Override
    public void discardTrains(DiscardTrain dt) {
        // TODO Auto-generated method stub
        super.discardTrains(dt);
    }
    
}

