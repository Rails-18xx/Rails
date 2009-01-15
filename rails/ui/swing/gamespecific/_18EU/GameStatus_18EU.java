package rails.ui.swing.gamespecific._18EU;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import rails.game.City;
import rails.game.PublicCompanyI;
import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import rails.game.specific._18EU.StartCompany_18EU;
import rails.ui.swing.GameStatus;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.util.LocalText;

/**
 * This class is incorporated into StatusWindow and displays the bulk of
 * rails.game status information.
 */
public class GameStatus_18EU extends GameStatus {
    @Override
    protected void initGameSpecificActions() {

        PublicCompanyI mergingCompany;
        int index;

        List<MergeCompanies> mergers =
                possibleActions.getType(MergeCompanies.class);
        if (mergers != null) {
            for (MergeCompanies merger : mergers) {
                mergingCompany = merger.getMergingCompany();
                if (mergingCompany != null) {
                    index = mergingCompany.getPublicNumber();
                    setPlayerCertButton(index, merger.getPlayerIndex(), true,
                            merger);
                }
            }
        }

    }

    /** Start a company - specific procedure for 18EU */
    @Override
    protected PossibleAction processGameSpecificActions(ActionEvent actor,
            PossibleAction chosenAction) {

        if (chosenAction instanceof MergeCompanies) {

            log.debug("Merge action: " + chosenAction.toString());

            MergeCompanies action = (MergeCompanies) chosenAction;
            PublicCompanyI minor = action.getMergingCompany();
            List<PublicCompanyI> targets = action.getTargetCompanies();

            if (minor == null || targets == null || targets.isEmpty()) {
                log.error("Bad " + action.toString());
                return null;
            }

            String[] options = new String[targets.size()];
            int i = 0;
            for (PublicCompanyI target : targets) {
                if (target != null) {
                    options[i++] =
                            target.getName() + " " + target.getLongName();
                } else {
                    options[i++] =
                            LocalText.getText("CloseMinor", minor.getName());
                }
            }
            int choice =
                    new RadioButtonDialog(this,
                            LocalText.getText("PleaseSelect"),
                            LocalText.getText("SelectCompanyToMergeMinorInto",
                                    minor.getName()), options, -1) //
                    .getSelectedOption();
            if (choice < 0) return null;

            PublicCompanyI major = targets.get(choice);
            action.setSelectedTargetCompany(major);

            if (major != null
                    && action.canReplaceToken(choice)) {
                boolean replaceToken =
                        JOptionPane.showConfirmDialog(this, LocalText.getText(
                                    "WantToReplaceToken",
                                    minor.getName(),
                                    major.getName() ),
                                LocalText.getText("PleaseSelect"),
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                action.setReplaceToken(replaceToken);
            } else {
                return chosenAction;
            }

        }
        return chosenAction;
    }

    /** Start a company - specific procedure for 18EU */
    @Override
    protected PossibleAction processGameSpecificFollowUpActions(
            ActionEvent actor, PossibleAction chosenAction) {

        if (chosenAction instanceof StartCompany_18EU) {

            StartCompany_18EU action = (StartCompany_18EU) chosenAction;
            List<PublicCompanyI> minors = //
                    ((StartCompany_18EU) chosenAction).getMinorsToMerge();

            if (minors == null || minors.isEmpty()) {
                // Do nothing
            } else if (minors.size() == 1) {
                PublicCompanyI minor = minors.get(0);
                int answer =
                        JOptionPane.showConfirmDialog(
                                parent,
                                LocalText.getText(
                                        "MergeMinorConfirm",
                                        minor.getName(),
                                        action.getCertificate().getCompany().getName() ),
                                LocalText.getText("PleaseConfirm"),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    action.setChosenMinor(minor);
                    chosenAction = action;
                } else {
                    chosenAction = null;
                }
            } else {
                String[] options = new String[minors.size()];
                int i = 0;
                for (PublicCompanyI minor : minors) {
                    options[i++] =
                            "Minor " + minor.getName() + " "
                                    + minor.getLongName();
                }
                int choice =
                        new RadioButtonDialog(
                                this, //
                                LocalText.getText("PleaseSelect"), //
                                LocalText.getText(
                                        "SelectMinorToMerge",
                                        action.getCertificate().getCompany().getName()),
                                options, -1).getSelectedOption();
                if (choice >= 0) {
                    action.setChosenMinor(minors.get(choice));
                    chosenAction = action;
                } else {
                    chosenAction = null;
                }
            }

            List<City> cities = action.getAvailableHomeStations();
            if (cities != null && !cities.isEmpty()) {
                String[] options = new String[cities.size()];
                for (int i = 0; i < options.length; i++) {
                    options[i] = cities.get(i).toString();
                }
                int index =
                        new RadioButtonDialog(
                                this, //
                                LocalText.getText("PleaseSelect"), //
                                LocalText.getText(
                                        "SelectHomeStation", //
                                        action.getCertificate().getCompany().getName()),
                                options, -1).getSelectedOption();
                if (index >= 0) {
                    action.setHomeStation(cities.get(index));
                } else {
                    return null;
                }
            }
        }
        return chosenAction;
    }

}
