package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompany;
import rails.game.StockSpace;
import rails.game.state.GenericState;
import rails.game.state.Model;

public final class PriceModel extends Model {

    // FIXME: Remove duplication of company and parent
    private final PublicCompany company;

    private final GenericState<StockSpace> stockPrice = GenericState.create(this, "stockPrice");
    
    private PriceModel(PublicCompany parent, String id) {
        super(parent, id);
        company = parent;
    }

    public static PriceModel create(PublicCompany parent, String id){
        return new PriceModel(parent, id);
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
