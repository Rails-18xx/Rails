/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ActionPerformer.java,v 1.6 2010/01/20 19:51:08 evos Exp $*/
package rails.ui.swing;

import rails.game.action.PossibleAction;

public interface ActionPerformer {

    public void updateStatus();

    public boolean process(PossibleAction action);

    public boolean processImmediateAction();
}
