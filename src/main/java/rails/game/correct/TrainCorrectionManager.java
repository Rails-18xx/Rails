package rails.game.correct;

import java.util.ArrayList;
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
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Owner;

public class TrainCorrectionManager extends CorrectionManager {

    private TrainCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_TRAINS);
    }

    public static TrainCorrectionManager create(GameManager parent) {
        return new TrainCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        // Only add the Mode Toggle. We handle the specific trains via the Wizard.
        List<CorrectionAction> actions = new ArrayList<>();
        actions.add(new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_TRAINS, isActive()));
        return actions;
    }

    @Override
    public boolean executeCorrection(CorrectionAction action) {
        
        // 1. Menu Click -> Wizard
        if (action instanceof CorrectionModeAction) {
            if (!isActive()) {
                // [STABILITY FIX] Prevent Popups during Reload
                if (!getParent().isReloading()) {
                    runWizard();
                }
                return true; 
            }
            return super.executeCorrection(action);
        }

        // 2. Execution Logic (Undo/Redo safe)
        if (action instanceof TrainCorrectionAction) {
            return execute((TrainCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
        TrainManager tmgr = getRoot().getTrainManager();
        CompanyManager cmgr = getRoot().getCompanyManager();
        Map<String, Owner> ownerMap = getAllTrainOwners(cmgr);
        List<String> ownerNames = new ArrayList<>(ownerMap.keySet());
        java.util.Collections.sort(ownerNames);

        // Step 1: Select Source Owner
        String sourceName = (String) JOptionPane.showInputDialog(
            null, 
            "Select Owner taking the train FROM:",
            "Train Correction (1/3)",
            JOptionPane.QUESTION_MESSAGE,
            null, 
            ownerNames.toArray(), 
            ownerNames.get(0)
        );
        if (sourceName == null) return;
        Owner source = ownerMap.get(sourceName);

        // Step 2: Select Train
        List<Train> trains = tmgr.getTrains(source);
        if (trains == null || trains.isEmpty()) {
            DisplayBuffer.add(this, sourceName + " has no trains.");
            return;
        }
        
        List<String> trainOptions = trains.stream()
            .map(t -> t.getName() + " (" + t.getId() + ")") 
            .collect(Collectors.toList());

        String selectedTrainStr = (String) JOptionPane.showInputDialog(
            null, 
            "Select Train:",
            "Train Correction (2/3)",
            JOptionPane.QUESTION_MESSAGE,
            null, 
            trainOptions.toArray(), 
            trainOptions.get(0)
        );
        if (selectedTrainStr == null) return;

        String trainId = selectedTrainStr.substring(
            selectedTrainStr.lastIndexOf("(") + 1, 
            selectedTrainStr.lastIndexOf(")")
        );
        Train train = tmgr.getTrainByUniqueId(trainId);

        // Step 3: Select Destination
        String destName = (String) JOptionPane.showInputDialog(
            null, 
            "Select Destination:",
            "Train Correction (3/3)",
            JOptionPane.QUESTION_MESSAGE,
            null, 
            ownerNames.toArray(), 
            ownerNames.get(0)
        );
        if (destName == null) return;

        // Step 4: Create Action
        TrainCorrectionAction tca = new TrainCorrectionAction(getRoot(), train, sourceName, destName);
        getParent().process(tca);
    }

    private boolean execute(TrainCorrectionAction action) {
        TrainManager tmgr = getRoot().getTrainManager();
        CompanyManager cmgr = getRoot().getCompanyManager();
        Map<String, Owner> ownerMap = getAllTrainOwners(cmgr);

        Train train = tmgr.getTrainByUniqueId(action.getTrainId());
        Owner dest = ownerMap.get(action.getDestOwnerName());
        Owner source = ownerMap.get(action.getSourceOwnerName());

        if (train == null || dest == null) {
            DisplayBuffer.add(this, "Error: Train or Destination not found during execution.");
            return false;
        }


        // We prioritize using the PortfolioModel API ("addTrainCard") over TrainManager.transferTrain.
        // using 'addTrainCard' ensures the card is correctly removed from the Source list (fixing Ghost Trains)
        // before being added to the Destination. 
        // If we called transferTrain first, the owner update would prevent the removal logic from firing.

        net.sf.rails.game.model.PortfolioModel destModel = null;

        if (dest instanceof net.sf.rails.game.financial.BankPortfolio) {
            destModel = ((net.sf.rails.game.financial.BankPortfolio) dest).getPortfolioModel();
        } else if (dest instanceof net.sf.rails.game.PublicCompany) {
            destModel = ((net.sf.rails.game.PublicCompany) dest).getPortfolioModel();
        } else if (dest instanceof net.sf.rails.game.Player) {
            destModel = ((net.sf.rails.game.Player) dest).getPortfolioModel();
        }

        if (action.getDestOwnerName().contains("Trash") || action.getDestOwnerName().contains("Scrap")) {
            tmgr.trashTrain(train);
        } else if (destModel != null) {
            // "Pull" the train to the new portfolio. This handles the remove(source) + add(dest) atomically.
            destModel.addTrainCard(train.getCard());
        } else {
            // Fallback for destinations without a PortfolioModel (should be rare)
            tmgr.transferTrain(train, dest);
        }

        // If the train was magically removed from the IPO, we must notify the TrainManager.
        // This ensures that if the last train of a tier (e.g., the last '2') is moved, 
        // the next tier (e.g., '3') is automatically released into the IPO.
        if (source == getRoot().getBank().getIpo()) {
            tmgr.checkTrainAvailability(train, source);
        }
        

        String msg = LocalText.getText("CorrectTrainTransfer", 
                train.getName(), 
                action.getSourceOwnerName(), 
                action.getDestOwnerName());
        
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);

        // [STABILITY FIX] Prevent UI Refresh during Reload to avoid NullPointerExceptions
        if (getParent().getGameUIManager() != null && !getParent().isReloading()) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }

        return true;
    }

    private Map<String, Owner> getAllTrainOwners(CompanyManager cmgr) {
        Map<String, Owner> owners = new HashMap<>();
        
        for (PublicCompany pc : cmgr.getAllPublicCompanies()) {
            if (!pc.isClosed()) { 
                owners.put(pc.getId(), pc);
            }
        }
        
        owners.put("Bank Pool", getRoot().getBank().getPool());
        owners.put("Bank IPO", getRoot().getBank().getIpo());
        owners.put("Trash/Scrap Heap", getRoot().getBank().getScrapHeap());

        return owners;
    }
}