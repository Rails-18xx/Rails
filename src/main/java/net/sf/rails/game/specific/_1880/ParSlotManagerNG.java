package net.sf.rails.game.specific._1880;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsManager;

/**
 * The ParSlotManager stores the available ParSlots
 */
public class ParSlotManagerNG extends RailsManager {

    private static final ImmutableList<Integer> PAR_PRICES = ImmutableList.of(100, 90, 80, 70);
    private static final int nb_slots = 4;
    
    private final ImmutableList<ParSlot> parSlots;
    
    protected ParSlotManagerNG(GameManager parent, String id) {
        super(parent, id);
 
        // create parSlots
        ImmutableList.Builder<ParSlot> parSlotsBuilder = ImmutableList.builder();
        int sequence = 0;
        for (int price:PAR_PRICES) {
            for (int s=1; s<= nb_slots; s++) {
                String slotId = String.valueOf(price) + "_" + String.valueOf(s);
                ParSlot slot = new ParSlot(this, slotId, price, sequence);
                parSlotsBuilder.add(slot);
                sequence++;
            }
        }
        parSlots = parSlotsBuilder.build();
    }
    
    public List<ParSlot> getParSlots() {
        return parSlots;
    }
    
    public List<PublicCompany> getCompaniesInParSlotOrder() {
        ImmutableList.Builder<PublicCompany> companies = ImmutableList.builder();
        for (ParSlot slot:parSlots) {
            if (!slot.isEmpty()) {
                companies.add(slot.getCompany().value());
            }
        }
        return companies.build();
    }
    

    public void setCompanyAtIndex(PublicCompany company, int parSlotIndex) {
        parSlots.get(parSlotIndex).setCompany(company);
    }
    
    public void getSlotAtIndex(int parSlotIndex) {
        parSlots.get(parSlotIndex);
    }

    private List<ParSlot> getAvailableSlots(int maximumPrice) {
        ImmutableList.Builder<ParSlot> availableSlots = ImmutableList.builder();
        for (ParSlot slot:parSlots) {
            if (slot.isEmpty() && slot.getPrice() <= maximumPrice) {
                availableSlots.add(slot);
            }
        }
        return availableSlots.build();
    }
    
    public List<Integer> getAvailaibleIndices(int maximumPrice) {
        ImmutableList.Builder<Integer> indices = ImmutableList.builder();
        for (ParSlot slot:getAvailableSlots(maximumPrice)) {
            indices.add(slot.getIndex());
        }
        return indices.build();
    }

    public List<Integer> getAvailablePrices(int maximumPrice) {
        List<Integer> prices = Lists.newArrayList();
        for (ParSlot slot:getAvailableSlots(maximumPrice)) {
            if (!prices.contains(slot.getPrice())) {
                prices.add(slot.getPrice());
            }
        }
        return prices;
    }
    
    public List<ParSlot> filterByPrice(int[] possibleParSlotIndices, int selectedPrice) {
        ImmutableList.Builder<ParSlot> filterSlots = ImmutableList.builder();
        for (int index:possibleParSlotIndices) {
            ParSlot slot = parSlots.get(index);
            if (slot.getPrice() == selectedPrice) {
                filterSlots.add(slot);
            }
        }
        return filterSlots.build();
    }
    
    public void trainPurchased(PublicCompany company) {
        for (ParSlot slot:parSlots) {
            if (slot.getCompany().value() == company) {
                slot.getLastTrain().set("X");
            } else {
                slot.getLastTrain().set("");
            }
        }
    }


        
    
    
    

    
    
    
}
