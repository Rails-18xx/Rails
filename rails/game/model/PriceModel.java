package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompany;
import rails.game.StockSpaceI;
import rails.game.state.Item;
import rails.game.state.PriceMove;

// TODO: Requires a complete rewrite

public class PriceModel extends Model {

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
     * Creates an initialized PriceModel
     */
    public PriceModel create(PublicCompany parent){
        return new PriceModel().init(parent);
    }
    
    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public PriceModel init(Item parent){
        super.init(parent);
        if (parent instanceof PublicCompany) {
            this.company = (PublicCompany)parent;
        } else {
            throw new IllegalArgumentException("PriceModel init() only works for PublicCompanies");
        }
        return this;
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
            return new ViewUpdate(getText())
                    .addObject(ViewUpdate.BGCOLOUR, stockPrice.getColour());
        } else {
            return getText();
        }
    }

    @Override
    protected String getText() {
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
