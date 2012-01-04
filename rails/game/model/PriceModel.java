package rails.game.model;

import rails.game.*;
import rails.game.state.Item;
import rails.game.state.PriceMove;

// TODO: Requires a complete rewrite

public class PriceModel extends Model<String> {

    private StockSpaceI stockPrice = null;
    private PublicCompany company = null;
    private String name = null;

    /**
     * PriceModel is initialized with default id "PriceModel"
     */
    public PriceModel() {
        super("PriceModel");
    }

    /**
     * Initialization of a PriceModel only possible for a PublicCompany
     * @param company
     */
    public void init(PublicCompany company) {
        super.init(company);
        this.company = company;
    }
    
    /** 
     * This method throws an IllegalArgumentException as  works only for PublicCompanies
     */
    @Override
    public void init(Item parent){
        throw new IllegalArgumentException("PriceModel init() only works for PublicCompanies");
    }
    

    // TODO: This has to be changed
    public void setPrice(StockSpaceI price) {
        new PriceMove(this, stockPrice, price);
    }

    public StockSpaceI getPrice() {
        return stockPrice;
    }

    public PublicCompany getCompany() {
        return company;
    }

    // FIXME: This is a reference to the usage of ViewUpdate
    public Object getUpdate() {
        if (stockPrice != null) {
            return new ViewUpdate(getData())
                    .addObject(ViewUpdate.BGCOLOUR, stockPrice.getColour());
        } else {
            return getData();
        }
    }

    public String getData() {
        if (stockPrice != null) {
            return Bank.format(stockPrice.getPrice()) + " ("
                   + stockPrice.getName() + ")";
        }
        return "";
    }

    // StateI required methods
    public Object get() {
        return stockPrice;
    }

    public void setState(Object object) {
        if (object == null) {
            stockPrice = null;
            update();
        } else if (object instanceof StockSpaceI) {
            stockPrice = (StockSpaceI) object;
            update();
        } else {
            new Exception("Incompatible object type "
                          + object.getClass().getName()
                          + "passed to PriceModel " + name).printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

}
