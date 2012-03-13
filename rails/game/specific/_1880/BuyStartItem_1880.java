/**
 * 
 */
package rails.game.specific._1880;

import rails.game.StartItem;
import rails.game.action.BuyStartItem;

import java.util.BitSet;

import rails.game.PublicCompanyI;
import rails.game.action.PossibleAction;
/**
 * @author Martin Brumm
 *
 */
public class BuyStartItem_1880 extends BuyStartItem {

    public static final long serialVersionUID = 1L;
    
    private BitSet buildingRights;
    
    private BitSet associatedBuildingRight;
    /**
     * @param startItem
     * @param price
     * @param selected
     * @param setSharePriceOnly
     */
    public BuyStartItem_1880(StartItem startItem, int price, boolean selected,
            boolean setSharePriceOnly) {
        
       
        super(startItem, price, selected, setSharePriceOnly);
        // TODO Auto-generated constructor stub
        this.buildingRights = new BitSet(4);
    }


    public BuyStartItem_1880(StartItem startItem, int price, boolean selected,
           boolean setSharePriceOnly, BitSet buildingRight) {

       super(startItem,price,selected,setSharePriceOnly);
       this.buildingRights = buildingRight;
      
        PublicCompanyI company;
        if ((company = startItem.needsPriceSetting()) != null) {
            sharePriceToSet = true;
            companyNeedingSharePrice = company.getName();
       }
    }

    /**
     * @param startItem
     * @param price
     * @param selected
     */
    public BuyStartItem_1880(StartItem startItem, int price, boolean selected) {
        super(startItem, price, selected);
        // TODO Auto-generated constructor stub
        this.buildingRights = new BitSet(5);
    }
    
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof BuyStartItem_1880)) return false;
         BuyStartItem_1880 a = (BuyStartItem_1880) action;
         return a.startItem == startItem && a.itemIndex == itemIndex
                && a.getPrice() == getPrice() && a.buildingRights == buildingRights;
     }
     public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof BuyStartItem_1880)) return false;
        BuyStartItem_1880 a = (BuyStartItem_1880) action;
     return a.equalsAsOption(this)
              && a.getAssociatedSharePrice() == getAssociatedSharePrice()
              && a.associatedBuildingRight == associatedBuildingRight;
    }
   public String toString() {
       StringBuffer b = new StringBuffer();
       b.append("BuyStartItem_1880 ").append(startItemName).append(" price=").append(
           getPrice()).append(" selected=").append(isSelected());
  
        if (sharePriceToSet) {
              b.append(" shareprice=").append(getAssociatedSharePrice()).append(" BuildingRight=").append(buildingRightToString(buildingRights)).append(
                     " for company " + companyNeedingSharePrice);
         }
         return b.toString();
      }
  
     /**
      * @return the buildingRights
      */
    public BitSet getAssociatedBuildingRight() {
         return associatedBuildingRight;
     }
    /**
      * @param buildingRights the buildingRights to set
      */
     public void setAssociatedBuildingRight(BitSet buildingRight ) {
         this.associatedBuildingRight = buildingRight;
     }
     
     public void setAssociatedBuildingRight(String buildingRightString ) {
         BitSet buildingRight = new BitSet();
         
        if (buildingRightString == "A") {
             buildingRight.set(0);
         } else  if (buildingRightString == "B") {
             buildingRight.set(1);
         } else  if (buildingRightString == "C") {
             buildingRight.set(2);
         } else  if (buildingRightString == "D") {
             buildingRight.set(3);
         } else  if (buildingRightString == "A+B") {
             buildingRight.set(0);
             buildingRight.set(1);
         } else  if (buildingRightString == "A+B+C") {
             buildingRight.set(0);
             buildingRight.set(1);
             buildingRight.set(2);
        } else  if (buildingRightString == "B+C") {
             buildingRight.set(1);
             buildingRight.set(2);
         } else  if (buildingRightString == "B+C+D") {
             buildingRight.set(1);
             buildingRight.set(2);
             buildingRight.set(3);
         } else  if (buildingRightString == "C+D") {
            buildingRight.set(2);
            buildingRight.set(3);
         }
         
        associatedBuildingRight = buildingRight;
     }
   
     public String buildingRightToString (BitSet buildingRight){
         String buildingRightString = null;
         
       if (! buildingRight.isEmpty()){
          if (buildingRight.get(0)== true) {
               buildingRightString = "A";
                if (buildingRight.get(1) == true) {
                     buildingRightString = "A+B";
                     if (buildingRight.get(2) == true) {
                         buildingRightString = "A+B+C";
                     }
                 }
             }
             else if (buildingRight.get(1) == true) {
                     buildingRightString = "B";
                     if (buildingRight.get(2) == true) {
                         buildingRightString = "B+C";
                       if (buildingRight.get(3) == true){
                            buildingRightString = "B+C+D";
                       }
                    }
             }
            else if (buildingRight.get(2) == true){
               buildingRightString = "C";
                 if (buildingRight.get(3) == true){
                     buildingRightString = "C+D";
                 }
             }
             else if (buildingRight.get(3) == true){
                buildingRightString= "D";
            }
        return buildingRightString;
        }
        return "None";
    }
     
}