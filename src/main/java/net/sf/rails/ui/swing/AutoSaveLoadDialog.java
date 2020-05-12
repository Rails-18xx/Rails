/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/AutoSaveLoadDialog.java,v 1.8 2010/01/31 22:22:34 macfreek Exp $*/
package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.stream.IntStream;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.LocalText;
import net.sf.rails.ui.swing.elements.DialogOwner;
import net.sf.rails.ui.swing.elements.Spinner;


/**
 * A generic dialog for presenting choices by radio buttons.
 */
public class AutoSaveLoadDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private GridBagConstraints gc;
    private JPanel optionsPane, buttonPane;
    private JButton okButton, cancelButton;
    private JRadioButton[] choiceButtons;
    private Spinner intervalSpinner;
    private Dimension size;
    private ButtonGroup group;
    private DialogOwner owner;

    private int status;
    private int interval;

    private static final int NUM_OPTIONS = 3;

    private static final Logger log = LoggerFactory.getLogger(AutoSaveLoadDialog.class);

    public AutoSaveLoadDialog(DialogOwner owner, int oldStatus, int oldInterval) {
        super((Frame) null, "AutoSaveLoad settings", false); // Non-modal
        this.owner = owner;
        this.status = oldStatus;
        this.interval = oldInterval;

        initialize();
        pack();

        int x = 400;
        int y = 400;
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

        cancelButton = new JButton(LocalText.getText("Cancel"));
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(this);
        buttonPane.add(cancelButton);

        choiceButtons = new JRadioButton[3];
        intervalSpinner = new Spinner (interval, 3, 0, 1);

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        optionsPane.setLayout(new GridBagLayout());
        // optionsPane.setBorder(BorderFactory.createLoweredBevelBorder());
        optionsPane.add(new JLabel(LocalText.getText("AutoSaveLoadOptions")),
                constraints(0, 0, 10, 10, 10, 10));

        choiceButtons = new JRadioButton[NUM_OPTIONS];
        group = new ButtonGroup();
        String[] options = new String[] {
                LocalText.getText("Off"),
                LocalText.getText("On"),
                LocalText.getText("Suspended")
        };

        for (int i = 0; i < NUM_OPTIONS; i++) {
            choiceButtons[i] =
                    new JRadioButton(options[i], i == status);
            optionsPane.add(choiceButtons[i], constraints(0, 1 + i, 0, 0, 0, 0));
            choiceButtons[i].setPreferredSize(size);
            group.add(choiceButtons[i]);
        }

        optionsPane.add (new JLabel("Polling interval:"), constraints (0,5,0,0,0,0));
        intervalSpinner.setVisible(true);
        optionsPane.add (intervalSpinner, constraints (1, 5, 0,0,0,0));
        optionsPane.add (new JLabel("sec."), constraints (2,5,0,0,0,0));

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

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(okButton)) {
            status = IntStream.range(0, NUM_OPTIONS).filter(i -> choiceButtons[i].isSelected()).findFirst().orElse(status);
            interval = (Integer) intervalSpinner.getValue();
        } else if (arg0.getSource().equals(cancelButton)) {
            //status = -1;
            // Better change nothing?
        }
        this.setVisible(false);
        this.dispose();
        owner.dialogActionPerformed();
    }

    public synchronized int getStatus() {
        return status;
    }

    public synchronized int getInterval() {
        return interval;
    }
}
