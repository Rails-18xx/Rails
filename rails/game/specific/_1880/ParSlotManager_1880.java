package rails.game.specific._1880;

/**
 * @author Michael Alexander
 * 
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.game.GameManagerI;
import rails.game.PublicCompanyI;
import rails.game.state.StringState;

public class ParSlotManager_1880 {

    private static final Map<Integer, Integer> SLOTS_PRICE_MAP = createMap();
    private static Map<Integer, Integer> createMap() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result.put((i*4+j), (100-(10*i)));
            }
        }
        return Collections.unmodifiableMap(result);
    }
    private final static int NUM_PAR_SLOTS = 16;
    
    ParSlotModel companies[] = new ParSlotModel[16];
    private GameManagerI gameManager;
    StringState lastTrainStates[] = new StringState[16];
    
    
    public ParSlotManager_1880(GameManagerI gameManager) {
        this.gameManager = gameManager;
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            companies[i] = new ParSlotModel("ParSlot_" + i);
            lastTrainStates[i] = new StringState("TrainState_" + i, " ");
        }
    }

    public List<PublicCompanyI> getCompaniesInParSlotOrder() {
        List<PublicCompanyI> results = new ArrayList<PublicCompanyI>();
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            if (companies[i].isEmpty() == false) {
                results.add(gameManager.getCompanyManager().getPublicCompany(companies[i].getText()));
            }
        }
        return results;
    }

    public void setCompanyAtSlot(PublicCompany_1880 company, int parSlotIndex) {
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

    public StringState getLastTrainStateAtSlot(int i) {
        return lastTrainStates[i];
    }
    
    public void trainPurchased(PublicCompany_1880 company) {
        for (int i = 0; i < NUM_PAR_SLOTS; i++) {
            if (companies[i].get() == company) {
                lastTrainStates[i].set("X");
            } else {
                lastTrainStates[i].set(" ");
            }
        }
    }
}
