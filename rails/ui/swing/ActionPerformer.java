package rails.ui.swing;

import rails.game.action.PossibleAction;

public interface ActionPerformer {
    
    public void updateStatus();
    
    public boolean process(PossibleAction action);
    
    public boolean processImmediateAction();


}
