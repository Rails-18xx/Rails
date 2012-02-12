package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import rails.util.Util;

public abstract class NonModalDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    protected String key;
    protected DialogOwner owner = null;
    protected JFrame window = null;
    protected String message;
    protected boolean hasCancelButton = true;

    GridBagConstraints gc;
    JPanel optionsPane, buttonPane;
    RailsIconButton okButton, cancelButton;
    String okTextKey = "OK";
    String cancelTextKey = "Cancel";

    public NonModalDialog(String key,
            DialogOwner owner, JFrame window, String title, String message) {

        super((Frame) null, title, false); // Non-modal
        this.key = key;
        this.owner = owner;
        this.window = window;
        this.message = message;
    }

    protected final void initialize (String okTextKey, String cancelTextKey) {
        this.okTextKey = okTextKey;
        this.cancelTextKey = cancelTextKey;
        this.hasCancelButton = Util.hasValue(cancelTextKey);
        initialize();
    }

    protected final void initialize (boolean hasCancelButton) {
        this.hasCancelButton = hasCancelButton;
        initialize();
    }

    protected final void initialize() {

        gc = new GridBagConstraints();

        optionsPane = new JPanel();
        buttonPane = new JPanel();

        okButton = new RailsIconButton(RailsIcon.getByConfigKey(okTextKey));
        okButton.setMnemonic(okTextKey.startsWith("Y") ? KeyEvent.VK_Y : KeyEvent.VK_O);
        okButton.addActionListener(this);
        buttonPane.add(okButton);

        if (hasCancelButton) {
            cancelButton = new RailsIconButton(RailsIcon.getByConfigKey(cancelTextKey));
            cancelButton.setMnemonic(cancelTextKey.startsWith("N") ? KeyEvent.VK_N : KeyEvent.VK_C);
            cancelButton.addActionListener(this);
            buttonPane.add(cancelButton);
        }

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        optionsPane.setLayout(new GridBagLayout());
        optionsPane.add(new JLabel(message), constraints(0, 0, 10, 10, 10, 10));

        initializeInput();

        getContentPane().add(optionsPane, constraints(0, 0, 0, 0, 0, 0));
        getContentPane().add(buttonPane, constraints(0, 1, 0, 0, 0, 0));

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

    protected void initializeInput() {}

    protected GridBagConstraints constraints(int gridx, int gridy, int leftinset,
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

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(okButton)) {
            processOK(actionEvent);
        } else if (actionEvent.getSource().equals(cancelButton)) {
            processCancel (actionEvent);
        }
        setVisible(false);
        dispose();
        owner.dialogActionPerformed ();
    }

    protected void processOK (ActionEvent actionEvent) {}

    protected void processCancel (ActionEvent actionEvent) {}

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "Dialog type="+getClass().getSimpleName()+" key="+key;
    }
}
