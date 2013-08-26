package rails.game.specific._1880;

/**
 * @author Michael Alexander
 * 
 */
import rails.game.PublicCompanyI;

public class ParSlot_1880 {
    private int index = -1;
    private int price = -1;
    private PublicCompanyI company = null;
    
    public ParSlot_1880(int index, int price) {
        this.index = index;
        this.price = price;
    }
    
    public void setCompany(PublicCompanyI company) {
        this.company = company;
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getPrice() {
        return price;
    }
    
    public PublicCompanyI getCompany() {
        return company;
    }

}
