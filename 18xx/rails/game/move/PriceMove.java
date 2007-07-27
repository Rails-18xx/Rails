/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/PriceMove.java,v 1.3 2007/07/27 22:05:14 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package rails.game.move;

//import rails.game.PublicCompanyI;
//import rails.game.StockMarket;
import rails.game.StockSpaceI;
import rails.game.model.PriceModel;

/**
 * @author Erik Vos
 */
public class PriceMove extends Move {
    
    protected StockSpaceI from, to;
    protected PriceModel price;
   // protected PublicCompanyI company;
    
    public PriceMove (PriceModel price, StockSpaceI from, StockSpaceI to) {
        this.from = from;
        this.to = to;
        this.price = price;
        //this.company = price.getCompany();
        MoveSet.add (this);
    }

    public boolean execute() {
        price.setState(to);
        //StockMarket.getInstance().processMove (company, from, to);
        return true;
    }

    /* (non-Javadoc)
     * @see rails.rails.game.action.Move#undo()
     */
    public boolean undo() {
        price.setState(from);
        //StockMarket.getInstance().processMove (company, to, from);
        return true;
    }
    
    public Object getObject() {return price;}
    
    public String toString () {
        return "PriceMove: "+price.getName()
            +" from "+from+" to "+to;
    }

}
