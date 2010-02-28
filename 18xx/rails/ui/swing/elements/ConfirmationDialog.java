/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ConfirmationDialog.java,v 1.1 2010/02/28 21:38:06 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.util.LocalText;

/**
 * A generic YES/NO dialog 
 */
public class ConfirmationDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    GridBagConstraints gc;
    JPanel messagePane, buttonPane;
    JButton okButton, cancelButton;
    Dimension size, optSize;
    DialogOwner owner;

    String message;
    boolean answer = false;

    protected static Logger log =
            Logger.getLogger(ConfirmationDialog.class.getPackage().getName());

    public ConfirmationDialog(DialogOwner owner, String title, String message,
            String okText, String cancelText) {
        super((Frame) null, title, false); // Non-modal
        this.owner = owner;
        this.message = message;

        initialize(okText, cancelText);
        pack();

        // Center on owner
        /*
        int x =
                (int) owner.getLocationOnScreen().getX()
                        + (owner.getWidth() - getWidth()) / 2;
        int y =
                (int) owner.getLocationOnScreen().getY()
                        + (owner.getHeight() - getHeight()) / 2;
                        */
        int x = 400;
        int y = 400;
        setLocation(x, y);

        setVisible(true);
        setAlwaysOnTop(true);
    }

    private void initialize(String okText, String cancelText) {

        gc = new GridBagConstraints();

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        messagePane = new JPanel();

        messagePane.add(new JLabel(message));

        buttonPane = new JPanel();

        okButton = new JButton(LocalText.getText(okText));
        // We only expect Yes/No or OK/Cancel
        if (okText.startsWith("O"))
            okButton.setMnemonic(KeyEvent.VK_O);
        else if (okText.startsWith("Y"))
            okButton.setMnemonic(KeyEvent.VK_Y);
        okButton.addActionListener(this);
        buttonPane.add(okButton);

        cancelButton = new JButton(LocalText.getText(cancelText));
        // We only expect Yes/No or OK/Cancel
        if (cancelText.startsWith("C"))
            cancelButton.setMnemonic(KeyEvent.VK_C);
        else if (cancelText.startsWith("N"))
            cancelButton.setMnemonic(KeyEvent.VK_N);
        cancelButton.addActionListener(this);
        buttonPane.add(cancelButton);

        getContentPane().add(messagePane, constraints(0, 0, 0, 0, 0, 0));
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
        if (arg0.getSource().equals(okButton)) {
            answer = true;
        } else if (arg0.getSource().equals(cancelButton)) {
            answer = false;
        }
        this.setVisible(false);
        this.dispose();
        owner.dialogActionPerformed();
    }

    public synchronized boolean getAnswer() {
        return answer;
    }
}
