/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ActionPerformer.java,v 1.4 2007/12/04 20:25:19 evos Exp $*/
package rails.ui.swing;

import rails.game.action.PossibleAction;

public interface ActionPerformer {
    
    public void updateStatus();
    
    public boolean process(PossibleAction action);
    
    public boolean processImmediateAction();

    public void displayServerMessage ();
}
