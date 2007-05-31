/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleAction.java,v 1.3 2007/05/31 20:49:52 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import org.apache.log4j.Logger;

import rails.game.GameManager;
import rails.game.Player;

/**
 * PossibleAction is the superclass of all classes that describe an allowed user action
 * (such as laying a tile or dropping a token on a specific hex, buying a train etc.).
 * @author Erik Vos
 */
/* Or should this be an interface? We will see. */
public abstract class PossibleAction {

	protected String playerName;
	protected int playerIndex;
	
	protected static Logger log = Logger.getLogger(PossibleAction.class.getPackage().getName());

    /**
     * 
     */
    public PossibleAction() {
    	
    	Player player = GameManager.getCurrentPlayer();
    	playerName = player.getName();
    	playerIndex = player.getIndex();
    }
    
    public String getPlayerName() {
    	return playerName;
    }
    
    public int getPlayerIndex() {
    	return playerIndex;
    }

}
