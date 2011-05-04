/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/CheckBoxDialog.java,v 1.9 2010/06/16 20:59:10 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.util.LocalText;

/**
 * A generic dialog for presenting choices by checkboxes.
 */
public class CheckBoxDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    protected DialogOwner owner = null;
    GridBagConstraints gc;
    JPanel optionsPane, buttonPane;
    JButton okButton, cancelButton;
    JCheckBox[] checkBoxes;
    Dimension size, optSize;
    ButtonGroup group;

    String message;
    int numOptions;
    String[] options;
    boolean selectedOptions[];
    int chosenOption = -1;
    boolean hasCancelButton = false;

    protected static Logger log =
            Logger.getLogger(CheckBoxDialog.class.getPackage().getName());

    public CheckBoxDialog(DialogOwner owner, JFrame window, String title, String message,
            String[] options) {
        this (owner, window, title, message, options, null, false);
    }

    public CheckBoxDialog(DialogOwner owner, JFrame window, String title, String message,
            String[] options, boolean[] selectedOptions, boolean addCancelButton) {
        super((Frame) null, title, false); // Non-modal
        this.owner = owner;
        this.message = message;
        this.options = options;
        this.numOptions = options.length;
        this.hasCancelButton = addCancelButton;
        if (selectedOptions != null) {
            this.selectedOptions = selectedOptions;
        } else {
            this.selectedOptions = new boolean[numOptions];
        }

        initialize();
        pack();

        // Center on owner
        int x = (int) window.getLocationOnScreen().getX()
                        + (window.getWidth() - getWidth()) / 2;
        int y = (int) window.getLocationOnScreen().getY()
                        + (window.getHeight() - getHeight()) / 2;
        setLocation(x, y);

        setVisible(true);
        setAlwaysOnTop(true);
    }

    private void initialize() {
        gc = new GridBagConstraints();

        optionsPane = new JPanel();
        buttonPane = new JPanel();

        okButton = new JButton(LocalText.getText("OK"));
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        buttonPane.add(okButton);

        if (hasCancelButton) {
            cancelButton = new JButton(LocalText.getText("Cancel"));
            cancelButton.setMnemonic(KeyEvent.VK_C);
            cancelButton.addActionListener(this);
            buttonPane.add(cancelButton);
        }

        checkBoxes = new JCheckBox[numOptions];

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        optionsPane.setLayout(new GridBagLayout());
        optionsPane.add(new JLabel(message), constraints(0, 0, 10, 10, 10, 10));

        checkBoxes = new JCheckBox[numOptions];

        for (int i = 0; i < numOptions; i++) {
            checkBoxes[i] =
                    new JCheckBox(options[i], selectedOptions[i]);
            optionsPane.add(checkBoxes[i], constraints(0, 1 + i, 0, 0, 0, 0));
            checkBoxes[i].setPreferredSize(size);
        }

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
        if (arg0.getSource().equals(okButton)) {
            for (int i = 0; i < numOptions; i++) {
                selectedOptions[i] = checkBoxes[i].isSelected();
            }
        } else if (arg0.getSource().equals(cancelButton)) {
        //    return;
        }
        setVisible(false);
        dispose();
        owner.dialogActionPerformed ();
    }

    public String[] getOptions () {
        return options;
    }

    public synchronized boolean[] getSelectedOptions() {
        return selectedOptions;
    }
}
