package net.sf.rails.ui.swing.gamespecific._1837;

import javax.swing.JDialog;

import rails.game.action.PossibleAction;
import net.sf.rails.ui.swing.StartRoundWindow;
import net.sf.rails.ui.swing.elements.NonModalDialog;

public class StartRoundWindow_1837 extends StartRoundWindow {

    private static final long serialVersionUID = 1L;

    public StartRoundWindow_1837() {
    }

    public void dialogActionPerformed () {
        String key="";

        if (currentDialog instanceof NonModalDialog) {
            key = ((NonModalDialog) currentDialog).getKey();
        }
    }

    public void setCurrentDialog (JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = dialog;
        currentDialogAction = action;
        disableButtons();
    }
}
