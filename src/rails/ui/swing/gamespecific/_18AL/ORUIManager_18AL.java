package rails.ui.swing.gamespecific._18AL;

import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.specific._18AL.AssignNamedTrains;
import rails.ui.swing.ORUIManager;

public class ORUIManager_18AL extends ORUIManager {

    /*
    @Override
    protected void checkForGameSpecificActions() {

        if (possibleActions.contains(AssignNamedTrains.class)) {

            log.debug("AssignNamedTrain possible action found");
            for (AssignNamedTrains action : possibleActions.getType(AssignNamedTrains.class)) {
                String text = action.toMenu();
                orPanel.addSpecialAction(action, text);
            }
        }
    }
    */

    protected boolean processGameSpecificActions(List<PossibleAction> actions) {

        Class<? extends PossibleAction> actionType = actions.get(0).getClass();
        if (actionType == AssignNamedTrains.class) {

            AssignNamedTrains action = (AssignNamedTrains) actions.get(0);
            NameTrainsDialog dialog =
                    new NameTrainsDialog(getORWindow(), action);
            dialog.setVisible(true);

            boolean changed = dialog.hasChanged();
            if (changed) action = dialog.getUpdatedAction();
            dialog.dispose();

            if (changed) orWindow.process(action);
        }

        return false;
    }

}
