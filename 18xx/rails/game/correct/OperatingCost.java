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
public class OperatingCost extends PossibleAction implements CorrectCashI, CorrectionAction {

    public enum OCType {LAY_TILE, LAY_BASE_TOKEN};
    
    /** The Constant serialVersionUID. */
    public static final long serialVersionUID = 1L;
    
    /* Preconditions */
    
    /** shows in correction menu */
    private boolean inCorrectionMenu;
    
    /** operating Company */
    transient private PublicCompanyI operatingCompany; 

    /** converted to name */
    private String operatingCompanyName; 

    /** operating cost type (as tile lay, token lay etc.) */
    private OCType operatingCostType;

    /** suggested costs */
    private int suggestedCost;
    
    /** maximum costs */
    private int maximumCost;
    
    /* Postconditions */

    /** selected cash amount */
    private int operatingCost; 

   /**
    * Instantiates an operating costs action
    * 
    * @param pc Public Company
    */
   public OperatingCost(PublicCompanyI pc, OCType ot, int ocCosts) {
       operatingCompany = pc;
       operatingCompanyName = pc.getName();
       operatingCostType = ot;
       suggestedCost = ocCosts;
       maximumCost = pc.getCash();
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
       return operatingCompany;
   }
   @Override
   public String getCashHolderName() {
       return operatingCompanyName;
   }

   @Override
   public int getAmount() {
       if (acted)
           return -operatingCost;
       else
           return suggestedCost;
   }
   @Override
   public void setAmount(int amount) {
       acted=true;
       operatingCost = amount;
   }
   public OCType getOCType(){
       return operatingCostType;
   }
   
    @Override
    public boolean equals(PossibleAction action) {
        if (!(action instanceof OperatingCost)) return false;
        OperatingCost a = (OperatingCost) action;
        return (a.operatingCompany == this.operatingCompany &&
                a.operatingCostType == this.operatingCostType &&
                a.suggestedCost == this.suggestedCost &&
                a.maximumCost == this.maximumCost &&
                a.inCorrectionMenu == this.inCorrectionMenu
        );
    }
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("OperatingCost");
        if (!acted) {
            b.append(" (not acted)");
            if (operatingCompany != null)
                b.append(", operatingCompany="+operatingCompany);
            b.append(", operatingCostType="+operatingCostType);
            b.append(", suggestedCost="+suggestedCost);
            b.append(", maximumCost="+maximumCost);
            b.append(", inCorrectionMenu="+inCorrectionMenu);
        } else {
            b.append(" (acted)");
            if (operatingCompany != null)
                b.append(", operatingCompany="+operatingCompany);
            b.append(", operatingCostType="+operatingCostType);
            b.append(", operatingCost="+operatingCost);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(operatingCompanyName))
                operatingCompany = getCompanyManager().getCompanyByName(operatingCompanyName);
    }
}
