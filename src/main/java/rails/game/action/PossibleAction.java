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

    protected transient RailsRoot root;

    protected String playerName;
    protected int playerIndex;
    protected transient Player player;

    protected boolean acted = false;

    protected transient Activity activity;


    public static final long serialVersionUID = 3L;
    
protected int executionTimeSeconds = 0;
    protected long absoluteTimestamp = 0L;
    
    public int getExecutionTimeSeconds() {
        return executionTimeSeconds;
    }

    public void setExecutionTimeSeconds(int seconds) {
        this.executionTimeSeconds = seconds;
    }

    public long getAbsoluteTimestamp() {
        return absoluteTimestamp;
    }

    public void setAbsoluteTimestamp(long timestamp) {
        this.absoluteTimestamp = timestamp;
    }

    private static final Logger log = LoggerFactory.getLogger(PossibleAction.class);

    protected transient boolean isAIAction = false; // transient = not saved in game saves
    /**
     * Transient (non-serialized) label for UI rendering.
     * This is set by the engine (e.g., OperatingRound) to provide a
     * human-readable string for a dynamically generated button.
     */
    protected transient String label;
    
/**
     * Sets a human-readable label for this action, intended for display on a UI
     * button (typically as a tooltip).
     * @param label The text to display.
     */
    public void setButtonLabel(String label) { // Renamed from setLabel
        this.label = label;
    }

    /**
     * Gets the human-readable label for this action.
     * If no label has been set, it defaults to the action's class name.
     * @return The display label.
     */
    public String getButtonLabel() { // Renamed from getLabel
        if (this.label != null) {
            return this.label;
        }
        // Fallback for actions that don't have a specific label set
        return this.getClass().getSimpleName();
    }

    public void setAIAction(boolean isAI) {
        this.isAIAction = isAI;
    }

    public boolean isAIAction() {
        return this.isAIAction;
    }

    

    // TODO: Replace this by a constructor argument for the player
    public PossibleAction(Activity activity) {
        if (activity == null) {
            return;
        }
        root = activity.getRoot();
        setPlayer();
        this.activity = activity;
    }

    public PossibleAction(RailsRoot root) {
        if (root == null) {
            log.debug("missing root", new Exception());
            return;
        }
        this.root = root;
        setPlayer();
    }

    public void setPlayer() {
        player = root.getPlayerManager().getCurrentPlayer();
        setPlayer(player);
    }

    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            playerName = player.getId();
            playerIndex = player.getIndex();
        }
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
        if (pa == null)
            return false;

        // explicitly ignoring executionTimeSeconds here to isolate the temporal 
        // payload from structural validation during engine replay/hash checks
        
        // not identical class, always false
        if (!(this.getClass().equals(pa.getClass())))
            return false;

        // check asOption attributes
        boolean options = Objects.equal(this.player, pa.player)
                || pa instanceof NullAction // TODO: Old save files are sometimes wrong to assign Null Actions
        ;

        // finish if asOptions check
        if (asOption)
            return options;

        return options && Objects.equal(this.acted, pa.acted);
    }

    /**
     * Compare the choice options of two action objects, without regard to whatever
     * choice has been made, if any.
     * In other words: only the server-set (prior) attributes must be compared.
     * <p>
     * This method is used by the server (engine) to validate
     * the incoming action that has actually been chosen in the client (GUI),
     * but only for the purpose to check if the chosen option was really on offer,
     * not to check if the chosen action is actually valid.
     * These perspectives could give different results in cases where
     * the PossibleAction does not fully restrict choices to valid values only
     * (such as the blanket LayTile that does not restrict the hex to lay a tile on,
     * or the SetDividend that will accept any revenue value).
     * 
     * @param pa Another PossibleAction to compare with.
     * @return True if the compared PossibleAction object has equal choice options.
     */
    public final boolean equalsAsOption(PossibleAction pa) {
        return equalsAs(pa, true);
    }

    /**
     * Compare the chosen actions of two action objects.
     * In other words: the client-set (posterior) attributes must be compared,
     * in addition to those server-set (prior) attributes that sufficiently identify
     * the action.
     * <p>
     * This method is used by the server (engine) to check if two action
     * objects represent the same actual action, as is done when reloading a saved
     * file
     * (i.e. loading a later stage of the same game).
     * 
     * @param pa Another PossibleAction to compare with.
     * @return True if the compared PossibleAction object has equal selected action
     *         values.
     */
    public final boolean equalsAsAction(PossibleAction pa) {
        return equalsAs(pa, false);
    }

    public void setRoot(RailsRoot root) {
        this.root = root;
    }
    
    public RailsRoot getRoot() {
        return root;
    }

    protected GameManager getGameManager() {
        return root.getGameManager();
    }

    protected CompanyManager getCompanyManager() {
        return root.getCompanyManager();
    }

    /*
     * Not used
     * public Activity getActivity() {
     * return activity;
     * }
     */

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

        String base = RailsObjects.stringHelper(this).addBaseText().toString();
if (executionTimeSeconds > 0) {
            return base + " [" + executionTimeSeconds + "s]";
        }
        return base;
    }

    // TODO: Rails 2.0 check if the combination above works correctly
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // inject RailsRoot for use by all subclasses during their own deserializing
        root = ((RailsObjectInputStream) in).getRoot();

        if (playerName != null) {
            player = root.getPlayerManager().getPlayerByName(playerName);
        } else {
            player = root.getPlayerManager().getPlayerByIndex(playerIndex);
        }
    }

}
