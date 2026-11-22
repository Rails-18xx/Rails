package rails.game.correct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.Owner;

public class PrivateMoveCorrectionManager extends CorrectionManager {

    private PrivateMoveCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.MOVE_PRIVATE);
    }

    public static PrivateMoveCorrectionManager create(GameManager parent) {
        return new PrivateMoveCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<>();
        actions.add(new CorrectionModeAction(getRoot(), CorrectionType.MOVE_PRIVATE, isActive()));
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

        if (action instanceof PrivateMoveCorrectionAction) {
            return execute((PrivateMoveCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
        CompanyManager cmgr = getRoot().getCompanyManager();
        Map<String, Owner> ownerMap = getAllPotentialOwners();

        // Step 1: Select Private Company
        List<PrivateCompany> privates = cmgr.getAllPrivateCompanies().stream()
                .filter(p -> !p.isClosed() && p.getOwner() != null)
                .collect(Collectors.toList());

        if (privates.isEmpty()) {
            DisplayBuffer.add(this, "No valid private companies available to move.");
            return;
        }

        List<String> privateNames = privates.stream()
                .map(PrivateCompany::getId)
                .sorted()
                .collect(Collectors.toList());

        String selectedPrivName = (String) JOptionPane.showInputDialog(
            null, "Select Private Company:", "Move Private Correction (1/2)",
            JOptionPane.QUESTION_MESSAGE, null, privateNames.toArray(), privateNames.get(0)
        );
        if (selectedPrivName == null) return;
        
        PrivateCompany priv = cmgr.getPrivateCompany(selectedPrivName);
        Owner source = priv.getOwner();

        // Step 2: Select Destination
        List<String> destOptions = new ArrayList<>(ownerMap.keySet());
        destOptions.remove(source.getId()); // Remove current owner from options
        Collections.sort(destOptions);
        
        String destName = (String) JOptionPane.showInputDialog(
            null, "Select Destination:", "Move Private Correction (2/2)",
            JOptionPane.QUESTION_MESSAGE, null, destOptions.toArray(), destOptions.get(0)
        );
        if (destName == null) return;
        Owner dest = ownerMap.get(destName);

        // Step 3: Dispatch Action
        PrivateMoveCorrectionAction pmca = new PrivateMoveCorrectionAction(getRoot(), priv, source, dest);
        getParent().process(pmca);
    }

    private boolean execute(PrivateMoveCorrectionAction action) {
        CompanyManager cmgr = getRoot().getCompanyManager();
        Map<String, Owner> ownerMap = getAllPotentialOwners();

        PrivateCompany priv = cmgr.getPrivateCompany(action.getPrivateId());
        Owner source = ownerMap.get(action.getSourceId());
        Owner dest = ownerMap.get(action.getDestId());

        if (priv == null || source == null || dest == null) {
            DisplayBuffer.add(this, "Error: Invalid entities in Private Move Correction.");
            return false;
        }

        if (priv.getOwner() != source) {
            DisplayBuffer.add(this, "Error: Source does not own the private company.");
            return false;
        }

        // Execute Move
        priv.moveTo(dest);

        String msg = String.format("Correction: Moved Private %s from %s to %s", 
                priv.getId(), action.getSourceId(), action.getDestId());
        
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);
        
        if (getParent().getGameUIManager() != null && !getParent().isReloading()) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }
        
        return true;
    }

    private Map<String, Owner> getAllPotentialOwners() {
        Map<String, Owner> owners = new HashMap<>();
        
        for (Player p : getRoot().getPlayerManager().getPlayers()) {
            owners.put(p.getId(), p);
        }
        
        if (getRoot().getBank().getIpo() != null)
            owners.put(getRoot().getBank().getIpo().getId(), getRoot().getBank().getIpo());
            
        if (getRoot().getBank().getPool() != null)
            owners.put(getRoot().getBank().getPool().getId(), getRoot().getBank().getPool());
        
        for (PublicCompany pc : getRoot().getCompanyManager().getAllPublicCompanies()) {
            if (pc.hasStarted() && !pc.isClosed()) {
                owners.put(pc.getId(), pc);
            }
        }
        
        return owners;
    }
}