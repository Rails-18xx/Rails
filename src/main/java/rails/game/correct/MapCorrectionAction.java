package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Player;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

public class MapCorrectionAction extends CorrectionAction {

    private static final long serialVersionUID = 1L;

    private String hexName;
    private String tileNumber;
    private int rotation;

    public MapCorrectionAction(RailsRoot root, String hexName, String tileNumber, int rotation) {
        super(root);
        
        // VALIDATION: Actor must be current player
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : "Moderator";

        this.hexName = hexName;
        this.tileNumber = tileNumber;
        this.rotation = rotation;
        
        setCorrectionType(CorrectionType.CORRECT_MAP);
    }

    public String getHexName() { return hexName; }
    public String getTileNumber() { return tileNumber; }
    public int getRotation() { return rotation; }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        MapCorrectionAction action = (MapCorrectionAction)pa;
        return Objects.equal(this.hexName, action.hexName)
            && Objects.equal(this.tileNumber, action.tileNumber)
            && Objects.equal(this.rotation, action.rotation);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("hex", hexName)
                    .addToString("tile", tileNumber)
                    .addToString("rot", rotation)
                    .toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.CORRECT_MAP);
        }
    }
}