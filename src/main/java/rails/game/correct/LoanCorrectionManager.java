package rails.game.correct;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.specific._1817.BondsModel_1817;
import net.sf.rails.game.specific._1817.GameManager_1817;
import net.sf.rails.game.specific._1817.PublicCompany_1817;

public class LoanCorrectionManager extends CorrectionManager {

    private LoanCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_LOANS);
    }

    public static LoanCorrectionManager create(GameManager parent) {
        return new LoanCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = super.createCorrections();

        if (isActive()) {
            List<PublicCompany> publicCompanies = getParent().getAllPublicCompanies();
            for (PublicCompany pc : publicCompanies) {
                if (pc.hasFloated() && !pc.isClosed()) {
                    actions.add(new LoanCorrectionAction(pc));
                }
            }
        }

        return actions;
    }

    @Override
    public boolean executeCorrection(CorrectionAction action) {
        log.info("DEBUG: LoanCorrectionManager executeCorrection action=" + action);

        if (action instanceof CorrectionModeAction) {
            if (!isActive()) {
                if (!getParent().isReloading()) {
                    javax.swing.SwingUtilities.invokeLater(() -> runWizard());
                }
                return true; 
            }
            return super.executeCorrection(action);
        }

        if (action instanceof LoanCorrectionAction) {
            LoanCorrectionAction lca = (LoanCorrectionAction) action;

            if (lca.getAmount() == 0 && !getParent().isReloading()) {
                log.info("DEBUG: Intercepted 0-amount LoanCorrection. Opening Dialog.");
                String input = (String) JOptionPane.showInputDialog(
                    null,
                    "Enter loan/bond adjustment for " + lca.getCompany().getId() + " (e.g., +1 or -1):",
                    "Correct Loans",
                    JOptionPane.QUESTION_MESSAGE,
                    null, null, "0"
                );

                if (input == null) return false; 

                if (input.trim().startsWith("+")) input = input.trim().substring(1);

                try {
                    int val = Integer.parseInt(input.trim());
                    if (val == 0) return false;
                    lca.setAmount(val);
                } catch (NumberFormatException e) {
                    DisplayBuffer.add(this, "Invalid Amount");
                    return false;
                }
            }
            return execute(lca);
        }
        
        return super.executeCorrection(action);
    }

    private void runWizard() {
        log.info("DEBUG: Starting Loan Correction Wizard");

        List<PublicCompany> candidates = new ArrayList<>();
        List<String> names = new ArrayList<>();
        
        for (PublicCompany c : getParent().getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                candidates.add(c);
                names.add(c.getId() + " (Loans: " + c.getNumberOfBonds() + ")");
            }
        }
        
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No valid companies available for loan correction.");
            return;
        }

        String selectedName = (String) JOptionPane.showInputDialog(
            null, 
            "Select Company to Correct:", 
            "Loan Correction Wizard",
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            names.toArray(), 
            names.get(0)
        );
        
        if (selectedName == null) return;
        
        int index = names.indexOf(selectedName);
        PublicCompany selectedCompany = candidates.get(index);
        
        String input = (String) JOptionPane.showInputDialog(
            null,
            "Enter loan adjustment for " + selectedCompany.getId() + " (Current: " + selectedCompany.getNumberOfBonds() + "):",
            "Loan Correction",
            JOptionPane.QUESTION_MESSAGE,
            null, null, "0"
        );
        
        if (input == null) return;
        
        try {
            if (input.trim().startsWith("+")) input = input.trim().substring(1);
            int amount = Integer.parseInt(input.trim());
            if (amount == 0) return;
            
            LoanCorrectionAction lca = new LoanCorrectionAction(selectedCompany);
            lca.setAmount(amount);
            
            getParent().process(lca); 
            
        } catch (NumberFormatException e) {
             DisplayBuffer.add(this, "Invalid Amount entered");
        }
    }

    private boolean execute(LoanCorrectionAction action) {
        PublicCompany comp = action.getCompany();
        int amount = action.getAmount();
        int currentLoans = comp.getNumberOfBonds();
        int newLoans = currentLoans + amount;

        if (newLoans < 0) {
            DisplayBuffer.add(this, "Correction failed: " + comp.getId() + " cannot have negative loans.");
            return true;
        }

        // 1817-specific bond adjustment
        if (comp instanceof PublicCompany_1817) {
            ((PublicCompany_1817) comp).setNumberOfBonds(newLoans);
        } else {
            log.warn("Attempting to correct loans on a non-1817 company. This may not be fully supported.");
        }

        String msg = "Correction: " + comp.getId() + " loans adjusted by " + (amount > 0 ? "+" + amount : amount) + " to " + newLoans + ".";
        ReportBuffer.add(this, msg);
        getParent().addToNextPlayerMessages(msg, true);
        
        // Trigger global interest rate recalculation if playing 1817
        if (getParent() instanceof GameManager_1817) {
            net.sf.rails.game.model.BondsModel bm = ((GameManager_1817) getParent()).getBondsModel();
            if (bm instanceof BondsModel_1817) {
                ((BondsModel_1817) bm).updateInterestRate();
                log.info("DEBUG: Triggered BondsModel_1817 interest rate update after loan correction.");
            }
        }

// Force UI Refresh specifically for Status Window
        if (getParent().getGameUIManager() != null) {
            getParent().getGameUIManager().forceFullUIRefresh();
            
            if (getParent().getGameUIManager().getStatusWindow() != null) {
                getParent().getGameUIManager().getStatusWindow().repaint();
            }
        }

        return true;
    }
}