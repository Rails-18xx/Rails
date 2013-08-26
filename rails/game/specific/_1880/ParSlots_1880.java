package rails.game.specific._1880;

/**
 * @author Michael Alexander
 * 
 */

import java.util.ArrayList;
import java.util.List;

import rails.game.PublicCompanyI;

public class ParSlots_1880 {

    private List<ParSlot_1880> parSlots = new ArrayList<ParSlot_1880>();
        
    public ParSlots_1880() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                parSlots.add(new ParSlot_1880(i*4+j, 100-(10*i)));
            }
        }
    }
    
    public List<ParSlot_1880> getEmptyParSlotsAtPrice(int price) {
        List<ParSlot_1880> emptySlots = new ArrayList<ParSlot_1880>();
        for (ParSlot_1880 slot : parSlots) {
            if ((slot.getCompany() == null) && (slot.getPrice() == price)) {
                emptySlots.add(slot);
            }
        }
        return emptySlots;
    }
        
    public boolean freeSlotAtPrice(int price) {
        for (ParSlot_1880 slot : parSlots) {
            if ((slot.getCompany() == null) && (slot.getPrice() == price)) {
                return true;
            }
        }
        return false;
    }

    public void setCompanyAtSlot(PublicCompanyI company, int index) {
        for (ParSlot_1880 slot : parSlots) {
            if (slot.getIndex() == index) {
                slot.setCompany(company);
                break;
            }
        }
    }
    
    public List<PublicCompanyI> getCompaniesInOperatingOrder() {
        List<PublicCompanyI> companies = new ArrayList<PublicCompanyI>();
        for (ParSlot_1880 slot : parSlots) {
            if (slot.getCompany() != null) {
                companies.add(slot.getCompany());
            }
        }
        return companies;
    }    
}
