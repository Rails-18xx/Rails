package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.Player;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
*/
public class RequestTurn extends PossibleAction {

    public static final long serialVersionUID = 1L;

    private String requestingPlayerName;

    public RequestTurn (Player player) {
        super(null); // not defined by an activity yet
        // Override player set by superclass
        if (player != null) {
            requestingPlayerName = player.getId();
        }
    }

    public String getRequestingPlayerName() {
        return requestingPlayerName;
    }
    
    
    @Override
    public String toMenu() {
        return LocalText.getText("RequestTurn", requestingPlayerName);
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        RequestTurn action = (RequestTurn)pa; 
        return Objects.equal(this.requestingPlayerName, action.requestingPlayerName);
        // no asAction attributes to be checked
    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("requestingPlayerName", requestingPlayerName)
                    .toString()
        ;
    }


    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        in.defaultReadObject();
    }
}
