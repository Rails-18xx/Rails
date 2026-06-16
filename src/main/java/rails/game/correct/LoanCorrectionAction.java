package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;

/**
 * Correction action that changes the loan/bond count of a PublicCompany.
 */
public class LoanCorrectionAction extends CorrectionAction {

    public static final long serialVersionUID = 1L;

    /* Preconditions */
    transient private PublicCompany correctCompany;
    private String companyName;

    /* Postconditions */
    private int correctAmount;

    public LoanCorrectionAction(PublicCompany pc) {
        super(pc.getRoot());
        this.correctCompany = pc;
        this.companyName = pc.getId();
        setCorrectionType(CorrectionType.CORRECT_LOANS);
    }

    public PublicCompany getCompany() {
        return correctCompany;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getAmount() {
        return correctAmount;
    }

    public void setAmount(int amount) {
        this.correctAmount = amount;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;

        LoanCorrectionAction action = (LoanCorrectionAction) pa;
        boolean options = Objects.equal(this.companyName, action.companyName);

        if (asOption) return options;

        return options && Objects.equal(this.correctAmount, action.correctAmount);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("correctCompany", correctCompany)
                    .addToStringOnlyActed("correctAmount", correctAmount)
                .toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (Util.hasValue(correctionName)) {
            correctionType = CorrectionType.valueOf(correctionName);
        }

        if (Util.hasValue(companyName)) {
            correctCompany = getCompanyManager().getPublicCompany(companyName);
        }
    }
}