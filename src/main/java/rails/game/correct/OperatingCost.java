package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * OR action for no map mode 
 * mirrors operating actions like tile and token lays, but
 * only changes the cash position of the public company

 * Rails 2.0: Updated equals and toString methods
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
   protected boolean equalsAs(PossibleAction pa, boolean asOption) {
       // identity always true
       if (pa == this) return true;
       //  super checks both class identity and super class attributes
       if (!super.equalsAs(pa, asOption)) return false; 

       // check asOption attributes
       OperatingCost action = (OperatingCost) pa;
       boolean options = Objects.equal(this.operatingCostType, action.operatingCostType)
               && Objects.equal(this.suggestedCost, action.suggestedCost)
               && Objects.equal(this.maximumCost, action.maximumCost)
               && Objects.equal(this.freeEntryAllowed, action.freeEntryAllowed)
       ;
       
       // finish if asOptions check
       if (asOption) return options;
       
       // check asAction attributes
       return options
               && Objects.equal(this.operatingCost, action.operatingCost)
       ;
    }
    
   @Override
   public String toString() {
       return super.toString() + 
               RailsObjects.stringHelper(this)
                   .addToString("operatingCostType", operatingCostType)
                   .addToString("suggestedCost", suggestedCost)
                   .addToString("maximumCost", maximumCost)
                   .addToString("freeEntryAllowed", freeEntryAllowed)
                   .addToStringOnlyActed("operatingCost", operatingCost)
                   .toString()
       ;
   }
   
    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(companyName))
                company = getCompanyManager().getPublicCompany(companyName);
    }
}
