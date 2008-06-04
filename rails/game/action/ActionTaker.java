package rails.game.action;

import java.util.List;

/**
 * Interface ActionTaker should be implemented by subclasses of Swing components
 * to which an action (PossibleAction object) must be tied. Example:
 * ActionButton, a subclass of JButton. When the component is activated, the
 * action to execute is immediately available; no lookup needed.<p> It is
 * possible to attach more than one PossibleAction. In such a case the code must
 * pick up the list and present a choice to the user.
 * 
 * @author Erik Vos
 * 
 */
public interface ActionTaker {

    /** Add a PossibleAction */
    public void addPossibleAction(PossibleAction o);

    /** Get the current PossibleActions */
    public List<PossibleAction> getPossibleActions();

    /** Clear the PossibleActions */
    public void clearPossibleActions();

    /**
     * Set just one PossibleAction (any previously added actions are removed
     * beforehand)
     */
    public void setPossibleAction(PossibleAction o);

}
