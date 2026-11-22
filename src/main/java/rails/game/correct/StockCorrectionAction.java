package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Player;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;

public class StockCorrectionAction extends CorrectionAction {

    private static final long serialVersionUID = 2L;

    // Persist as Strings/Ints for safety
    private String companyName;
    private int rowIndex;
    private int colIndex;
    private String priceDisplay; // For logging (e.g. "100")

    public StockCorrectionAction(RailsRoot root, PublicCompany company, StockSpace space) {
        super(root);
        
        // VALIDATION: Actor must be current player
        Player current = root.getGameManager().getCurrentPlayer();
        this.playerName = (current != null) ? current.getId() : "Moderator";

        this.companyName = company.getId();
        this.rowIndex = space.getRow();
        this.colIndex = space.getColumn();
        // [FIX] Use toText() for price display, as getName() does not exist
        this.priceDisplay = space.toText(); 
        
        setCorrectionType(CorrectionType.CORRECT_STOCK);
    }

    public String getCompanyName() { return companyName; }
    public int getRowIndex() { return rowIndex; }
    public int getColIndex() { return colIndex; }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        StockCorrectionAction action = (StockCorrectionAction)pa;
        return Objects.equal(this.companyName, action.companyName)
            && Objects.equal(this.rowIndex, action.rowIndex)
            && Objects.equal(this.colIndex, action.colIndex);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("company", companyName)
                    .addToString("pos", "[" + rowIndex + "," + colIndex + "]")
                    .addToString("price", priceDisplay)
                    .toString();
    }

    // --- FIX: Explicitly restore CorrectionType on reload ---
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.correctionType == null) {
            setCorrectionType(CorrectionType.CORRECT_STOCK);
        }
    }
}