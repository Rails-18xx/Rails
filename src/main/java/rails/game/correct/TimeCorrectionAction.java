package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Player;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

public class TimeCorrectionAction extends CorrectionAction {
    private static final long serialVersionUID = 2L; 

    private String targetPlayerName; // The player whose time is changed
    private int seconds; 

    public TimeCorrectionAction(RailsRoot root, Player targetPlayer, int seconds) {
        super(root);
        
        // VALIDATION FIX: The 'Actor' must be the current player
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : targetPlayer.getId();
        
        this.targetPlayerName = targetPlayer.getId();
        this.seconds = seconds;
        
        setCorrectionType(CorrectionType.CORRECT_TIME);
    }

    public String getTargetPlayerName() {
        return (targetPlayerName != null) ? targetPlayerName : getPlayerName();
    }

    public int getSeconds() {
        return seconds;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("actor", playerName)
                    .addToString("target", targetPlayerName)
                    .addToString("seconds", seconds)
                    .toString();
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        TimeCorrectionAction other = (TimeCorrectionAction) pa;
        return Objects.equal(this.playerName, other.playerName)
                && Objects.equal(this.targetPlayerName, other.targetPlayerName)
                && Objects.equal(this.seconds, other.seconds);
    }

    // --- FIX: Explicitly restore the CorrectionType on reload ---
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Force the type to be set correctly if it wasn't restored by the superclass
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.CORRECT_TIME);
        }
    }
}