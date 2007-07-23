/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/DiscardTrain.java,v 1.2 2007/07/23 19:59:16 evos Exp $
 * 
 * Created on 20-May-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.Train;
import rails.game.TrainI;

/**
 * @author Erik Vos
 */
public class DiscardTrain extends PossibleORAction {

	// Initial settings
    transient private List<TrainI> ownedTrains = null;
    private String[] ownedTrainsUniqueIds;
    
    // User settings
    transient private TrainI discardedTrain = null;
    private String discardedTrainUniqueId;
    
    
    public DiscardTrain (List<TrainI> trains) {
    	
        this.ownedTrains = trains;
        this.ownedTrainsUniqueIds = new String[trains.size()];
        for (int i=0; i<trains.size(); i++) {
            ownedTrainsUniqueIds[i] = trains.get(i).getName(); // TODO: Must be replaced by unique id
        }
    }
    
    public List<TrainI> getOwnedTrains() {
        return ownedTrains;
    }
    
    public void setDiscardedTrain (TrainI train) {
        discardedTrain = train;
        discardedTrainUniqueId = train.getName(); // TODO: Must be replaced by unique id
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
    
    /** Deserialize */
	private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
		discardedTrain = Train.getByUniqueId(discardedTrainUniqueId);
		if (ownedTrainsUniqueIds != null
				&& ownedTrainsUniqueIds.length > 0) {
			ownedTrains = new ArrayList<TrainI>();
			for (int i=0; i<ownedTrainsUniqueIds.length; i++) {
				ownedTrains.add (Train.getByUniqueId(ownedTrainsUniqueIds[i]));
			}
		}
	}


}
