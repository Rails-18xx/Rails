/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/gamespecific/_18EU/StatusWindow_18EU.java,v 1.6 2009/09/11 19:26:44 evos Exp $*/
package rails.ui.swing.gamespecific._18EU;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import rails.game.PublicCompanyI;
import rails.game.TrainI;
import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import rails.game.specific._18EU.FinalMinorExchangeRound;
import rails.ui.swing.StatusWindow;
import rails.util.LocalText;

/**
 * This is the Window used for displaying nearly all of the rails.game status.
 * This is also from where the ORWindow and StartRoundWindow are triggered.
 */
public class StatusWindow_18EU extends StatusWindow {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean updateGameSpecificSettings() {

        if (possibleActions.contains(DiscardTrain.class)) {
            immediateAction =
                    possibleActions.getType(DiscardTrain.class).get(0);
        }

        if (currentRound instanceof FinalMinorExchangeRound) {
            setTitle(LocalText.getText("FinalMinorExchangeRoundTitle"));
            gameStatus.initTurn(getCurrentPlayer().getIndex(), true);
            return true;
        }
        return false;
    }

    @Override
    // Copied from StartRoundWindow, might become a generic function
    public boolean processImmediateAction() {

        log.debug("ImmediateAction=" + immediateAction);
        if (immediateAction != null) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            PossibleAction nextAction = immediateAction;
            immediateAction = null;
            if (nextAction instanceof DiscardTrain) {
                DiscardTrain dt = (DiscardTrain) nextAction;

                // Following code largely copied from ORUIManager
                PublicCompanyI c = dt.getCompany();
                String playerName = dt.getPlayerName();
                List<TrainI> trains = dt.getOwnedTrains();
                List<String> trainOptions =
                        new ArrayList<String>(trains.size());
                String[] options = new String[trains.size()];

                for (int i = 0; i < options.length; i++) {
                    options[i] =
                            LocalText.getText("N_Train",
                                    trains.get(i).getName());
                    trainOptions.add(options[i]);
                }
                String discardedTrainName =
                        (String) JOptionPane.showInputDialog(
                                this,
                                LocalText.getText(
                                        "HAS_TOO_MANY_TRAINS",
                                        playerName,
                                        c.getName() ),
                                LocalText.getText("WhichTrainToDiscard"),
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[0]);
                if (discardedTrainName != null) {
                    TrainI discardedTrain =
                            trains.get(trainOptions.indexOf(discardedTrainName));

                    dt.setDiscardedTrain(discardedTrain);

                    process(dt);
                }
            }
        }
        return true;

    }

}
