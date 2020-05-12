package net.sf.rails.ui.swing.gamespecific._18EU;

import java.awt.event.ActionEvent;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.GameStatus;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;


/**
 * This class is incorporated into StatusWindow and displays the bulk of
 * rails.game status information.
 */
public class GameStatus_18EU extends GameStatus {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GameStatus_18EU.class);


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

    /** Start a company - specific procedure for 18EU */
    @Override
    protected PossibleAction processGameSpecificActions(ActionEvent actor, PossibleAction chosenAction) {

        if (chosenAction instanceof MergeCompanies) {

            log.debug("Merge action: {}", chosenAction);

            MergeCompanies action = (MergeCompanies) chosenAction;
            PublicCompany minor = action.getMergingCompany();
            List<PublicCompany> targets = action.getTargetCompanies();

            if (minor == null || targets == null || targets.isEmpty()) {
                log.error("Bad {}", action);
                return null;
            }

            String[] options = new String[targets.size()];
            int i = 0;
            for (PublicCompany target : targets) {
                if (target != null) {
                    options[i++] = target.getId() + " " + target.getLongName();
                } else {
                    options[i++] = LocalText.getText("CloseMinor", minor.getId());
                }
            }

            RadioButtonDialog dialog = new RadioButtonDialog (
                    GameUIManager_18EU.SELECT_MERGING_MAJOR,
                    gameUIManager,
                    parent,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText("SelectCompanyToMergeMinorInto", minor.getId()),
                    options, -1);
            gameUIManager.setCurrentDialog(dialog, action);
            parent.disableButtons();

        }
        return null;
    }


}
