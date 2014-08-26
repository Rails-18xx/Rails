/**
 * 
 */
package net.sf.rails.ui.swing.gamespecific._1837;

import java.awt.event.ActionEvent;
import java.util.List;

import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.GameStatus;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;
import net.sf.rails.ui.swing.gamespecific._1837.GameUIManager_1837;

/**
 * @author martin based on work by Erik Voss for 18EU
 *
 */
public class GameStatus_1837 extends GameStatus {

    /**
     * 
     */
    public GameStatus_1837() {
        // TODO Auto-generated constructor stub
    }
    private static final long serialVersionUID = 1L;

    @Override
    protected void initGameSpecificActions() {

        PublicCompany mergingCompany;
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

    /** Start a company - specific procedure for 1837 copied from 18EU */
    @Override
    protected PossibleAction processGameSpecificActions(ActionEvent actor,
            PossibleAction chosenAction) {

        if (chosenAction instanceof MergeCompanies) {

            log.debug("Merge action: " + chosenAction.toString());

            MergeCompanies action = (MergeCompanies) chosenAction;
            PublicCompany minor = action.getMergingCompany();
            List<PublicCompany> targets = action.getTargetCompanies();

            if (minor == null || targets == null || targets.isEmpty()) {
                log.error("Bad " + action.toString());
                return null;
            }

            String[] options = new String[targets.size()];
            int i = 0;
            for (PublicCompany target : targets) {
                if (target != null) {
                    options[i++] =
                            target.getId() + " " + target.getLongName();
                } else {
                    options[i++] =
                            LocalText.getText("CloseCoal", minor.getId());
                }
            }

            RadioButtonDialog dialog = new RadioButtonDialog (
                    GameUIManager_1837.SELECT_MERGING_MAJOR,
                    gameUIManager,
                    parent,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText("SelectCompanyToMergeMinorInto",
                            minor.getId()),
                            options, -1);
            gameUIManager.setCurrentDialog(dialog, action);
            parent.disableButtons();

        }
        return null;
    }



}
