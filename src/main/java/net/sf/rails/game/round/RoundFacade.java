package net.sf.rails.game.round;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.Creatable;
import rails.game.action.PossibleAction;

public interface RoundFacade extends Creatable, RailsItem {

    // called from GameManager
    boolean process(PossibleAction action);

    // called from GameManager and GameLoader
    boolean setPossibleActions();

    // called from GameManager
    void resume();

    // called from GameManager and GameUIManager
    String getRoundName();

    /** A stub for processing actions triggered by a phase change.
     * Must be overridden by subclasses that need to process such actions.
     * @param name (required) The name of the action to be executed
     * @param value (optional) The value of the action to be executed, if applicable
     */
    // can this be moved to GameManager, not yet as there are internal dependencies
    // called from GameManager
    void processPhaseAction(String name, String value);

    String getOwnWindowTitle ();
}