package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompany;
import rails.game.StockSpace;
import rails.game.state.GenericState;
import rails.game.state.Item;
import rails.game.state.Model;

public class PriceModel extends Model {

    // fields 
    private PublicCompany company = null;

    // states
    private final GenericState<StockSpace> stockPrice = GenericState.create();

    
    private PriceModel() {}

    public static PriceModel create(){
        return new PriceModel();
    }
    
    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public void init(Item parent, String id){
        super.checkedInit(parent, id, PublicCompany.class);
        stockPrice.init(this, "stockPrice");
    }
    
    public void setPrice(StockSpace price) {
        stockPrice.set(price);
    }

    public StockSpace getPrice() {
        return stockPrice.get();
    }

    public PublicCompany getCompany() {
        return company;
    }

    // FIXME: This is a reference to the usage of ViewUpdate
    // TODO: The color reference has to be taken care of, remove view update
//    public Object getUpdate() {
//        if (stockPrice != null) {
//            return new ViewUpdate(getText())
//                    .addObject(ViewUpdate.BGCOLOUR, stockPrice.getColour());
//        } else {
//            return getText();
//        }
//    }

    @Override
    public String toString() {
        if (stockPrice != null) {
            return Bank.format(stockPrice.get().getPrice()) + " ("
                   + stockPrice.get().getId() + ")";
        }
        return "";
    }

}
