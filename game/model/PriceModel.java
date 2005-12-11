/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/PriceModel.java,v 1.1 2005/12/11 00:03:36 evos Exp $
 * 
 * Created on 08-Dec-2005
 * Change Log:
 */
package game.model;

import game.Bank;
import game.StockSpaceI;

/**
 * @author Erik Vos
 */
public class PriceModel extends ModelObject {
    
    private StockSpaceI stockPrice;
    
    public PriceModel (StockSpaceI price) {
        this.stockPrice = price;
    }
    
    public void setPrice (StockSpaceI price) {
        stockPrice = price;
        notifyViewObjects();
    }
    
    public StockSpaceI getPrice () {
        return stockPrice;
    }
    
    public String toString() {
        return Bank.format(stockPrice.getPrice());
    }

}
