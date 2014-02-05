/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/CheckBoxDialog.java,v 1.9 2010/06/16 20:59:10 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.*;

/**
 * A generic dialog for presenting choices by checkboxes.
 */
public class CheckBoxDialog extends NonModalDialog {

    private static final long serialVersionUID = 1L;

    JCheckBox[] checkBoxes;
    Dimension size, optSize;
    ButtonGroup group;

    String message;
    int numOptions;
    String[] options;
    boolean selectedOptions[];
    int chosenOption = -1;
    boolean hasCancelButton = false;

    public CheckBoxDialog(String key, DialogOwner owner, JFrame window, String title, String message,
            String[] options) {
        this (key, owner, window, title, message, options, null, false);
    }

    public CheckBoxDialog(String key, DialogOwner owner, JFrame window, String title, String message,
            String[] options, boolean[] selectedOptions, boolean addCancelButton) {

        super (key, owner, window, title, message);
        this.hasCancelButton = addCancelButton;

        this.options = options;
        this.numOptions = options.length;
        if (selectedOptions != null) {
            this.selectedOptions = selectedOptions;
        } else {
            this.selectedOptions = new boolean[numOptions];
        }

        initialize(hasCancelButton);
    }

    @Override
    protected void initializeInput() {

        checkBoxes = new JCheckBox[numOptions];

        for (int i = 0; i < numOptions; i++) {
            checkBoxes[i] =
                new JCheckBox(options[i], selectedOptions[i]);
            optionsPane.add(checkBoxes[i], constraints(0, 1 + i, 0, 0, 0, 0));
            //checkBoxes[i].setPreferredSize(size);
        }
    }

    @Override
    protected void processOK (ActionEvent actionEvent) {
        for (int i = 0; i < numOptions; i++) {
            selectedOptions[i] = checkBoxes[i].isSelected();
        }
    }

    @Override
    protected void processCancel (ActionEvent actionEvent) {

    }

    public String[] getOptions () {
        return options;
    }

    public synchronized boolean[] getSelectedOptions() {
        return selectedOptions;
    }
}
