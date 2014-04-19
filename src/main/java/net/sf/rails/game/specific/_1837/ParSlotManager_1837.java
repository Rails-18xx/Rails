package net.sf.rails.game.specific._1837;

/**
 * @author Michael Alexander
 * 
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.model.ParSlotModel;


public class ParSlotManager_1837 extends RailsManager{

    private static final Map<Integer, Integer> SLOTS_PRICE_MAP = createMap();
    
    
    private static Map<Integer, Integer> createMap() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        
        int[] parSlotPrices = { 104, 97, 91, 85, 80, 75, 70}; 
        
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                result.put((i*2+j), parSlotPrices[i]);
            }
        }
        return Collections.unmodifiableMap(result);
    }
    private final static int NUM_PAR_SLOTS = 14;
    
    ParSlotModel companies[] = new ParSlotModel[14];
        
    
    public ParSlotManager_1837(RailsRoot parent, String id) {
        super(parent, id);
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            companies[i] = new ParSlotModel(null, "ParSlot_" + i);
            }
    }

    public List<PublicCompany> getCompaniesInParSlotOrder() {
        List<PublicCompany> results = new ArrayList<PublicCompany>();
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            if (companies[i].isEmpty() == false) {
                results.add(getRoot().getCompanyManager().getPublicCompany(companies[i].getText()));
            }
        }
        return results;
    }

    public void setCompanyAtSlot(PublicCompany company, int parSlotIndex) {
        companies[parSlotIndex].setCompany(company);
    }
    
    public ParSlotModel getModelAtSlot(int slot) {
        return companies[slot];
    }
    
    public Integer[] getAvailableSlots(int maximumPrice) {
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            if ((companies[i].isEmpty()) && (SLOTS_PRICE_MAP.get(i) <= maximumPrice)) {
                slots.add(i);
            }
        }
        return slots.toArray(new Integer[slots.size()]);
    }
    
    public Integer[] getAvailablePrices(int maximumPrice) {
        List<Integer> prices = new ArrayList<Integer>();
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            if ((companies[i].isEmpty() == true) && (SLOTS_PRICE_MAP.get(i) <= maximumPrice) && 
                    (prices.contains(SLOTS_PRICE_MAP.get(i)) == false)) {
                prices.add(SLOTS_PRICE_MAP.get(i));
            }
        }
        return prices.toArray(new Integer[prices.size()]);
    }

    public static int getPriceForSlot(int i) {
        return SLOTS_PRICE_MAP.get(i);
    }

    public static int[] filterByPrice(int[] possibleParSlotIndices, int selectedPrice) {
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < possibleParSlotIndices.length; i++) {
            if (SLOTS_PRICE_MAP.get(possibleParSlotIndices[i]) == selectedPrice) {
                slots.add(possibleParSlotIndices[i]);
            }
        }
        
        int[] results = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            results[i] = slots.get(i);
        }
        return results;
    }

}
