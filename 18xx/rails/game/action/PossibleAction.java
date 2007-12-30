/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleAction.java,v 1.10 2007/12/30 14:25:12 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.log4j.Logger;

import rails.game.GameManager;
import rails.game.Player;

/**
 * PossibleAction is the superclass of all classes that describe an allowed user action
 * (such as laying a tile or dropping a token on a specific hex, buying a train etc.).
 * @author Erik Vos
 */
/* Or should this be an interface? We will see. */
public abstract class PossibleAction implements Serializable {

	protected String playerName;
	protected int playerIndex;
    
    protected boolean acted = false;

    public static final long serialVersionUID = 3L;

	protected static Logger log = Logger.getLogger(PossibleAction.class.getPackage().getName());

    /**
     * 
     */
    public PossibleAction() {
    	
    	Player player = GameManager.getCurrentPlayer();
    	if (player != null) {
    		playerName = player.getName();
    		playerIndex = player.getIndex();
    	}
    }
    
    public String getPlayerName() {
    	return playerName;
    }
    
    public int getPlayerIndex() {
    	return playerIndex;
    }
    
    /** Set the name of the player who <b>executed</b> the action
     * (as opposed to the player who was <b>allowed</b> to do the action,
     * which is the one set in the constructor).
     * @param playerName
     */
    public void setPlayerName (String playerName) {
    	this.playerName = playerName;
    }
    
     public boolean hasActed() {
        return acted;
    }

    public void setActed() {
        this.acted = true;
    }

    public abstract boolean equals(PossibleAction pa);
     
     private void readObject(ObjectInputStream in) 
     throws IOException, ClassNotFoundException {
    	 in.defaultReadObject();
     }
}
