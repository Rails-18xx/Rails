/**
 * 
 */
package rails.game.specific._1880;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rails.game.PublicCompanyI;
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

    protected String buildingRightsString;
    private int parSlotIndex = 0;
    
    /**
     * @param company
     * @param prices
     * @param maximumNumber
     */
    public StartCompany_1880(PublicCompanyI company, int[] prices,
            int maximumNumber) {
        super(company, prices, maximumNumber);
    }

    /**
     * @param company
     * @param startPrice
     */
    public StartCompany_1880(PublicCompanyI company, int[] startPrice) {
        this(company, startPrice, 1);        
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
    }

    
    public void setBuildingRights(String buildingRightsString) {
        this.buildingRightsString = buildingRightsString;
    }
    
    public String getBuildingRights() {
        return buildingRightsString;
    }

    /* (non-Javadoc)
     * @see rails.game.action.StartCompany#getStartPrices()
     */
    public int[] getStartPrices() {
        int [] startPrices2;
        List<Integer> startPrices_new = new ArrayList<Integer>();
        startPrices2 = super.getStartPrices();
        for (int e = 0 ; e < startPrices2.length ; e++)
        {
            if (((GameManager_1880) gameManager).getParSlots().freeSlotAtPrice(startPrices2[e])) //free slot found
            {
                startPrices_new.add(startPrices2[e]);
            }
        }
        int[] startPrices2_new = new int [startPrices_new.size()];
        for ( int i = 0; i < startPrices2_new.length; i++)
            startPrices2_new[i] = startPrices_new.get(i).intValue();
        return startPrices2_new;
    }
    
    public List<ParSlot_1880> getStartParSlots() {
        List<ParSlot_1880> startParSlots = new ArrayList<ParSlot_1880>();
        int []startPrices = super.getStartPrices();
        Integer []startPrices2 = new Integer[startPrices.length];
        ParSlots_1880 parSlots = ((GameManager_1880) gameManager).getParSlots();
             
        for (int i = 0; i < startPrices.length ; i++) {
            startPrices2[i] = startPrices[i];
        }
        Arrays.sort(startPrices2, Collections.reverseOrder());
        
        for (int i = 0; i < startPrices2.length ; i++) {
            List<ParSlot_1880> emptySlotsAtThisPrice = parSlots.getEmptyParSlotsAtPrice(startPrices2[i]);
            for (ParSlot_1880 slot : emptySlotsAtThisPrice) {
                startParSlots.add(slot);
            }
        }
        return startParSlots;
    }

    /* (non-Javadoc)
     * @see rails.game.action.StartCompany#setStartPrice(int)
     */
    public void setStartPrice(int startPrice, int index) {
        StockSpaceI parPrice=gameManager.getStockMarket().getStartSpace(startPrice);
        this.getCompany().setParSpace(parPrice);
    }
    
    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {
        in.defaultReadObject();
    }
    
    public int getParSlotIndex() {
        return parSlotIndex;
    }
    
    public void setParSlotIndex(int index) {
        parSlotIndex = index;
        ((GameManager_1880) gameManager).getParSlots().setCompanyAtSlot(this.getCompany(), index);
    }
}
