package rails.game.correct;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.MoneyOwner;

public class CashCorrectionManager extends CorrectionManager {

    private CashCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_CASH);
    }

    public static CashCorrectionManager create(GameManager parent) {
        return new CashCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        // Keep existing logic for GameStatus buttons
        List<CorrectionAction> actions = super.createCorrections();

        if (isActive()) {
            List<Player> players = getRoot().getPlayerManager().getPlayers();
            for(Player pl:players){
                actions.add(new CashCorrectionAction(getRoot(), pl));
            }

            List<PublicCompany> publicCompanies = getParent().getAllPublicCompanies();
            for(PublicCompany pc:publicCompanies){
                if (pc.hasFloated() && !pc.isClosed())
                    actions.add(new CashCorrectionAction(pc));
            }
        }

        return actions;
    }



// ... (lines of unchanged context code) ...
    @Override
    public boolean executeCorrection(CorrectionAction action) {
        // 1. Logging Trace
        log.info("DEBUG: CashCorrectionManager executeCorrection action=" + action);

        // 2. Intercept Menu Click (CorrectionModeAction)
        if (action instanceof CorrectionModeAction) {
            // --- START FIX ---
            // Delegate to the base class natively to toggle the internal active state boolean.
            // This allows the matrix fields to turn on and become clickable.
            return super.executeCorrection(action);
            // --- END FIX ---
        }

        // 3. Handle Direct Action (e.g. from GameStatus button click, Replay, or Wizard completion)
        if (action instanceof CashCorrectionAction) {
            CashCorrectionAction cca = (CashCorrectionAction) action;

            // FIX: Check isReloading. 
            if (cca.getAmount() == 0 && !getParent().isReloading()) {
                log.info("DEBUG: Intercepted 0-amount CashCorrection. Opening Dialog.");
                // --- START FIX ---
                javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 10));
                panel.add(new javax.swing.JLabel(LocalText.getText("CorrectCashDialogMessage", cca.getCashHolder().getId())));
                
                javax.swing.JRadioButton btnReceive = new javax.swing.JRadioButton("Receives Money (+)", true);
                javax.swing.JRadioButton btnPay = new javax.swing.JRadioButton("Pays Money (-)");
                javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
                bg.add(btnReceive);
                bg.add(btnPay);
                
                javax.swing.JPanel radioPanel = new javax.swing.JPanel(new java.awt.GridLayout(2, 1));
                radioPanel.add(btnReceive);
                radioPanel.add(btnPay);
                panel.add(radioPanel);
                
                javax.swing.JTextField amountField = new javax.swing.JTextField(10);
                javax.swing.JPanel inputPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
                
                // Fetch current currency symbol dynamically via Bank formatting
                String currencySymbol = net.sf.rails.game.financial.Bank.format(this, 0).replace("0", "").trim();
                inputPanel.add(new javax.swing.JLabel("Amount: " + currencySymbol + " "));
                inputPanel.add(amountField);
                panel.add(inputPanel);
                
                int result = javax.swing.JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    LocalText.getText("CorrectCashDialogTitle"),
                    javax.swing.JOptionPane.OK_CANCEL_OPTION,
                    javax.swing.JOptionPane.PLAIN_MESSAGE
                );

                if (result != javax.swing.JOptionPane.OK_OPTION) return false; 

                String input = amountField.getText();
                if (input == null || input.trim().isEmpty()) return false;

                try {
                    int val = Integer.parseInt(input.trim());
                    if (val == 0) return false;
                    if (btnPay.isSelected()) {
                        val = -val; // Convert to negative debt if Pays Money is checked
                    }
                    cca.setAmount(val);
                } catch (NumberFormatException e) {
                    DisplayBuffer.add(this, "Invalid Amount");
                    return false;
                }
                // --- END FIX ---
            }
            
            // --- START FIX ---
            boolean success = execute(cca);
            if (success && isActive() && !getParent().isReloading()) {
                // Auto-turn off correction mode after a successful adjustment to restore the matrix layout
                CorrectionModeAction deactivateAction = new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_CASH, false);
                super.executeCorrection(deactivateAction);
            }
            return success;
            // --- END FIX ---
        }
        
        return super.executeCorrection(action);
    }
// ... (rest of the file) ...



    private boolean execute(CashCorrectionAction cashAction) {

        boolean result = false;

        MoneyOwner ch = cashAction.getCashHolder();
        int amount = cashAction.getAmount();

        String errMsg = null;

        while (true) {
            if (amount == 0 ) {
                errMsg =
                    LocalText.getText("CorrectCashZero");
                break;
            }
            if ((amount + ch.getCash()) < 0) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            ch.getId(),
                            Bank.format(this, ch.getCash()),
                            Bank.format(this, -amount)
                    );
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CorrectCashError",
                    ch.getId(),
                    errMsg));
            result = true;
        } else {
            // no error occured

            String msg;
            if (amount < 0) {
                // negative amounts: remove cash from cashholder
                String text = Currency.toBank(ch, -amount);

                msg = LocalText.getText("CorrectCashSubstractMoney",
                        ch.getId(),
                        text );
            } else {
                // positive amounts: add cash to cashholder
                String text = Currency.fromBank(amount, ch);
                msg = LocalText.getText("CorrectCashAddMoney",
                        ch.getId(),
                        text);
            }
            ReportBuffer.add(this, msg);
            DisplayBuffer.add(this, msg);
            result = true;

            // Force UI Refresh
            if (getParent().getGameUIManager() != null) {
                getParent().getGameUIManager().forceFullUIRefresh();
            }

            // Force UI Refresh specifically for Status Window
        if (getParent().getGameUIManager() != null) {
            getParent().getGameUIManager().forceFullUIRefresh();
            
            if (getParent().getGameUIManager().getStatusWindow() != null) {
                getParent().getGameUIManager().getStatusWindow().repaint();
            }
        }
        }

       return result;
    }

}