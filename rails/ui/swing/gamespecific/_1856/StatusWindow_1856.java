package rails.ui.swing.gamespecific._1856;

import javax.swing.WindowConstants;

import rails.game.*;
import rails.game.action.*;
import rails.game.specific._1856.CGRFormationRound;
import rails.ui.swing.StatusWindow;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.util.LocalText;
import rails.util.Util;

public class StatusWindow_1856 extends StatusWindow {

    private static final long serialVersionUID = 1L;

    public StatusWindow_1856() {
        super();
    }

    @Override
    public void updateStatus(boolean myTurn) {
        RoundI currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof CGRFormationRound)) {
            super.updateStatus(myTurn);
        } else if (possibleActions.contains(RepayLoans.class)) {
            //RepayLoans action = possibleActions.getType(RepayLoans.class).get(0);
            //repayLoans (action);
            immediateAction = possibleActions.getType(RepayLoans.class).get(0);
        // Moved up
        } else if (possibleActions.contains(DiscardTrain.class)) {
            immediateAction = possibleActions.getType(DiscardTrain.class).get(0);
        } else if (possibleActions.contains(ExchangeTokens.class)) {
            immediateAction = possibleActions.getType(ExchangeTokens.class).get(0);
        }
    }


    @Override
    public boolean processImmediateAction() {

        if (immediateAction == null) {
            return false;
        } else if (immediateAction instanceof RepayLoans) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            RepayLoans nextAction = (RepayLoans) immediateAction;
            immediateAction = null;
            repayLoans (nextAction);
            return true;
           
            /* Moved up
        } else if (immediateAction instanceof DiscardTrain) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            DiscardTrain nextAction = (DiscardTrain) immediateAction;
            immediateAction = null;
            gameUIManager.discardTrains (nextAction);
            return true;
            */
        } else if (immediateAction instanceof ExchangeTokens) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            ExchangeTokens nextAction = (ExchangeTokens) immediateAction;
            immediateAction = null;
            gameUIManager.exchangeTokens (nextAction);
            return true;
        } else {
            return super.processImmediateAction();
        }
    }

    // Code partly copied from ORUIManager
    protected void repayLoans (RepayLoans action) {

        int minNumber = action.getMinNumber();
        int maxNumber = action.getMaxNumber();
        int loanAmount = action.getPrice();

        String[] options = new String[maxNumber-minNumber+1];
        for (int i=minNumber; i<=maxNumber; i++) {
            if (i == 0) {
                options[i] = LocalText.getText("None");
            } else {
                options[i] = LocalText.getText("RepayLoan",
                        i,
                        Bank.format(loanAmount),
                        Bank.format(i * loanAmount));
            }
        }

        String message = LocalText.getText("SelectLoansToRepay",
                action.getCompanyName());
        String[] waitingMessages = DisplayBuffer.get();
        if (waitingMessages != null) {
            message = "<html>" + Util.joinWithDelimiter(waitingMessages, "<br>")
                + "<br>" + message;
        }

       RadioButtonDialog currentDialog = new RadioButtonDialog (gameUIManager,
                this,
                LocalText.getText("1856MergerDialog", action.getCompanyName()),
                message,
                options,
                0);
       currentDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
       gameUIManager.setCurrentDialog (currentDialog, action);
    }

}
