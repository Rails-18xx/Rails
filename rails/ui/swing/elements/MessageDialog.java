/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/MessageDialog.java,v 1.1 2010/01/19 19:49:39 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.util.LocalText;

/**
 * A generic dialog for presenting choices by checkboxes.
 */
public class MessageDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    protected DialogOwner owner = null;
    GridBagConstraints gc;
    JPanel optionsPane, buttonPane;
    JButton okButton;
    Dimension size, optSize;

    String message;

    protected static Logger log =
            Logger.getLogger(MessageDialog.class.getPackage().getName());

    public MessageDialog(DialogOwner owner, JFrame window, String title, String message) {

        super((Frame) null, title, false); // Non-modal
        this.owner = owner;
        this.message = message;

        initialize();
        pack();

        // Center on window
        int x = (int) window.getLocationOnScreen().getX()
                        + (window.getWidth() - getWidth()) / 2;
        int y = (int) window.getLocationOnScreen().getY()
                        + (window.getHeight() - getHeight()) / 2;
        setLocation(x, y);

        setVisible(true);
        toFront();
    }

    private void initialize() {
        gc = new GridBagConstraints();

        optionsPane = new JPanel();
        buttonPane = new JPanel();

        okButton = new JButton(LocalText.getText("OK"));
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        buttonPane.add(okButton);

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        optionsPane.setLayout(new GridBagLayout());
        optionsPane.add(new JLabel(message), constraints(0, 0, 10, 10, 10, 10));

        getContentPane().add(optionsPane, constraints(0, 0, 0, 0, 0, 0));
        getContentPane().add(buttonPane, constraints(0, 1, 0, 0, 0, 0));
    }

    private GridBagConstraints constraints(int gridx, int gridy, int leftinset,
            int topinset, int rightinset, int bottominset) {
        if (gridx >= 0) gc.gridx = gridx;
        if (gridy >= 0) gc.gridy = gridy;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        if (leftinset >= 0) gc.insets.left = leftinset;
        if (topinset >= 0) gc.insets.top = topinset;
        if (rightinset >= 0) gc.insets.right = rightinset;
        if (bottominset >= 0) gc.insets.bottom = bottominset;

        return gc;
    }

    public void actionPerformed(ActionEvent arg0) {
        setVisible(false);
        dispose();
        owner.dialogActionPerformed ();
    }

}
