/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/PriceModel.java,v 1.9 2010/01/31 22:22:29 macfreek Exp $*/
package rails.game.model;

import rails.game.*;
import rails.game.move.PriceMove;
import rails.game.state.StateI;

public class PriceModel extends ModelObject implements StateI {

    private StockSpaceI stockPrice = null;
    private PublicCompanyI company = null;
    private String name = null;

    public PriceModel(PublicCompanyI company, String name) {
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

    @Override
    public Object getUpdate() {
        if (stockPrice != null) {
            return new ViewUpdate(getText())
                    .addObject(ViewUpdate.BGCOLOUR, stockPrice.getColour());
        } else {
            return getText();
        }
    }

    @Override
    public String getText() {
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
