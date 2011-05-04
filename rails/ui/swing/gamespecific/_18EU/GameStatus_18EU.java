package rails.ui.swing.gamespecific._18EU;

import java.awt.event.ActionEvent;
import java.util.List;

import rails.game.PublicCompanyI;
import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import rails.ui.swing.GameStatus;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.util.LocalText;

/**
 * This class is incorporated into StatusWindow and displays the bulk of
 * rails.game status information.
 */
public class GameStatus_18EU extends GameStatus {

    private static final long serialVersionUID = 1L;

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

            RadioButtonDialog dialog = new RadioButtonDialog (gameUIManager,
                    parent,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText("SelectCompanyToMergeMinorInto",
                            minor.getName()),
                            options, -1);
            gameUIManager.setCurrentDialog(dialog, action);
        }
        return null;
    }


}
