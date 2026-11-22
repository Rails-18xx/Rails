package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Company; // Generic Company interface
import net.sf.rails.game.Player;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

public class PrivateCorrectionAction extends CorrectionAction {

    private static final long serialVersionUID = 1L;

    private String companyId;

    public PrivateCorrectionAction(RailsRoot root, Company company) {
        super(root);
        
        // VALIDATION: Actor must be current player
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : "Moderator";

        this.companyId = company.getId();
        
        setCorrectionType(CorrectionType.CLOSE_PRIVATE);
    }

    public String getCompanyId() { return companyId; }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        PrivateCorrectionAction action = (PrivateCorrectionAction)pa;
        return Objects.equal(this.companyId, action.companyId);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("company", companyId)
                    .toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.CLOSE_PRIVATE);
        }
    }
}