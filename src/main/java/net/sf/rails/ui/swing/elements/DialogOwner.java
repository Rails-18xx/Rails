package net.sf.rails.ui.swing.elements;

import javax.swing.JDialog;

import net.sf.rails.game.action.PossibleAction;


public interface DialogOwner {

    public void dialogActionPerformed ();

    public JDialog getCurrentDialog();

    public PossibleAction getCurrentDialogAction();

    public void setCurrentDialog (JDialog dialog, PossibleAction action);
}
