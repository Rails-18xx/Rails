package rails.game.correct;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Owner;

public class PrivateCorrectionManager extends CorrectionManager {

    private PrivateCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CLOSE_PRIVATE);
    }

    public static PrivateCorrectionManager create(GameManager parent) {
        return new PrivateCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<>();
        actions.add(new CorrectionModeAction(getRoot(), CorrectionType.CLOSE_PRIVATE, isActive()));
        return actions;
    }

    @Override
    public boolean executeCorrection(CorrectionAction action) {
        if (action instanceof CorrectionModeAction) {
            if (!isActive()) {
                if (!getParent().isReloading()) {
                    runWizard();
                }
                return true; 
            }
            return super.executeCorrection(action);
        }

        if (action instanceof PrivateCorrectionAction) {
            return execute((PrivateCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
        CompanyManager cmgr = getRoot().getCompanyManager();
        List<Company> candidates = new ArrayList<>();

        // 1. Add Open Privates
        for (PrivateCompany p : cmgr.getAllPrivateCompanies()) {
            if (!p.isClosed()) candidates.add(p);
        }

        // 2. Add Open Minors (Public Companies typed as "Minor")
        for (PublicCompany p : cmgr.getAllPublicCompanies()) {
            // [FIX] Use getId() to check for "Minor" type (matches PrussianFormationRound logic)
            if (!p.isClosed() && p.getType().getId().contains("Minor")) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            DisplayBuffer.add(this, "No open private or minor companies found.");
            return;
        }

        List<String> names = candidates.stream()
                .map(Company::getId)
                .sorted()
                .collect(Collectors.toList());

        String selectedName = (String) JOptionPane.showInputDialog(
            null, 
            "Select Company to CLOSE:",
            "Close Company Correction",
            JOptionPane.QUESTION_MESSAGE,
            null, 
            names.toArray(), 
            names.get(0)
        );

        if (selectedName == null) return;

        // Find the object
        Company selectedComp = cmgr.getPrivateCompany(selectedName);
        if (selectedComp == null) selectedComp = cmgr.getPublicCompany(selectedName);
        
        if (selectedComp == null) return;

        PrivateCorrectionAction pca = new PrivateCorrectionAction(getRoot(), selectedComp);
        getParent().process(pca);
    }

    private boolean execute(PrivateCorrectionAction action) {
        CompanyManager cmgr = getRoot().getCompanyManager();
        String id = action.getCompanyId();
        
        Company company = cmgr.getPrivateCompany(id);
        if (company == null) company = cmgr.getPublicCompany(id);

        if (company == null) {
            DisplayBuffer.add(this, "Error: Company not found: " + id);
            return false;
        }

        boolean isClosed = (company instanceof PrivateCompany) ? ((PrivateCompany)company).isClosed() : ((PublicCompany)company).isClosed();
        if (isClosed) {
            DisplayBuffer.add(this, "Company " + id + " is already closed.");
            return true; 
        }

        // --- SPECIAL LOGIC: Handle 1835 Exchange (BB/HB/Minors -> PR Share) ---
        handleExchange(company);

        // Execute Close
        if (company instanceof PrivateCompany) {
            ((PrivateCompany) company).close();
        } else if (company instanceof PublicCompany) {
            ((PublicCompany) company).setClosed();
        }

        String msg = "Correction: Closed " + company.getId();
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);
        
        if (getParent().getGameUIManager() != null && !getParent().isReloading()) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }
        
        return true;
    }

    /**
     * Checks if the company has an exchange property (like 1835's BB/HB/Minors)
     * and performs the share swap if applicable.
     */
    private void handleExchange(Company comp) {
        // Only players can hold the exchanged share
        Owner owner;
        if (comp instanceof PrivateCompany) owner = ((PrivateCompany)comp).getOwner();
        else owner = ((PublicCompany)comp).getPresident();

        if (!(owner instanceof Player)) return; 

        // 1. Look for ExchangeForShare property
        Set<SpecialProperty> sps = comp.getSpecialProperties();
        if (sps == null) return;

        ExchangeForShare exchangeProp = null;
        for (SpecialProperty sp : sps) {
            if (sp instanceof ExchangeForShare) {
                exchangeProp = (ExchangeForShare) sp;
                break;
            }
        }

        if (exchangeProp == null) return;

        // 2. Identify Target Company (Hardcoded to "PR" for 1835 context)
        CompanyManager cmgr = getRoot().getCompanyManager();
        PublicCompany pr = cmgr.getPublicCompany("PR"); 
        
        if (pr == null) return; 

        int sharePercent = exchangeProp.getShare();
        
        // 3. Find the certificate in the Bank's IPO
        PortfolioModel ipo = getRoot().getBank().getIpo().getPortfolioModel();
        PublicCertificate cert = null;

        for (PublicCertificate c : ipo.getCertificates(pr)) {
            if (c.getShare() == sharePercent) {
                cert = c;
                break;
            }
        }

        // 4. Execute Transfer
        if (cert != null) {
            cert.moveTo(owner); // <--- This line ensures the share goes to the owner
            String msg = "Correction: Exchanged " + comp.getId() + " for " + sharePercent + "% of " + pr.getId();
            ReportBuffer.add(this, msg);
            DisplayBuffer.add(this, msg);
        } else {
            DisplayBuffer.add(this, "Warning: Could not find " + sharePercent + "% " + pr.getId() + " share for exchange in IPO.");
        }
    }
}