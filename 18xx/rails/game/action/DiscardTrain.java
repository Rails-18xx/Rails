/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/DiscardTrain.java,v 1.1 2007/07/05 17:57:54 evos Exp $
 * 
 * Created on 20-May-2006
 * Change Log:
 */
package rails.game.action;

import java.util.List;

import rails.game.TrainI;

/**
 * @author Erik Vos
 */
public class DiscardTrain extends PossibleORAction {

	// Initial settings
    private List<TrainI> ownedTrains = null;
    
    // User settings
    private TrainI discardedTrain = null;
    
    public DiscardTrain (List<TrainI> trains) {
    	
        this.ownedTrains = trains;
    }
    
    public List<TrainI> getOwnedTrains() {
        return ownedTrains;
    }
    
    public void setDiscardedTrain (TrainI train) {
        discardedTrain = train;
    }
    
    public TrainI getDiscardedTrain() {
        return discardedTrain;
    }
    
	public String toString() {
		
		StringBuffer b = new StringBuffer();
        b.append("Discard train: ").append (company.getName());
        b.append (" owns");
        for (TrainI train : ownedTrains) {
            b.append(" ").append(train.getName());
        }
		return b.toString();
    }
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof DiscardTrain)) return false;
        DiscardTrain a = (DiscardTrain) action;
        return a.ownedTrains == ownedTrains
        	&& a.company == company;
    }

}
