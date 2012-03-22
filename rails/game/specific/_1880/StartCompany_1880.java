/**
 * 
 */
package rails.game.specific._1880;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.BitSet;

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
        // TODO Auto-generated constructor stub
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
        BitSet buildingRight = new BitSet(5);
        
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
    @Override
    public int[] getStartPrices() {
        // TODO Auto-generated method stub
        return super.getStartPrices();
    }

    /* (non-Javadoc)
     * @see rails.game.action.StartCompany#setStartPrice(int)
     */
    @Override
    public void setStartPrice(int startPrice) {
        // TODO Auto-generated method stub
        price = startPrice;
        StockSpaceI parPrice=gameManager.getStockMarket().getStartSpace(startPrice);
        this.getCompany().setParSpace(parPrice);
        ((StockMarket_1880) gameManager.getStockMarket()).setParSlot(startPrice);
    }
    
    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        in.defaultReadObject();

        CompanyManagerI cmgr = getCompanyManager();
        
    }
}
