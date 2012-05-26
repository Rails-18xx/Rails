package rails.game.correct;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;
import rails.util.Util;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * OR action for no map mode 
 * mirrors operating actions like tile and token lays, but
 * only changes the cash position of the public company
 * @author Stefan Frey
 */
public class OperatingCost extends PossibleORAction {

    public enum OCType {LAY_TILE, LAY_BASE_TOKEN};
    
    /** The Constant serialVersionUID. */
    public static final long serialVersionUID = 2L;
    
    /* Preconditions */

    /** operating cost type (as tile lay, token lay etc.) */
    private OCType operatingCostType;

    /** suggested costs */
    private int suggestedCost;
    
    /** maximum costs */
    private int maximumCost;
    
    /** allow free entry */
    private boolean freeEntryAllowed;
    
    /* Postconditions */

    /** selected cash amount */
    private int operatingCost; 

   /**
    * Instantiates an operating costs action
    * 
    * @param pc Public Company
    */
   public OperatingCost(OCType ot, int ocCosts, boolean freeEntry) {
       
       super();

       operatingCostType = ot;
       suggestedCost = ocCosts;
       freeEntryAllowed = freeEntry;
       maximumCost = company.getCash();
   }
   
   public boolean isFreeEntryAllowed() {
       return freeEntryAllowed;
   }

   public int getAmount() {
       if (acted)
           return operatingCost;
       else
           return suggestedCost;
   }

   public void setAmount(int amount) {
       acted=true;
       operatingCost = amount;
   }
   public OCType getOCType(){
       return operatingCostType;
   }
   
    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof OperatingCost)) return false;
        OperatingCost a = (OperatingCost) action;
        return (a.company == this.company &&
                a.operatingCostType == this.operatingCostType &&
                a.suggestedCost == this.suggestedCost &&
                a.maximumCost == this.maximumCost
        );
    }
    
    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof OperatingCost)) return false;
        OperatingCost a = (OperatingCost) action;
        return (a.company == this.company &&
                a.operatingCostType == this.operatingCostType &&
                a.operatingCost == this.operatingCost
        );
    }
    
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("OperatingCost");
        if (!acted) {
            b.append(" (not acted)");
            if (company != null)
                b.append(", company="+company);
            b.append(", operatingCostType="+operatingCostType);
            b.append(", suggestedCost="+suggestedCost);
            b.append(", maximumCost="+maximumCost);
        } else {
            b.append(" (acted)");
            if (company != null)
                b.append(", company="+company);
            b.append(", operatingCostType="+operatingCostType);
            b.append(", operatingCost="+operatingCost);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(companyName))
                company = getCompanyManager().getPublicCompany(companyName);
    }
}
