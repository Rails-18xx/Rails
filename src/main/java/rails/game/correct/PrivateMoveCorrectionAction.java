package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.Player;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

public class PrivateMoveCorrectionAction extends CorrectionAction {

    private static final long serialVersionUID = 1L;

    private String privateId;
    private String sourceId;
    private String destId;

    public PrivateMoveCorrectionAction(RailsRoot root, PrivateCompany priv, Owner source, Owner dest) {
        super(root);
        
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : "Moderator";

        this.privateId = priv.getId();
        this.sourceId = source.getId();
        this.destId = dest.getId();
        
        setCorrectionType(CorrectionType.MOVE_PRIVATE);
    }

    public String getPrivateId() { return privateId; }
    public String getSourceId() { return sourceId; }
    public String getDestId() { return destId; }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        PrivateMoveCorrectionAction action = (PrivateMoveCorrectionAction)pa;
        return Objects.equal(this.privateId, action.privateId)
            && Objects.equal(this.sourceId, action.sourceId)
            && Objects.equal(this.destId, action.destId);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("private", privateId)
                    .addToString("from", sourceId)
                    .addToString("to", destId)
                    .toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.MOVE_PRIVATE);
        }
    }
}