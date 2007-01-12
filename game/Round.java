/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Round.java,v 1.9 2007/01/12 22:51:28 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package game;

import game.action.PossibleActions;

import java.util.List;

import org.apache.log4j.Logger;

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
     * @see game.RoundI#getCurrentPlayer()
     */
    public Player getCurrentPlayer() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see game.RoundI#getHelp()
     */
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see game.RoundI#getSpecialProperties()
     */
    public List getSpecialProperties() {
        // TODO Auto-generated method stub
        return null;
    }

}
