package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.special.SpecialProperty;
import rails.game.specific._18AL.NamedTrainToken;
import rails.game.specific._18AL.NameTrains;

public class AssignNamedTrains extends PossibleAction {
	
	transient private NameTrains namedTrainsSpecialProperty;
	private int namedTrainsSpecialPropertyId;

	public AssignNamedTrains (NameTrains namedTrainsSpecialProperty) {
		this.namedTrainsSpecialProperty = namedTrainsSpecialProperty;
		this.namedTrainsSpecialPropertyId = namedTrainsSpecialProperty.getUniqueId();
	}
	
    public String toString () {
        StringBuffer b = new StringBuffer  ("AssignNamedTrains ");
        for (NamedTrainToken token : namedTrainsSpecialProperty.getTokens()) {
        	b.append(token.toString()).append(",");
        }
        b.deleteCharAt(b.length()-1);
        return b.toString();
    }

    /** Deserialize */
	private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
		if (namedTrainsSpecialPropertyId  > 0) {
			namedTrainsSpecialProperty 
				= (NameTrains) SpecialProperty.getByUniqueId (namedTrainsSpecialPropertyId);
		}
	}

	@Override
	public boolean equals(PossibleAction pa) {

		return pa instanceof AssignNamedTrains 
			&& ((AssignNamedTrains)pa).namedTrainsSpecialPropertyId 
				== namedTrainsSpecialPropertyId;
	}

}
