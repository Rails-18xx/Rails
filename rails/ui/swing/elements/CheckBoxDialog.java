/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/CheckBoxDialog.java,v 1.1 2008/11/20 21:46:00 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import rails.util.LocalText;

/**
 * A generic dialog for presenting choices by radio buttons.
 */
public class CheckBoxDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
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

    protected static Logger log =
            Logger.getLogger(CheckBoxDialog.class.getPackage().getName());

    public CheckBoxDialog(JComponent owner, String title, String message,
            String[] options) {
        this (owner, title, message, options, null);
    }
        
    public CheckBoxDialog(JComponent owner, String title, String message,
            String[] options, boolean[] selectedOptions) {
        super((Frame) null, title, true); // Modal !?
        this.message = message;
        this.options = options;
        this.numOptions = options.length;
        if (selectedOptions != null) {
            this.selectedOptions = selectedOptions;
        } else {
            this.selectedOptions = new boolean[numOptions];
        }

        initialize();
        pack();

        // Center on owner
        int x =
                (int) owner.getLocationOnScreen().getX()
                        + (owner.getWidth() - getWidth()) / 2;
        int y =
                (int) owner.getLocationOnScreen().getY()
                        + (owner.getHeight() - getHeight()) / 2;
        setLocation(x, y);

        this.setVisible(true);
    }

    private void initialize() {
        gc = new GridBagConstraints();

        optionsPane = new JPanel();
        buttonPane = new JPanel();

        okButton = new JButton(LocalText.getText("OK"));
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        buttonPane.add(okButton);

        cancelButton = new JButton(LocalText.getText("Cancel"));
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(this);
        buttonPane.add(cancelButton);

        checkBoxes = new JCheckBox[numOptions];

        this.getContentPane().setLayout(new GridBagLayout());
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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
        }
        this.setVisible(false);
        this.dispose();

    }

    public boolean[] getSelectedOptions() {
        return selectedOptions;
    }
}
