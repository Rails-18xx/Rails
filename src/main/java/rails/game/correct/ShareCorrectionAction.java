package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.Player;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

public class ShareCorrectionAction extends CorrectionAction {

    private static final long serialVersionUID = 2L;

    // Persist ID strings to be safe for Save/Load
    private String companyId;
    private String sourceId;
    private String destId;
    
    // Certificate identification
    private int sharePercent;
    private boolean isPresidentShare;

    public ShareCorrectionAction(RailsRoot root, PublicCompany company, Owner source, Owner dest, PublicCertificate cert) {
        super(root);
        
        // VALIDATION: Actor must be current player
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : "Moderator";

        this.companyId = company.getId();
        this.sourceId = source.getId();
        this.destId = dest.getId();
        
        this.sharePercent = cert.getShare();
        this.isPresidentShare = cert.isPresidentShare();
        
        setCorrectionType(CorrectionType.CORRECT_SHARES);
    }

    public String getCompanyId() { return companyId; }
    public String getSourceId() { return sourceId; }
    public String getDestId() { return destId; }
    public int getSharePercent() { return sharePercent; }
    public boolean isPresidentShare() { return isPresidentShare; }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        ShareCorrectionAction action = (ShareCorrectionAction)pa;
        return Objects.equal(this.companyId, action.companyId)
            && Objects.equal(this.sourceId, action.sourceId)
            && Objects.equal(this.destId, action.destId)
            && Objects.equal(this.sharePercent, action.sharePercent)
            && Objects.equal(this.isPresidentShare, action.isPresidentShare);
    }

    @Override
    public String toString() {
        String type = isPresidentShare ? "Pres" : (sharePercent + "%");
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("comp", companyId)
                    .addToString("share", type)
                    .addToString("from", sourceId)
                    .addToString("to", destId)
                    .toString();
    }

    // --- FIX: Explicitly restore CorrectionType on reload ---
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.CORRECT_SHARES);
        }
    }
}