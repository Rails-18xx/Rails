/**
 * 
 */
package rails.game.specific._1880;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import rails.game.CompanyManagerI;
import rails.game.PublicCompanyI;
import rails.game.StockSpace;
import rails.game.StockSpaceI;
import rails.game.action.StartCompany;

/**
 * @author Martin
 *
 */
public class StartCompany_1880 extends StartCompany {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    BitSet buildingRight = new BitSet(4);
    BitSet savedBuildingRight = new BitSet(4);
    
    /**
     * @param company
     * @param prices
     * @param maximumNumber
     */
    public StartCompany_1880(PublicCompanyI company, int[] prices,
            int maximumNumber) {
        super(company, prices, maximumNumber);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param company
     * @param startPrice
     */
    public StartCompany_1880(PublicCompanyI company, int[] startPrice) {
        this(company, startPrice, 1);
        // TODO Auto-generated constructor stub
        
    }

    /**
     * @param company
     * @param price
     * @param maximumNumber
     */
    public StartCompany_1880(PublicCompanyI company, int price,
            int maximumNumber) {
        super(company, price, maximumNumber);
        StockSpaceI parPrice=gameManager.getStockMarket().getStartSpace(price);
        this.getCompany().setParSpace(parPrice);
    }

    /**
     * @param company
     * @param price
     */
    public StartCompany_1880(PublicCompanyI company, int price) {
        super(company, price);
        // TODO Auto-generated constructor stub
    }

    
    public void setBuildingRight(PublicCompany_1880 company, String buildingRightString ) {
      
        
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
        
       company.setBuildingRights( buildingRight);
       company.setRight("BuildingRight", buildingRightString);
    }

    
    public void setPresidentPercentage(PublicCompany_1880 company, int percentage) {
        company.setPresidentShares(percentage);
    }

    /* (non-Javadoc)
     * @see rails.game.action.StartCompany#getStartPrices()
     */
  
    public int[] getStartPrices() {
        int [] startPrices2;
        List<Integer> startPrices_new = new ArrayList<Integer>();
        // TODO Auto-generated method stub
        // make sure that all exhausted price Slots will not be returned as valid prices anymore...
        startPrices2 = super.getStartPrices();
        for (int e = 0 ; e < startPrices2.length ; e++)
        {
            if (  ((StockMarket_1880) gameManager.getStockMarket()).getParSlot(startPrices2[e])== true) //free slot found
            {
                startPrices_new.add(startPrices2[e]);
            }
        }
        int[] startPrices2_new = new int [startPrices_new.size()];
        for ( int i = 0; i < startPrices2_new.length; i++)
            startPrices2_new[i] = startPrices_new.get(i).intValue();
        return startPrices2_new;
    }

    /* (non-Javadoc)
     * @see rails.game.action.StartCompany#setStartPrice(int)
     */
    public void setStartPrice(int startPrice, int index) {
        StockSpaceI parPrice=gameManager.getStockMarket().getStartSpace(startPrice);
        this.getCompany().setParSpace(parPrice);
        ((StockMarket_1880) gameManager.getStockMarket()).setParSlot(index);
    }
    
    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        in.defaultReadObject();

        if (!buildingRight.isEmpty()) {
            savedBuildingRight = (BitSet) buildingRight.clone();
        }
        
    }

    public String buildingRightToString(BitSet buildingRight2) {
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

    public void setOperatingSlot(int index) {
        ((PublicCompany_1880) this.getCompany()).setOperationSlotIndex(index);
        
    }
}
