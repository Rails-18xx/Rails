package rails.game.correct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.state.Owner;

public class ShareCorrectionManager extends CorrectionManager {

    private ShareCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_SHARES);
    }

    public static ShareCorrectionManager create(GameManager parent) {
        return new ShareCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<>();
        actions.add(new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_SHARES, isActive()));
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
        if (action instanceof ShareCorrectionAction) {
            return execute((ShareCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
        CompanyManager cmgr = getRoot().getCompanyManager();
        Map<String, Owner> ownerMap = getAllPotentialOwners();

        // Step 1: Select Company
        List<PublicCompany> companies = cmgr.getAllPublicCompanies();
        List<String> companyNames = companies.stream()
                .map(PublicCompany::getId)
                .sorted()
                .collect(Collectors.toList());

        String selectedCompName = (String) JOptionPane.showInputDialog(
            null, "Select Company:", "Share Correction (1/4)",
            JOptionPane.QUESTION_MESSAGE, null, companyNames.toArray(), companyNames.get(0)
        );
        if (selectedCompName == null) return;
        PublicCompany company = cmgr.getPublicCompany(selectedCompName);

        // Step 2: Select Source Owner
        // Filter owners to only those who actually have shares in this company
        List<String> validSources = new ArrayList<>();
        for (Map.Entry<String, Owner> entry : ownerMap.entrySet()) {
            Owner owner = entry.getValue();
            if (owner instanceof PortfolioOwner) {
                PortfolioModel pm = ((PortfolioOwner) owner).getPortfolioModel();
                if (pm.getShares(company) > 0) {
                    validSources.add(entry.getKey());
                }
            }
        }
        
        if (validSources.isEmpty()) {
            DisplayBuffer.add(this, "No one owns shares in " + company.getId());
            return;
        }
        Collections.sort(validSources);

        String sourceName = (String) JOptionPane.showInputDialog(
            null, "Select Source (Owner):", "Share Correction (2/4)",
            JOptionPane.QUESTION_MESSAGE, null, validSources.toArray(), validSources.get(0)
        );
        if (sourceName == null) return;
        Owner source = ownerMap.get(sourceName);

        // Step 3: Select Specific Certificate
        PortfolioModel sourcePm = ((PortfolioOwner) source).getPortfolioModel();
        List<PublicCertificate> certs = new ArrayList<>(sourcePm.getCertificates(company));
        
        if (certs.isEmpty()) return; 

        List<String> certOptions = certs.stream()
            .map(c -> (c.isPresidentShare() ? "Pres " : "") + c.getShare() + "%")
            .collect(Collectors.toList());

        String selectedCertStr = (String) JOptionPane.showInputDialog(
            null, "Select Certificate to Move:", "Share Correction (3/4)",
            JOptionPane.QUESTION_MESSAGE, null, certOptions.toArray(), certOptions.get(0)
        );
        if (selectedCertStr == null) return;
        
        // Map string back to certificate
        int certIndex = certOptions.indexOf(selectedCertStr);
        PublicCertificate selectedCert = certs.get(certIndex);

        // Step 4: Select Destination
        List<String> destOptions = new ArrayList<>(ownerMap.keySet());
        Collections.sort(destOptions);
        
        String destName = (String) JOptionPane.showInputDialog(
            null, "Select Destination:", "Share Correction (4/4)",
            JOptionPane.QUESTION_MESSAGE, null, destOptions.toArray(), destOptions.get(0)
        );
        if (destName == null) return;
        Owner dest = ownerMap.get(destName);

        // Step 5: Dispatch Action
        ShareCorrectionAction sca = new ShareCorrectionAction(getRoot(), company, source, dest, selectedCert);
        getParent().process(sca);
    }

    private boolean execute(ShareCorrectionAction action) {
        CompanyManager cmgr = getRoot().getCompanyManager();
        Map<String, Owner> ownerMap = getAllPotentialOwners();

        // Resolve Objects using IDs
        PublicCompany company = cmgr.getPublicCompany(action.getCompanyId());
        Owner source = ownerMap.get(action.getSourceId());
        Owner dest = ownerMap.get(action.getDestId());

        if (company == null || source == null || dest == null) {
            DisplayBuffer.add(this, "Error: Invalid entities in Share Correction.");
            // Log details for debugging
            // log.error("ShareCorrection Failure: Comp={}, SrcID={}, DestID={}", 
            //    action.getCompanyId(), action.getSourceId(), action.getDestId());
            return false;
        }

        if (!(source instanceof PortfolioOwner)) {
            DisplayBuffer.add(this, "Error: Source is not a valid portfolio owner.");
            return false;
        }

        // Find the specific certificate matching the action details
        PortfolioModel sourcePm = ((PortfolioOwner) source).getPortfolioModel();
        PublicCertificate certToMove = null;

        for (PublicCertificate cert : sourcePm.getCertificates(company)) {
            if (cert.getShare() == action.getSharePercent() && cert.isPresidentShare() == action.isPresidentShare()) {
                certToMove = cert;
                break;
            }
        }

        if (certToMove == null) {
            DisplayBuffer.add(this, "Error: Certificate not found in source portfolio.");
            return false;
        }

        // Execute Move (Standard Rails Method)
        certToMove.moveTo(dest);

        // Log
        String type = action.isPresidentShare() ? "Presidency" : (action.getSharePercent() + "%");
        
        // Use a direct string to avoid "Missing Key" errors for now, or ensure LocalText has this key
        String msg = String.format("Correction: Moved %s of %s from %s to %s", 
                type, company.getId(), action.getSourceId(), action.getDestId());
        
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);
        
        // Safe UI Refresh
        if (getParent().getGameUIManager() != null && !getParent().isReloading()) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }
        
        return true;
    }

    /**
     * Helper to get all possible share owners.
     * [FIX] Use getId() for map keys to match the Action storage format.
     */
    private Map<String, Owner> getAllPotentialOwners() {
        Map<String, Owner> owners = new HashMap<>();
        
        // Players
        for (Player p : getRoot().getPlayerManager().getPlayers()) {
            owners.put(p.getId(), p);
        }
        
        // Bank (Use actual IDs like "IPO", "Pool")
        if (getRoot().getBank().getIpo() != null)
            owners.put(getRoot().getBank().getIpo().getId(), getRoot().getBank().getIpo());
            
        if (getRoot().getBank().getPool() != null)
            owners.put(getRoot().getBank().getPool().getId(), getRoot().getBank().getPool());
        
        // Public Companies (Treasury)
        for (PublicCompany pc : getRoot().getCompanyManager().getAllPublicCompanies()) {
            // Only started companies exist as entities that can hold things
            if (pc.hasStarted() && !pc.isClosed()) {
                owners.put(pc.getId(), pc);
            }
        }
        
        return owners;
    }
}