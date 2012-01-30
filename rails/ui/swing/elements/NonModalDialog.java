package rails.ui.swing.elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.LocalText;

public abstract class NonModalDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    protected Type type;
    protected Usage usage;
    protected DialogOwner owner = null;
    protected JFrame window = null;
    protected String message;
    protected boolean hasCancelButton = false;

    GridBagConstraints gc;
    JPanel optionsPane, buttonPane;
    JButton okButton, cancelButton;

    public static enum Usage {
        REPAY_LOANS,
        DESTINATION_REACHED,
        BUY_WHICH_TRAIN,
        COMPANY_START_PRICE,
        EXCHANGE_TOKENS,
        SELECT_FOLDING_COMPANIES,
        SELECT_DESTINATION_COMPANIES,
        SELECT_COMPANY,
        SELECT_HOME_STATION
    }

    public static enum Type {
        CHECKBOX,
        RADIO,
        MESSAGE,
        MIXED
    }

    protected static Logger log =
        Logger.getLogger(NonModalDialog.class.getPackage().getName());

    public NonModalDialog(Type type, Usage usage,
            DialogOwner owner, JFrame window, String title, String message,
            boolean addCancelButton) {

        super((Frame) null, title, false); // Non-modal
        this.type = type;
        this.usage = usage;
        this.owner = owner;
        this.window = window;
        this.message = message;
        hasCancelButton = addCancelButton;
    }

    protected final void initialize() {

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

    protected void initializeInput() {
    }

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

    protected void processOK (ActionEvent actionEvent) {};

    protected void processCancel (ActionEvent actionEvent) {};
}
