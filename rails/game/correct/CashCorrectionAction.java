package rails.game.correct;

import rails.game.*;
import rails.game.action.PossibleAction;
import rails.util.Util;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Correction action that changes the cash position of a cashholder.
 * 
 * @author Stefan Frey
 */
public class CashCorrectionAction extends CorrectionAction {

    /** The Constant serialVersionUID. */
    public static final long serialVersionUID = 1L;
    
    /* Preconditions */
   
    /** cash holder */
    transient private CashHolder correctCashHolder; 

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
   public CashCorrectionAction(Player pl) {
       correctCashHolder = pl;
       cashHolderName = pl.getName();
       cashHolderType = "Player";
       maximumNegative = pl.getCash();
       setCorrectionType(CorrectionType.CORRECT_CASH);
   }
   /**
    * Instantiates a new correct cash
    * 
    * @param pc Public Company
    */
   public CashCorrectionAction(PublicCompanyI pc) {
       correctCashHolder = pc;
       cashHolderName = pc.getName();
       cashHolderType = "PublicCompany";
       maximumNegative = pc.getCash();
       setCorrectionType(CorrectionType.CORRECT_CASH);
   }
   
   
   public CashHolder getCashHolder() {
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
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof CashCorrectionAction)) return false;
        CashCorrectionAction a = (CashCorrectionAction) action;
        return (a.correctCashHolder == this.correctCashHolder &&
                a.maximumNegative == this.maximumNegative
        );
    }
    
    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof CashCorrectionAction)) return false;
        CashCorrectionAction a = (CashCorrectionAction) action;
        return (a.correctCashHolder == this.correctCashHolder &&
                a.correctAmount == this.correctAmount
        );
    }
    
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("CashCorrectionAction ");
        if (acted) {
            b.append(" (acted)");
            if (correctCashHolder != null)
                b.append(", correctCashHolder="+correctCashHolder);
            b.append(", correctAmount="+correctAmount);
        } else {
            b.append(" (not acted)");
            if (correctCashHolder != null)
                b.append(", correctCashHolder="+correctCashHolder);
            b.append(", maximumNegative="+maximumNegative);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        
        if (Util.hasValue(correctionName))
            correctionType = CorrectionType.valueOf(correctionName);

        if (Util.hasValue(cashHolderType) && Util.hasValue(cashHolderName)) {
            if (cashHolderType.equals("Player"))
                correctCashHolder = getGameManager().getPlayerManager().getPlayerByName(cashHolderName);
            else if (cashHolderType.equals("PublicCompany"))
                correctCashHolder = getCompanyManager().getPublicCompany(cashHolderName);
        }
    }
}
