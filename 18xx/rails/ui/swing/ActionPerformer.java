/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ActionPerformer.java,v 1.5 2008/06/04 19:00:33 evos Exp $*/
package rails.ui.swing;

import rails.game.action.PossibleAction;

public interface ActionPerformer {

    public void updateStatus();

    public boolean process(PossibleAction action);

    public boolean processImmediateAction();

    public void displayServerMessage();
}
