package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.Player;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;

public class TrainCorrectionAction extends CorrectionAction {

    private static final long serialVersionUID = 2L;

    // Legacy Enum for backward compatibility with GameStatus
    public static enum ActionType {
        ADD_TO_BANK,
        TRANSFER_OWNERSHIP,
        REMOVE_TO_TRASH;
    }

    // Persist IDs as Strings (Safe for Save/Load)
    private String trainId;
    private String sourceOwnerName;
    private String destOwnerName;

    public TrainCorrectionAction(RailsRoot root, Train train, String sourceName, String destName) {
        super(root);
        
        // VALIDATION: Actor must be current player to pass GameManager checks
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : "Moderator";

        this.trainId = train.getId(); 
        this.sourceOwnerName = sourceName;
        this.destOwnerName = destName;
        
        setCorrectionType(CorrectionType.CORRECT_TRAINS);
    }

    // --- Data Accessors ---
    public String getTrainId() { return trainId; }
    public String getSourceOwnerName() { return sourceOwnerName; }
    public String getDestOwnerName() { return destOwnerName; }

    // --- COMPATIBILITY METHODS (Restored for GameStatus.java) ---
    
    public Train getTargetTrain() {
        if (trainId == null || getRoot() == null) return null;
        // Look up the train object dynamically
        return getRoot().getTrainManager().getTrainByUniqueId(trainId);
    }

    public String getCurrentOwnerId() {
        return sourceOwnerName;
    }

    public String getNewOwnerId() {
        return destOwnerName;
    }

    public ActionType getActionType() {
        if (destOwnerName != null && (destOwnerName.contains("Trash") || destOwnerName.contains("Scrap"))) {
            return ActionType.REMOVE_TO_TRASH;
        }
        return ActionType.TRANSFER_OWNERSHIP;
    }

    // --- Serialization & Object Methods ---

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        TrainCorrectionAction action = (TrainCorrectionAction)pa;
        return Objects.equal(this.trainId, action.trainId)
            && Objects.equal(this.sourceOwnerName, action.sourceOwnerName)
            && Objects.equal(this.destOwnerName, action.destOwnerName);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("train", trainId)
                    .addToString("from", sourceOwnerName)
                    .addToString("to", destOwnerName)
                    .toString();
    }

    // Fix: Explicitly restore CorrectionType on reload to prevent null pointer crashes
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.CORRECT_TRAINS);
        }
    }
}