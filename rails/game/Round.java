/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Round.java,v 1.4 2007/07/16 20:40:19 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game;


import java.util.List;

import org.apache.log4j.Logger;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.special.SpecialPropertyI;

/**
 * @author Erik Vos
 */
public class Round implements RoundI {
    
    protected PossibleActions possibleActions = PossibleActions.getInstance();

	protected static Logger log = Logger.getLogger(Round.class.getPackage().getName());

    /**
     * 
     */
    public Round() {
        super();
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see rails.game.RoundI#getCurrentPlayer()
     */
    public Player getCurrentPlayer() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see rails.game.RoundI#getHelp()
     */
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see rails.game.RoundI#getSpecialProperties()
     */
    public List<SpecialPropertyI> getSpecialProperties() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public boolean process (PossibleAction action) {
    	return true;
    }
    
    /** Default version, does nothing.
     * Subclasses should override this method with a real version.
     * @return
     */
    public boolean setPossibleActions () {
        return false;
    }

    public String toString () {
        return getClass().getName().replaceAll(".*\\.", "");
    }
    
}
