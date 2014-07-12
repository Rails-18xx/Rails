package rails.game.specific._1880;

import rails.game.action.BuyStartItem;

import java.util.BitSet;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

// FIXME: Is this class every used, a first search did not show any calls?
public class BuyStartItem_1880 extends BuyStartItem {

    public static final long serialVersionUID = 1L;

    private BitSet buildingRights;

    private BitSet associatedBuildingRight;

    private int parSlotIndex = 0;
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

        PublicCompany company;
        if ((company = startItem.needsPriceSetting()) != null) {
            sharePriceToSet = true;
            companyNeedingSharePrice = company.getLongName();
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


    public void setParSlotIndex(int index) {
        parSlotIndex = index;        
    }

    public int getParSlotIndex() {
        return parSlotIndex;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        BuyStartItem_1880 action = (BuyStartItem_1880)pa; 
        boolean options = Objects.equal(this.buildingRights, action.buildingRights);
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.associatedBuildingRight, action.associatedBuildingRight) 
                && Objects.equal(this.parSlotIndex, action.parSlotIndex)
        ;

    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                .addToString("buildingRights", buildingRights)
                .addToStringOnlyActed("associatedBuildingRight", associatedBuildingRight)
                .addToStringOnlyActed("pasSlotIndex", parSlotIndex)
                .toString()
        ;
    }

}