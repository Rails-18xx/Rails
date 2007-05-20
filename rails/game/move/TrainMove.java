/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/Attic/TrainMove.java,v 1.1 2007/05/20 17:54:52 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class TrainMove extends Move {
    
    TrainI train;
    Portfolio from;
    Portfolio to;
    
    /**
     * Create a generic TrainMove object.
     * @param train The train to be moved.
     * @param from The portfolio from which the train is removed (e.g. the Bank).
     * @param to The portfolio to which the train is moved (e.g. a PublicCompany).
     */
            
    public TrainMove (TrainI train, Portfolio from, Portfolio to) {
        
        this.train = train;
        this.from = from;
        this.to = to;
    }


    public boolean execute() {

        Portfolio.transferTrain(train, from, to);
        return true;
    }  

    public boolean undo() {
        
        Portfolio.transferTrain(train, to, from);
        return true;
    }
    
    public String toString() {
        if (train == null) log.error ("Train is null");
        if (from == null) log.error ("From is null");
        if (to == null) log.error ("To is null");        
        return "TrainMove: "+train.getName()
        	+ " from " + from.getName()
        	+ " to " + to.getName();
   }

}
