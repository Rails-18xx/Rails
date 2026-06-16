package rails.game.correct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;

public class StockCorrectionManager extends CorrectionManager {

    private StockCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_STOCK);
    }

    public static StockCorrectionManager create(GameManager parent) {
        return new StockCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<>();
        actions.add(new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_STOCK, isActive()));
        return actions;
    }

    @Override
    public boolean executeCorrection(CorrectionAction action) {
        
        // 1. Menu Click -> Wizard
        if (action instanceof CorrectionModeAction) {
            if (!isActive()) {
                // Prevent Popups during Reload
                if (!getParent().isReloading()) {
                    runWizard();
                }
                return true; 
            }
            return super.executeCorrection(action);
        }

        // 2. Execution Logic (Undo/Redo safe)
        if (action instanceof StockCorrectionAction) {
            return execute((StockCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
        // Step 1: Select Company
        List<PublicCompany> companies = getRoot().getCompanyManager().getAllPublicCompanies();
        
        // Filter: Only companies that have started or floated can be on the market
        List<String> companyNames = companies.stream()
                .filter(c -> !c.isClosed() && (c.hasFloated() || c.hasStarted()))
                .map(PublicCompany::getId)
                .sorted()
                .collect(Collectors.toList());

        if (companyNames.isEmpty()) {
            DisplayBuffer.add(this, "No active public companies found on the market.");
            return;
        }

        String selectedCompName = (String) JOptionPane.showInputDialog(
            null, 
            "Select Company to Move:",
            "Stock Correction (1/2)",
            JOptionPane.QUESTION_MESSAGE,
            null, 
            companyNames.toArray(), 
            companyNames.get(0)
        );
        if (selectedCompName == null) return;

        PublicCompany company = getRoot().getCompanyManager().getPublicCompany(selectedCompName);

        // Step 2: Select Target Space
        // We scan the market to build a list of valid spaces
        List<StockSpace> validSpaces = scanMarketForSpaces();
        
        if (validSpaces.isEmpty()) {
            DisplayBuffer.add(this, "Could not find any stock market spaces.");
            return;
        }

        // Format: "H1 $100 (Row 5, Col 2)"
        List<String> spaceOptions = validSpaces.stream()
            .map(s -> String.format("%s %s (R:%d, C:%d)", s.getId(), s.toText(), s.getRow(), s.getColumn()))
            .collect(Collectors.toList());

        // Pre-select the company's current position if possible
        String defaultOption = spaceOptions.get(0);
        if (company.getCurrentSpace() != null) {
            StockSpace curr = company.getCurrentSpace();
            String currStr = String.format("%s %s (R:%d, C:%d)", curr.getId(), curr.toText(), curr.getRow(), curr.getColumn());
            if (spaceOptions.contains(currStr)) defaultOption = currStr;
        }

        String selectedSpaceStr = (String) JOptionPane.showInputDialog(
            null, 
            "Select New Market Position:",
            "Stock Correction (2/2)",
            JOptionPane.QUESTION_MESSAGE,
            null, 
            spaceOptions.toArray(), 
            defaultOption
        );
        if (selectedSpaceStr == null) return;

        // Map selection back to object
        int index = spaceOptions.indexOf(selectedSpaceStr);
        if (index < 0) return;
        StockSpace targetSpace = validSpaces.get(index);

        // Step 3: Create Action
        StockCorrectionAction sca = new StockCorrectionAction(getRoot(), company, targetSpace);
        getParent().process(sca);
    }

    private boolean execute(StockCorrectionAction action) {
        PublicCompany company = getRoot().getCompanyManager().getPublicCompany(action.getCompanyName());
        StockMarket market = getRoot().getStockMarket();

        if (company == null || market == null) return false;

        StockSpace newSpace = market.getStockSpace(action.getRowIndex(), action.getColIndex());
        if (newSpace == null) {
            DisplayBuffer.add(this, "Error: Invalid stock market coordinates: " + action.getRowIndex() + "," + action.getColIndex());
            return false;
        }

        // [FIX] Perform the physical move on the StockSpace objects
        // 1. Remove from old space (if exists)
        StockSpace oldSpace = company.getCurrentSpace();
        if (oldSpace != null) {
            oldSpace.removeToken(company);
        }

        // 2. Add to new space (this updates the token stack which the UI draws)
        newSpace.addToken(company);

        // 3. Update the company's internal reference
        company.setCurrentSpace(newSpace);

        // [FIX] Hardcoded message to avoid "Missing text" error
        String msg = "Correction: Moved " + company.getId() + " to " + newSpace.getId() + " (" + newSpace.toText() + ")";
        
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);
        
        // Safe UI Refresh
        if (getParent().getGameUIManager() != null && !getParent().isReloading()) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }
        
        return true;
    }

    /**
     * Helper to find all valid spaces.
     * Iterates a reasonable grid range since StockMarket doesn't always expose a full list.
     */
    private List<StockSpace> scanMarketForSpaces() {
        List<StockSpace> spaces = new ArrayList<>();
        StockMarket market = getRoot().getStockMarket();
        
        // Brute-force scan reasonable limits
        for (int r = 0; r < 50; r++) {
            for (int c = 0; c < 50; c++) {
                StockSpace ss = market.getStockSpace(r, c);
                if (ss != null) {
                    spaces.add(ss);
                }
            }
        }
        
        // Sort by Price (Ascending), then Row/Col
        Collections.sort(spaces, new Comparator<StockSpace>() {
            @Override
            public int compare(StockSpace s1, StockSpace s2) {
                int p1 = s1.getPrice();
                int p2 = s2.getPrice();
                if (p1 != p2) return Integer.compare(p1, p2);
                if (s1.getRow() != s2.getRow()) return Integer.compare(s1.getRow(), s2.getRow());
                return Integer.compare(s1.getColumn(), s2.getColumn());
            }
        });
        
        return spaces;
    }
}