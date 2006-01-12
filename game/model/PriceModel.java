/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/PriceModel.java,v 1.4 2006/01/12 22:13:37 evos Exp $
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
    
    private StockSpaceI stockPrice = null;
    
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
        if (stockPrice != null) {
            return Bank.format(stockPrice.getPrice()) + " ("+stockPrice.getName()+")"	;
        }
        return "";
    }

}
