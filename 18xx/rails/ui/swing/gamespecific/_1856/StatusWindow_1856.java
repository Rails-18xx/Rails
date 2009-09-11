package rails.ui.swing.gamespecific._1856;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import rails.game.*;
import rails.game.action.*;
import rails.game.specific._1856.CGRFormationRound;
import rails.ui.swing.StatusWindow;
import rails.util.LocalText;

public class StatusWindow_1856 extends StatusWindow {

    private static final long serialVersionUID = 1L;

    public StatusWindow_1856() {
        super();
    }

    @Override
    public void updateStatus() {
        RoundI currentRound = gameUIManager.getCurrentRound();
        //if (!(currentRound instanceof OperatingRound)) {
        if (!(currentRound instanceof CGRFormationRound)) {
            super.updateStatus();
        } else if (possibleActions.contains(RepayLoans.class)) {
            //RepayLoans action = possibleActions.getType(RepayLoans.class).get(0);
            //repayLoans (action);
            immediateAction = possibleActions.getType(RepayLoans.class).get(0);
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
        } else if (immediateAction instanceof DiscardTrain) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            DiscardTrain nextAction = (DiscardTrain) immediateAction;
            immediateAction = null;
            gameUIManager.discardTrains (nextAction);
            return true;
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
        int numberRepaid = 0;

        List<String> options = new ArrayList<String>();
        for (int i=minNumber; i<=maxNumber; i++) {
            if (i == 0) {
                options.add(LocalText.getText("None"));
            } else {
                options.add(LocalText.getText("RepayLoan",
                        i,
                        Bank.format(loanAmount),
                        Bank.format(i * loanAmount)));
            }
        }
        int displayBufSize = DisplayBuffer.getSize();
        String[] message = new String[displayBufSize+1];
        System.arraycopy (DisplayBuffer.get(),0,message,0,displayBufSize);
        message[displayBufSize] = LocalText.getText("SelectLoansToRepay",
                action.getCompanyName());

        Object choice = JOptionPane.showInputDialog(this,
                message,
                LocalText.getText("Select"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                options.toArray(),
                options.get(0));

        numberRepaid = minNumber + options.indexOf(choice);

        action.setNumberTaken(numberRepaid);
        process (action);
    }
}
