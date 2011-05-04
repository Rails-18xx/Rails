/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/RadioButtonDialog.java,v 1.8 2010/01/31 22:22:34 macfreek Exp $*/
package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.util.LocalText;

/**
 * A generic dialog for presenting choices by radio buttons.
 */
public class RadioButtonDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    GridBagConstraints gc;
    JPanel optionsPane, buttonPane;
    JButton okButton, cancelButton;
    JRadioButton[] choiceButtons;
    Dimension size, optSize;
    ButtonGroup group;
    DialogOwner owner;

    String message;
    int numOptions;
    String[] options;
    int selectedOption;
    int chosenOption = -1;

    protected static Logger log =
            Logger.getLogger(RadioButtonDialog.class.getPackage().getName());

    public RadioButtonDialog(DialogOwner owner, JFrame window, String title, String message,
            String[] options, int selectedOption) {
        super((Frame) null, title, false); // Non-modal
        this.owner = owner;
        this.message = message;
        this.options = options;
        this.numOptions = options.length;
        this.selectedOption = selectedOption;

        initialize();
        pack();

        // Center on window
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

        if (selectedOption < 0) {
            // If an option has been preselected, selection is mandatory.
            cancelButton = new JButton(LocalText.getText("Cancel"));
            cancelButton.setMnemonic(KeyEvent.VK_C);
            cancelButton.addActionListener(this);
            buttonPane.add(cancelButton);
        }

        choiceButtons = new JRadioButton[numOptions];

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        optionsPane.setLayout(new GridBagLayout());
        // optionsPane.setBorder(BorderFactory.createLoweredBevelBorder());
        optionsPane.add(new JLabel(message), constraints(0, 0, 10, 10, 10, 10));

        choiceButtons = new JRadioButton[numOptions];
        group = new ButtonGroup();

        for (int i = 0; i < numOptions; i++) {
            choiceButtons[i] =
                    new JRadioButton(options[i], i == selectedOption);
            optionsPane.add(choiceButtons[i], constraints(0, 1 + i, 0, 0, 0, 0));
            choiceButtons[i].setPreferredSize(size);
            group.add(choiceButtons[i]);
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
                if (choiceButtons[i].isSelected()) {
                    chosenOption = i;
                    break;
                }
            }
        } else if (arg0.getSource().equals(cancelButton)) {
            chosenOption = -1;
        }
        this.setVisible(false);
        this.dispose();
        owner.dialogActionPerformed();
    }

    public synchronized int getSelectedOption() {
        return chosenOption;
    }
}
