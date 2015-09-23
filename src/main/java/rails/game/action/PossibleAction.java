package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.round.Activity;
import net.sf.rails.game.state.ChangeAction;
import net.sf.rails.game.state.ChangeActionOwner;
import net.sf.rails.util.GameLoader.RailsObjectInputStream;
import net.sf.rails.util.RailsObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;


/**
 * PossibleAction is the superclass of all classes that describe an allowed user
 * action (such as laying a tile or dropping a token on a specific hex, buying a
 * train etc.).
 * 
 * Rails 2.0: Added updated equals and toString methods 
 */

// TODO (Rails2.0): Replace this with a new XML version

public abstract class PossibleAction implements ChangeAction, Serializable {

    protected String playerName;
    protected int playerIndex;
    transient protected Player player;

    protected boolean acted = false;
    
    transient protected RailsRoot root;
    transient protected Activity activity;

    public static final long serialVersionUID = 3L;

    protected static Logger log =
            LoggerFactory.getLogger(PossibleAction.class);

    // TODO: Replace this by a constructor argument for the player
    public PossibleAction(Activity activity) {
        root = RailsRoot.getInstance();
        player = getRoot().getPlayerManager().getCurrentPlayer();
        if (player != null) {
            playerName = player.getId();
            playerIndex = player.getIndex();
        }
        this.activity = activity;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
    
    public Player getPlayer() {
        return player;
    }
    

    /**
     * Set the name of the player who <b>executed</b> the action (as opposed to
     * the player who was <b>allowed</b> to do the action, which is the one set
     * in the constructor).
     *
     * @param playerName
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public boolean hasActed() {
        return acted;
    }

    public void setActed() {
        this.acted = true;
    }

    // joint internal method for both equalAs methods
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // compared to null, always false
        if (pa == null) return false;
        // not identical class, always false
        if (!(this.getClass().equals(pa.getClass()))) return false;
        
        // check asOption attributes
        boolean options = Objects.equal(this.player, pa.player)
                        || pa instanceof NullAction // TODO: Old save files are sometimes wrong to assign Null Actions 
        ;      
        
        // finish if asOptions check
        if (asOption) return options;
        
        return options 
                && Objects.equal(this.acted, pa.acted)
        ;
    }
    
    
    /** 
     * Compare the choice options of two action objects, without regard to whatever choice has been made, if any.
     * In other words: only the server-set (prior) attributes must be compared.
     * <p>This method is used by the server (engine) to validate 
     * the incoming action that has actually been chosen in the client (GUI),
     * but only for the purpose to check if the chosen option was really on offer,
     * not to check if the chosen action is actually valid. 
     * These perspectives could give different results in cases where 
     * the PossibleAction does not fully restrict choices to valid values only
     * (such as the blanket LayTile that does no restrict the hex to lay a tile on,
     * or the SetDividend that will accept any revenue value).
     * @param pa Another PossibleAction to compare with.
     * @return True if the compared PossibleAction object has equal choice options.
     */
    public final boolean equalsAsOption (PossibleAction pa) {
        return equalsAs(pa, true);
    }

    /** 
     * Compare the chosen actions of two action objects.
     * In other words: the client-set (posterior) attributes must be compared,
     * in addition to those server-set (prior) attributes that sufficiently identify the action.
     * <p>This method is used by the server (engine) to check if two action 
     * objects represent the same actual action, as is done when reloading a saved file
     * (i.e. loading a later stage of the same game).
     * @param pa Another PossibleAction to compare with.
     * @return True if the compared PossibleAction object has equal selected action values.
     */
    public final boolean equalsAsAction (PossibleAction pa) {
        return equalsAs(pa, false);
    }

    protected RailsRoot getRoot() {
        return root;
    }
    
    protected GameManager getGameManager() {
        return root.getGameManager();
    }

    protected CompanyManager getCompanyManager () {
        return root.getCompanyManager();
    }
    
    public Activity getActivity() {
        return activity;
    }
    
    /**
     * @return true if it is an action to correct the game state
     */
    public boolean isCorrection() {
        return false;
    }

    /** Default version of an Menu item text. To be overridden where useful. */
    public String toMenu() {
        return toString();
    }

    // Implementation for ChangeAction Interface
    public ChangeActionOwner getActionOwner() {
        return player;
    }
    
    @Override
    public String toString() {
        return RailsObjects.stringHelper(this).addBaseText().toString();
    }

    // TODO: Rails 2.0 check if the combination above works correctly
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        if (in instanceof RailsObjectInputStream) {
            root = ((RailsObjectInputStream) in).getRoot();
        }
        
        if (playerName != null) {
            player = getRoot().getPlayerManager().getPlayerByName(playerName);
        } else {
            player = getRoot().getPlayerManager().getPlayerByIndex(playerIndex);
        }
    }
    
}
