/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/RadioButtonDialog.java,v 1.8 2010/01/31 22:22:34 macfreek Exp $*/
package net.sf.rails.ui.swing.elements;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.*;

/**
 * A generic dialog for presenting choices by radio buttons.
 */
public class RadioButtonDialog extends NonModalDialog {

    private static final long serialVersionUID = 1L;

    JRadioButton[] choiceButtons;
    Dimension size, optSize;
    ButtonGroup group;

    int numOptions;
    String[] options;
    int selectedOption;
    int chosenOption = -1;

    public RadioButtonDialog(String key, DialogOwner owner, JFrame window, String title, String message,
            String[] options, int selectedOption) {

        super (key, owner, window, title, message);

        this.options = options;
        this.numOptions = options.length;
        this.selectedOption = selectedOption;

        initialize(selectedOption < 0);
    }

    @Override
    protected void initializeInput() {

        choiceButtons = new JRadioButton[numOptions];
        group = new ButtonGroup();

        for (int i = 0; i < numOptions; i++) {
            choiceButtons[i] =
                new JRadioButton(options[i], i == selectedOption);
            optionsPane.add(choiceButtons[i], constraints(0, 1 + i, 0, 0, 0, 0));
            choiceButtons[i].setPreferredSize(size);
            group.add(choiceButtons[i]);
        }
    }

    @Override
    protected void processOK(ActionEvent arg0) {
        for (int i = 0; i < numOptions; i++) {
            if (choiceButtons[i].isSelected()) {
                chosenOption = i;
                break;
            }
        }
    }

    @Override
    protected void processCancel(ActionEvent arg0) {
        chosenOption = -1;
    }

    public synchronized int getSelectedOption() {
        return chosenOption;
    }
}
