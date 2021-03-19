/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/CheckBoxDialog.java,v 1.9 2010/06/16 20:59:10 evos Exp $*/
package net.sf.rails.ui.swing.elements;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.*;

/**
 * A generic dialog for presenting choices by checkboxes.
 */
public class CheckBoxDialog extends NonModalDialog {

    private static final long serialVersionUID = 1L;

    private JCheckBox[] checkBoxes;

    private int numOptions;
    private String[] options;
    private boolean[] selectedOptions;

    public CheckBoxDialog(String key, DialogOwner owner, JFrame window, String title, String message,
            String[] options) {
        this (key, owner, window, title, message, options, null, false);
    }

    public CheckBoxDialog(String key, DialogOwner owner, JFrame window, String title, String message,
            String[] options, boolean[] selectedOptions, boolean addCancelButton) {
        this(key, owner, window, title, message, options, null,
                "OK", (addCancelButton ? "Cancel" : ""));
    }

    public CheckBoxDialog(String key, DialogOwner owner, JFrame window, String title, String message,
                          String[] options, boolean[] selectedOptions,
                          String okButtonText, String cancelButtonText) {

        super (key, owner, window, title, message);

        this.options = options;
        this.numOptions = options.length;
        if (selectedOptions != null) {
            this.selectedOptions = selectedOptions;
        } else {
            this.selectedOptions = new boolean[numOptions];
        }

        initialize(okButtonText, cancelButtonText);
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
