/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/MessageDialog.java,v 1.1 2010/01/19 19:49:39 evos Exp $*/
package net.sf.rails.ui.swing.elements;

import javax.swing.JFrame;

/**
 * A generic dialog for presenting choices by checkboxes.
 */
public class MessageDialog extends NonModalDialog {

    private static final long serialVersionUID = 1L;

    public MessageDialog(String key, DialogOwner owner, JFrame window, String title, String message) {

        super (key, owner, window, title, message);

        initialize(false);

    }

}
