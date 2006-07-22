/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/PriceMove.java,v 1.2 2006/07/22 22:51:53 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package game.action;

//import game.PublicCompanyI;
//import game.StockMarket;
import game.StockSpaceI;
import game.model.PriceModel;

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
    }

    public boolean execute() {
        price.setState(to);
        //StockMarket.getInstance().processMove (company, from, to);
        return true;
    }

    /* (non-Javadoc)
     * @see game.action.Move#undo()
     */
    public boolean undo() {
        price.setState(from);
        //StockMarket.getInstance().processMove (company, to, from);
        return true;
    }
    
    public Object getObject() {return price;}
    
    public String toString () {
        return "PriceMove: "+price.getCompany().getName()
            +" from "+from+" to "+to;
    }

}
