package rails.game.correct;

import rails.game.*;
import rails.game.action.PossibleAction;
import rails.util.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * Correction action that changes the cash position of a cashholder.
 * 
 * @author Stefan Frey
 */
public class CorrectCash extends PossibleAction implements CorrectCashI, CorrectionAction {

    /** The Constant serialVersionUID. */
    public static final long serialVersionUID = 1L;
    
    /* Preconditions */
    
    /** shows in correction menu */
    private boolean inCorrectionMenu;
    
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
   public CorrectCash(Player pl) {
       correctCashHolder = pl;
       cashHolderName = pl.getName();
       cashHolderType = "Player";
       maximumNegative = pl.getCash();
   }
   /**
    * Instantiates a new correct cash
    * 
    * @param pc Public Company
    */
   public CorrectCash(PublicCompanyI pc) {
       correctCashHolder = pc;
       cashHolderName = pc.getName();
       cashHolderType = "PublicCompany";
       maximumNegative = pc.getCash();
   }
   
   
   @Override
   public boolean isInCorrectionMenu(){
       return inCorrectionMenu;
   }
   public void setCorrectionMenu(boolean menu){
       inCorrectionMenu = menu;
   }

   @Override
   public CashHolder getCashHolder() {
       return correctCashHolder;
   }
   @Override
   public String getCashHolderName() {
       return cashHolderName;
   }

   @Override
   public int getAmount() {
       return correctAmount;
   }
   @Override
   public void setAmount(int amount) {
       correctAmount = amount;
   }
    
    @Override
    public boolean equals(PossibleAction action) {
        if (!(action instanceof CorrectCash)) return false;
        CorrectCash a = (CorrectCash) action;
        return (a.correctCashHolder == this.correctCashHolder &&
                a.maximumNegative == this.maximumNegative &&
                a.inCorrectionMenu == this.inCorrectionMenu
        );
    }
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("CorrectCash");
        if (acted) {
            b.append("Not Acted");
            if (correctCashHolder != null)
                b.append("correctCashHolder="+correctCashHolder);
            b.append("maximumNegative="+maximumNegative);
            b.append("inCorrectionMenu="+inCorrectionMenu);
        } else {
            b.append("Acted");
            if (correctCashHolder != null)
                b.append("correctCashHolder="+correctCashHolder);
            b.append("correctAmount="+correctAmount);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(cashHolderName) && Util.hasValue(cashHolderType)) {
            if (cashHolderType == "Player")
                correctCashHolder = getGameManager().getPlayerManager().getPlayerByName(cashHolderName);
            else if (cashHolderType == "PublicCompany")
                correctCashHolder = getCompanyManager().getCompanyByName(cashHolderName);
        }
    }
}
