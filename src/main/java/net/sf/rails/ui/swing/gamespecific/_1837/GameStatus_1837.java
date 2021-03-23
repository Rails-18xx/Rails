package net.sf.rails.ui.swing.gamespecific._1837;

import java.awt.event.ActionEvent;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            if (minor == null || targets == null || targets.isEmpty()
                    || targets.size() > 1 || targets.get(0) == null) {
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
