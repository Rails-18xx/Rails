package net.sf.rails.game.round;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Round;
import net.sf.rails.game.state.GenericState;
import rails.game.action.PossibleAction;
import rails.game.action.NullAction;
// Import the action from the net.sf package where we placed it
import net.sf.rails.game.action.SelectHexAction; 

public class SelectionRound extends Round {

    protected final GenericState<String> selectionContext;

    public SelectionRound(GameManager parent, String id, String context) {
        super(parent, id);
        this.selectionContext = new GenericState<>(this, "selectionContext_" + id, context);
    }

@Override
public boolean setPossibleActions() {
    possibleActions.clear();
    // Allow pass
    possibleActions.add(new rails.game.action.NullAction(gameManager.getRoot(), rails.game.action.NullAction.Mode.PASS));
    
    // Explicitly add the SelectHexAction to the allowed list for this round
    possibleActions.add(new net.sf.rails.game.action.SelectHexAction(gameManager.getRoot(), "DUMMY", gameManager.getCurrentPlayer()));
    
    return true;
}

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            gameManager.nextRound(this); 
            return true;
        }

        if (action instanceof SelectHexAction) {
            // Logic handled by the specific Round that interrupted this one
            gameManager.nextRound(this);
            return true;
        }
        return super.process(action);
    }
}