package net.sf.rails.ui.swing.gamespecific._1856;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.specific._1856.CGRFormationRound;
import net.sf.rails.game.specific._1856.PublicCompany_1856;
import net.sf.rails.ui.swing.GameStatus;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.StatusWindow;
import net.sf.rails.ui.swing.elements.Caption;
import net.sf.rails.ui.swing.elements.Field;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;
import net.sf.rails.util.Util;
import rails.game.action.DiscardTrain;
import rails.game.action.ExchangeTokens;
import rails.game.action.RepayLoans;

public class StatusWindow_1856 extends StatusWindow {

    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(StatusWindow_1856.class);

    public StatusWindow_1856() {
        super();
    }

    @Override
    protected GameStatus createGameStatus() {
        return new GameStatus_1856();
    }

    @Override
    public void updateStatus(boolean myTurn) {
        RoundFacade currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof CGRFormationRound)) {
            super.updateStatus(myTurn);
        } else if (possibleActions.contains(RepayLoans.class)) {
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
            RepayLoans nextAction = (RepayLoans) immediateAction;
            immediateAction = null;
            repayLoans(nextAction);
            return true;
        } else if (immediateAction instanceof ExchangeTokens) {
            ExchangeTokens nextAction = (ExchangeTokens) immediateAction;
            immediateAction = null;
            gameUIManager.exchangeTokens(nextAction);
            return true;
        } else {
            return super.processImmediateAction();
        }
    }

    protected void repayLoans(RepayLoans action) {
        int minNumber = action.getMinNumber();
        int maxNumber = action.getMaxNumber();
        int loanAmount = action.getPrice();

        String[] options = new String[maxNumber - minNumber + 1];
        for (int i = minNumber; i <= maxNumber; i++) {
            if (i == 0) {
                options[i] = LocalText.getText("None");
            } else {
                options[i] = LocalText.getText("RepayLoan",
                        i,
                        gameUIManager.format(loanAmount),
                        gameUIManager.format(i * loanAmount));
            }
        }

        String message = LocalText.getText("SelectLoansToRepay", action.getCompanyName());
        String[] waitingMessages = gameUIManager.getDisplayBuffer().get();
        if (waitingMessages != null) {
            message = "<html>" + Util.join(waitingMessages, "<br>") + "<br>" + message;
        }

        RadioButtonDialog currentDialog = new RadioButtonDialog(GameUIManager.REPAY_LOANS_DIALOG,
                gameUIManager,
                this,
                LocalText.getText("1856MergerDialog", action.getCompanyName()),
                message,
                options,
                0);
        currentDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        gameUIManager.setCurrentDialog(currentDialog, action);
        disableButtons();
    }

    protected class GameStatus_1856 extends GameStatus {
        private static final long serialVersionUID = 1L;

        protected Field[] compEscrow;
        protected Field[] compLoans;
        protected int escrowX;
        protected int loansX;

        @Override
        protected void initFields() {
            super.initFields();

            // Append 1856-specific columns
            int col = rightCompCaptionXOffset + 1;
            escrowX = col++;
            loansX = col++;

            Caption hEscrow = new Caption("Escrow");
            hEscrow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY));
            addField(hEscrow, escrowX, 1, 1, 1, 0, true);

            Caption hLoans = new Caption("Loans");
            hLoans.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY));
            addField(hLoans, loansX, 1, 1, 1, 0, true);

            compEscrow = new Field[nc];
            compLoans = new Field[nc];

            for (int i = 0; i < nc; i++) {
                PublicCompany c = companies[i];
                if (c.isClosed()) continue;

                Integer y = companyCertRow.get(c);
                if (y == null) continue;

                // Create fields, default to opaque so background shows
                compEscrow[i] = new Field("0");
                compEscrow[i].setOpaque(true);
                addField(compEscrow[i], escrowX, y, 1, 1, 0, true);

                compLoans[i] = new Field("0");
                compLoans[i].setOpaque(true);
                addField(compLoans[i], loansX, y, 1, 1, 0, true);
            }
        }

        @Override
        public void initTurn(int actorIndex, boolean myTurn) {
            // Run standard update first
            super.initTurn(actorIndex, myTurn);


            // Identify Operating Company for highlighting
            PublicCompany operatingComp = null;
            if (gameUIManager.getGameManager().getCurrentRound() instanceof net.sf.rails.game.OperatingRound) {
                operatingComp = ((net.sf.rails.game.OperatingRound) gameUIManager.getGameManager().getCurrentRound()).getOperatingCompany();
            } else if (gameUIManager.getGameManager().getCurrentRound() instanceof CGRFormationRound) {
                for (PublicCompany c : companies) {
                    if (c.getId().equals("CGR")) operatingComp = c;
                }
            }

            for (int i = 0; i < nc; i++) {
                PublicCompany c = companies[i];
                if (c.isClosed()) continue;

                // Color Logic
                boolean isOperating = (operatingComp != null && c == operatingComp);
                boolean hasOwner = c.getPresidentsShare() != null && c.getPresidentsShare().getOwner() instanceof net.sf.rails.game.Player;
                boolean isActive = c.hasFloated() || hasOwner;
                boolean isMinor = !c.hasStockPrice();

                Color bgRow;
                if (isOperating) {
                    bgRow = new Color(255, 255, 200); // Yellow
                } else if (!isActive) {
                    bgRow = new Color(235, 235, 235); // gray
                } else if (isMinor) {
                    bgRow = new Color(235, 235, 235); // gray
                } else {
                    bgRow = new Color(235, 230, 255); // Mauve
                }

                // 1. Set Backgrounds (Always to prevent white cells)
                if (compEscrow[i] != null) compEscrow[i].setBackground(bgRow);
                if (compLoans[i] != null) compLoans[i].setBackground(bgRow);

                if (c instanceof PublicCompany_1856) {
                    PublicCompany_1856 p = (PublicCompany_1856) c;

                    if (compEscrow[i] != null) {
                        compEscrow[i].setText(gameUIManager.format(p.getMoneyInEscrow()));
                    }
                    if (compLoans[i] != null) {
                        compLoans[i].setText(String.valueOf(p.getCurrentNumberOfLoans()));
                    }
                } 
                // SPECIAL CHECK: If it's the CGR but NOT an instance of PublicCompany_1856
                else if (c.getId().equals("CGR")) {
                    // If CGR is a generic PublicCompany, these methods won't exist.
                    // We default to "0" or "N/A" to indicate the UI sees it but can't read data.
                    // This confirms if the casting is the issue.
                     if (compEscrow[i] != null) compEscrow[i].setText("0"); // Placeholder
                     if (compLoans[i] != null) compLoans[i].setText("0");   // Placeholder
                }
                else {
                    // Clear text for closed/non-1856 companies
                    if (compEscrow[i] != null) compEscrow[i].setText("");
                    if (compLoans[i] != null) compLoans[i].setText("");
                }

            }
        }
    }
}