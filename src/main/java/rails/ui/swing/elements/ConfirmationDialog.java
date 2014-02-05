/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ConfirmationDialog.java,v 1.1 2010/02/28 21:38:06 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

/**
 * A generic YES/NO dialog
 */
public class ConfirmationDialog extends NonModalDialog {

    private static final long serialVersionUID = 1L;

    boolean answer = false;

    public ConfirmationDialog(String key, DialogOwner owner, JFrame window, String title, String message,
            String okTextKey, String cancelTextKey) {

        super (key, owner, window, title, message);

        initialize(okTextKey, cancelTextKey);
    }

    @Override
    protected void processOK (ActionEvent actionEvent) {
        answer = true;
    }

    @Override
    protected void processCancel (ActionEvent actionEvent) {
        answer = false;
    }

    public synchronized boolean getAnswer() {
        return answer;
    }
}
