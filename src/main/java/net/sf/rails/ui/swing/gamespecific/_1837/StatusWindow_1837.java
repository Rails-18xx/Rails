package net.sf.rails.ui.swing.gamespecific._1837;

import java.util.List;

import net.sf.rails.game.specific._1837.CoalExchangeRound;
import net.sf.rails.game.specific._1837.PublicCompany_1837;
import rails.game.action.DiscardTrain;
import rails.game.action.FoldIntoNational;

import com.google.common.collect.Iterables;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.Company;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.specific._1837.NationalFormationRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.StatusWindow;
import net.sf.rails.ui.swing.elements.CheckBoxDialog;
import net.sf.rails.ui.swing.elements.ConfirmationDialog;
import net.sf.rails.ui.swing.elements.NonModalDialog;


/**
 * @author martin
 * @date 27.07.2014 @time 12:49:01
 */
public class StatusWindow_1837 extends StatusWindow {

        private PublicCompany suedbahn;
        private PublicCompany kuk;
        private PublicCompany hungary;

        private static final long serialVersionUID = 1L;

        public StatusWindow_1837() {
            super();
        }

        @Override
        public void init (GameUIManager gameUIManager) {
            super.init(gameUIManager);
            suedbahn = gameUIManager.getRoot().getCompanyManager().getPublicCompany("Sd");
            kuk = gameUIManager.getRoot().getCompanyManager().getPublicCompany("KK");
            hungary = gameUIManager.getRoot().getCompanyManager().getPublicCompany("Ug");
        }

        @Override
        public void updateStatus(boolean myTurn) {
            RoundFacade currentRound = gameUIManager.getCurrentRound();
            if (currentRound instanceof CoalExchangeRound)  {
                // Not sure why the UI reset is otherwise bypassed.
                // Somehow the UI update flow in this round type is different
                gameStatus.initTurn(-1, true);
            }
            if (! (currentRound instanceof NationalFormationRound)) {
                super.updateStatus(myTurn);
            } else if (possibleActions.contains(FoldIntoNational.class)) {
                immediateAction = possibleActions.getType(FoldIntoNational.class).get(0);
                /*
            } else if (possibleActions.contains(FoldIntoKuK.class)) {
                immediateAction = possibleActions.getType(FoldIntoKuK.class).get(0);
            } else if (possibleActions.contains(FoldIntoSuedbahn.class)) {
                immediateAction = possibleActions.getType(FoldIntoSuedbahn.class).get(0);
            */} else if (possibleActions.contains(DiscardTrain.class)) {
                immediateAction = possibleActions.getType(DiscardTrain.class).get(0);
            }
        }


        @Override
        public boolean processImmediateAction() {

            if (immediateAction == null) {
                return false;
            } else if (immediateAction instanceof FoldIntoNational) {
                // Make a local copy and discard the original,
                // so that it's not going to loop.
                FoldIntoNational nextAction = (FoldIntoNational) immediateAction;
                immediateAction = null;
                fold (nextAction);
                return true;
            } else {
                return super.processImmediateAction();
            }
        }

        // Code partly copied from ORUIManager
        protected void fold (FoldIntoNational action) {

            PublicCompany_1837 national = action.getNationalCompany();
            PublicCompany_1837 startingMinor = national.getStartingMinor();
            List<Company> foldables = action.getFoldableCompanies();
            NonModalDialog currentDialog;

            if (foldables.get(0).equals(national.getStartingMinor())) {
                currentDialog = new ConfirmationDialog (GameUIManager_1837.START_NATIONAL_DIALOG,
                        gameUIManager, this,
                        LocalText.getText("Select"),
                        LocalText.getText("MergeMinorConfirm",
                                getCurrentPlayer().getId(),
                                startingMinor.getId(), national.getId()),
                        "Yes",
                        "No");
            } else {
                // Ask if any other preNationals should be folded
                String[] options = new String[foldables.size()];
                PublicCompany company;
                for (int i=0; i<options.length; i++) {
                    company = (PublicCompany)foldables.get(i); // TODO: in 1835 also privates are involved
                    options[i] = LocalText.getText("MergeOption",
                            company.getId(),
                            company.getLongName(),
                            national.getId(),
                            national.getShareUnit()   /// TODO Check this
                            //((ExchangeForShare)(Iterables.get(company.getSpecialProperties(),0))).getShare()
                    );
                }
                currentDialog = new CheckBoxDialog (GameUIManager_1837.MERGE_INTO_NATIONAL_DIALOG,
                        gameUIManager,
                        this,
                        LocalText.getText("Select"),
                        LocalText.getText("SelectCompaniesToFold",
                                getCurrentPlayer().getId(),
                                national.getLongName()),
                                options);
            }
            gameUIManager.setCurrentDialog (currentDialog, action);
            disableButtons();
        }

    }

