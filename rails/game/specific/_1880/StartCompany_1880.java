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

    private int[] possibleParSlotIndices;
    
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
    public StartCompany_1880(PublicCompanyI company, int[] startPrices) {
        this(company, startPrices, 1);        
    }
    
    public void setBuildingRights(String buildingRightsString) {
        this.buildingRightsString = buildingRightsString;
    }
    
    public String getBuildingRights() {
        return buildingRightsString;
    }

    /* (non-Javadoc)
     * @see rails.game.action.StartCompany#setStartPrice(int)
     */
    public void setStartPrice(int startPrice) {
        super.setStartPrice(startPrice);
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
//        ((GameManager_1880) gameManager).getParSlots().setCompanyAtSlot(this.getCompany(), index);
    }

    public int[] getPossibleParSlotIndices() {
        return possibleParSlotIndices;
    }

    public void setPossibleParSlotIndices(int[] possibleParSlotIndices) {
        this.possibleParSlotIndices = possibleParSlotIndices;
    }
}
