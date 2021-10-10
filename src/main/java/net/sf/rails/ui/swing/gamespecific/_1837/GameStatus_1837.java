package net.sf.rails.ui.swing.gamespecific._1837;

import java.awt.event.ActionEvent;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.Company;
import net.sf.rails.game.specific._1837.PublicCompany_1837;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.FoldIntoNational;
import rails.game.action.LayBaseToken;
import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.GameStatus;

import javax.swing.*;

/**
 * @author martin based on work by Erik Voss for 18EU
 *
 */
public class GameStatus_1837 extends GameStatus {
    private static final Logger log = LoggerFactory.getLogger(GameStatus_1837.class);

    /**
     *
     */
    public GameStatus_1837() {
    }
    private static final long serialVersionUID = 1L;

    @Override
    protected void initGameSpecificActions() {

        PublicCompany mergingCompany;
        int index;

        if (possibleActions.contains(MergeCompanies.class)) {
            List<MergeCompanies> mergers =
                    possibleActions.getType(MergeCompanies.class);
            for (MergeCompanies merger : mergers) {
                mergingCompany = merger.getMergingCompany();
                if (mergingCompany != null) {
                    index = mergingCompany.getPublicNumber();
                    setPlayerCertButton(index, merger.getPlayerIndex(), true,
                            merger);
                }
            }
            /* This could be the 'silent mode' prompt to merge a minor
        } else if (possibleActions.contains(FoldIntoNational.class)) {
            FoldIntoNational mergers =
                    possibleActions.getType(FoldIntoNational.class).get(0);
            PublicCompany_1837 national = mergers.getNationalCompany();
            for (Company merger : mergers.getFoldableCompanies()) {
                if (merger instanceof PublicCompany) {
                    PublicCompany minor = (PublicCompany) merger;
                    index = minor.getPublicNumber();
                    setPlayerCertButton(index, mergers.getPlayerIndex(), true,
                            merger);
                }
            }*/
        }
    }

    @Override
    protected void setPlayerCertButton(int i, int j, boolean clickable, Object o) {

        super.setPlayerCertButton(i, j, clickable, o);

        if (clickable && o instanceof PossibleAction) {
            if (o instanceof FoldIntoNational) {
                addToolTipText (certPerPlayerButton[i][j], LocalText.getText("MergeTooltip",
                        ((FoldIntoNational) o).getNationalCompany().getId()));

            }
        }
    }


        /** Start a company - specific procedure for 1837 copied from 18EU */
    @Override
    protected PossibleAction processGameSpecificActions(ActionEvent actor,
            PossibleAction chosenAction) {

        if (chosenAction instanceof MergeCompanies) {

            log.debug("Merge action: {}", chosenAction);

            MergeCompanies action = (MergeCompanies) chosenAction;
            PublicCompany minor = action.getMergingCompany();
            List<PublicCompany> targets = action.getTargetCompanies();

            if (minor == null || targets == null
                    || targets.size() != 1 || targets.get(0) == null) {
                log.error("Bad {}", action);
                return null;
            }

            PublicCompany major = targets.get(0);
            action.setSelectedTargetCompany(major);

            return action;

        }
        return null;
    }



}
