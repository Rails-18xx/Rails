package rails.game.correct;


import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.GameLoader;
import rails.game.action.PossibleAction;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * Correction action that changes the cash position of a MoneyOwner.
 *
 * Rails 2.0: updated equals and toString methods
 */
public class CashCorrectionAction extends CorrectionAction {

    /** The Constant serialVersionUID. */
    public static final long serialVersionUID = 1L;

    /* Preconditions */

    /** cash holder */
    transient private MoneyOwner correctCashHolder;

    /** converted to name */
    private String cashHolderName;
    private String cashHolderType;

    /** maximum Amount to deduct */
    private int maximumNegative;

    /* Postconditions */

    /** selected cash amount */
    private int correctAmount;

   /**
    * Instantiates a new correct cash
    *
    * @param pl Player
    */
   public CashCorrectionAction(RailsRoot root, Player pl) {
       super(root);
       correctCashHolder = pl;
       cashHolderName = pl.getId();
       cashHolderType = "Player";
       maximumNegative = pl.getCashValue();
       setCorrectionType(CorrectionType.CORRECT_CASH);
   }
   /**
    * Instantiates a new correct cash
    *
    * @param pc Public Company
    */
   public CashCorrectionAction(PublicCompany pc) {
       super(pc.getRoot());
       correctCashHolder = pc;
       cashHolderName = pc.getId();
       cashHolderType = "PublicCompany";
       maximumNegative = pc.getCash();
       setCorrectionType(CorrectionType.CORRECT_CASH);
   }


   public MoneyOwner getCashHolder() {
       return correctCashHolder;
   }

   public String getCashHolderName() {
       return cashHolderName;
   }

   public int getMaximumNegative(){
       return maximumNegative;
   }

   public int getAmount() {
       return correctAmount;
   }

   public void setAmount(int amount) {
       correctAmount = amount;
   }

   @Override
   protected boolean equalsAs(PossibleAction pa, boolean asOption) {
       // identity always true
       if (pa == this) return true;
       //  super checks both class identity and super class attributes
       if (!super.equalsAs(pa, asOption)) return false;

       // check asOption attributes
        CashCorrectionAction action = (CashCorrectionAction)pa;
        boolean options =  Objects.equal(this.correctCashHolder, action.correctCashHolder)
                && Objects.equal(this.maximumNegative, action.maximumNegative)
        ;

        // finish if asOptions check
        if (asOption) return options;

        return options
                && Objects.equal(this.correctAmount, action.correctAmount)
        ;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("correctCashHolder", correctCashHolder)
                    .addToString("maximumNegative", maximumNegative)
                    .addToStringOnlyActed("correctAmount", correctAmount)
                .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (in instanceof GameLoader.RailsObjectInputStream) {
            if (Util.hasValue(correctionName))
                correctionType = CorrectionType.valueOf(correctionName);

            if (Util.hasValue(cashHolderType) && Util.hasValue(cashHolderName)) {
                if (cashHolderType.equals("Player"))
                    correctCashHolder = getGameManager().getRoot().getPlayerManager().getPlayerByName(cashHolderName);
                else if (cashHolderType.equals("PublicCompany"))
                    correctCashHolder = getCompanyManager().getPublicCompany(cashHolderName);
            }
        }
    }

    public void applyRailsRoot(RailsRoot root) {
        super.applyRailsRoot(root);

        if (Util.hasValue(correctionName))
            correctionType = CorrectionType.valueOf(correctionName);

        if (Util.hasValue(cashHolderType) && Util.hasValue(cashHolderName)) {
            if (cashHolderType.equals("Player"))
                correctCashHolder = getGameManager().getRoot().getPlayerManager().getPlayerByName(cashHolderName);
            else if (cashHolderType.equals("PublicCompany"))
                correctCashHolder = getCompanyManager().getPublicCompany(cashHolderName);
        }
    }
}
