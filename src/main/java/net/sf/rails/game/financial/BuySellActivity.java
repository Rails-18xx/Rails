package net.sf.rails.game.financial;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import net.sf.rails.game.round.Activity;
import net.sf.rails.game.round.Actor;
import net.sf.rails.game.round.RoundNG;

public class BuySellActivity extends Activity {

    protected BuySellActivity(RoundNG parent, String id) {
        super(parent, id);
    }

    @Override
    public void createActions(Actor actor, PossibleActions actions) {

    }

    @Override
    public boolean isActionExecutable(PossibleAction action) {
        return false;
    }

    @Override
    public void executeAction(PossibleAction action) {
    }

    @Override
    public void reportExecution(PossibleAction action) {

    }
  
}
