/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/PriceModel.java,v 1.9 2010/01/31 22:22:29 macfreek Exp $*/
package rails.game.model;

import rails.game.*;
import rails.game.state.PriceMove;

// TODO: Requires a complete rewrite

public class PriceModel extends AbstractModel<String> {

    private StockSpaceI stockPrice = null;
    private PublicCompanyI company = null;
    private String name = null;

    public PriceModel(PublicCompanyI company, String name) {
        super(company, name);
        this.company = company;
        this.name = name;
    }

    public void setPrice(StockSpaceI price) {
        new PriceMove(this, stockPrice, price);
    }

    public StockSpaceI getPrice() {
        return stockPrice;
    }

    public PublicCompanyI getCompany() {
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
            notifyModel();
        } else if (object instanceof StockSpaceI) {
            stockPrice = (StockSpaceI) object;
            notifyModel();
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
