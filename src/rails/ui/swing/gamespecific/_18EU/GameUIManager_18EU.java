package rails.ui.swing.gamespecific._18EU;

import java.util.List;

import javax.swing.JOptionPane;

import rails.common.LocalText;
import rails.game.PublicCompany;
import rails.game.Stop;
import rails.game.action.MergeCompanies;
import rails.game.specific._18EU.StartCompany_18EU;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.elements.NonModalDialog;
import rails.ui.swing.elements.RadioButtonDialog;

public class GameUIManager_18EU extends GameUIManager {

    // Keys of dialogs owned by this class.
    public static final String SELECT_CONVERTING_MINOR = "SelectConvertingMinor";
    public static final String SELECT_MERGING_MAJOR = "SelectMergingMajor";
    public static final String SELECT_HOME_STATION_DIALOG = "SelectHomeStation";

    @Override
    public void dialogActionPerformed () {

        String key = "";
        if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog)currentDialog).getKey();

        // Check for the dialogs that are postprocessed in this class.
        if (SELECT_MERGING_MAJOR.equals(key)) {

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

        } else if (COMPANY_START_PRICE_DIALOG.equals(key)
                && currentDialogAction instanceof StartCompany_18EU) {

            // A start price has been selected (or not) for a stating major company.
            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_18EU action = (StartCompany_18EU) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }
            action.setStartPrice(action.getStartPrices()[index]);

            // Set up another dialog for the next step
            List<PublicCompany> minors = action.getMinorsToMerge();

            if (minors != null && !minors.isEmpty()) {
                // Up to phase 6, a minor must be exchanged
                String[] options = new String[minors.size()];
                int i = 0;
                    for (PublicCompany minor : minors) {
                    options[i++] =
                        "Minor " + minor.getId() + " "
                        + minor.getLongName();
                }
                dialog = new RadioButtonDialog (SELECT_CONVERTING_MINOR,
                        this,
                        statusWindow,
                        LocalText.getText("PleaseSelect"),
                        LocalText.getText(
                                "SelectMinorToMerge",
                                action.getCompanyName()),
                                options, -1);
                setCurrentDialog(dialog, action);
                statusWindow.disableButtons();
                return;
            } else {

                // From phase 6, no minors are involved, but a home station must be chosen
                List<Stop> cities = action.getAvailableHomeStations();
                if (cities != null && !cities.isEmpty()) {
                    String[] options = new String[cities.size()];
                    for (int i = 0; i < options.length; i++) {
                        options[i] = cities.get(i).toString();
                    }
                    dialog = new RadioButtonDialog (SELECT_HOME_STATION_DIALOG,
                            this,
                            statusWindow,
                            LocalText.getText("PleaseSelect"),
                            LocalText.getText(
                                    "SelectHomeStation",
                                    action.getCompanyName()),
                                    options, -1);
                    setCurrentDialog(dialog, action);
                    statusWindow.disableButtons();
                    return;
                }
            }

        } else if (SELECT_HOME_STATION_DIALOG.equals(key)) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_18EU action = (StartCompany_18EU) currentDialogAction;

            if (action.getAvailableHomeStations() != null) {
                // From phase 6: a home station has been selected (or not)

                int index = dialog.getSelectedOption();
                if (index < 0) {
                    currentDialogAction = null;
                    return;
                }
                action.setHomeStation(action.getAvailableHomeStations().get(index));
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
