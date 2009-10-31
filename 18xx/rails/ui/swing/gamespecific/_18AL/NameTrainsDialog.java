/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/gamespecific/_18AL/NameTrainsDialog.java,v 1.4 2009/10/31 17:08:27 evos Exp $*/
package rails.ui.swing.gamespecific._18AL;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.specific._18AL.AssignNamedTrains;
import rails.game.specific._18AL.NameableTrain;
import rails.game.specific._18AL.NamedTrainToken;
import rails.ui.swing.ORWindow;
import rails.util.*;

import java.util.*;
import java.util.List;

/**
 * The Game Setup Window displays the first window presented to the user. This
 * window contains all of the options available for starting a new rails.game.
 */
public class NameTrainsDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    GridBagConstraints gc;
    ORWindow orWindow;
    AssignNamedTrains action;
    List<NamedTrainToken> tokens;
    List<NameableTrain> trains;
    int numberOfTrains;

    List<NameableTrain> trainPerToken;
    boolean changed = false;

    JPanel pane, selectionPane, buttonPane;
    JButton doneButton, cancelButton;
    Map<NamedTrainToken, List<JRadioButton>> buttonsPerToken =
            new HashMap<NamedTrainToken, List<JRadioButton>>();

    protected static Logger log =
            Logger.getLogger(NameTrainsDialog.class.getPackage().getName());

    public NameTrainsDialog(ORWindow orWindow, AssignNamedTrains action) {

        super(orWindow, true);

        this.orWindow = orWindow;
        this.action = action;
        this.tokens = action.getTokens();
        this.trains = action.getNameableTrains();
        numberOfTrains = trains.size();
        trainPerToken = new ArrayList<NameableTrain>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            trainPerToken.add(null);
        }

        initialize();

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(orWindow);
        this.pack();
    }

    private void initialize() {

        ButtonGroup group;
        JRadioButton radioButton;

        pane = new JPanel(new BorderLayout());

        selectionPane = new JPanel();
        buttonPane = new JPanel();

        selectionPane.setLayout(new GridBagLayout());
        this.setTitle("Assign Named Trains");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.ipadx = 0;
        gc.ipady = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(1, 1, 1, 1);

        int x = 2;
        int y = 0;
        for (TrainI train : trains) {
            addItem(new JLabel(train.getName(), SwingConstants.CENTER), x++, y);
        }
        addItem(new JLabel("None", SwingConstants.CENTER), x, y);

        for (NamedTrainToken token : tokens) {
            x = 0;
            NameableTrain preAssignedTrain =
                    action.getPreTrainPerToken().get(tokens.indexOf(token));
            trainPerToken.set(tokens.indexOf(token), preAssignedTrain);

            addItem(new JLabel(token.getName()), x, ++y);
            addItem(new JLabel("+" + Bank.format(token.getValue())), ++x, y);
            group = new ButtonGroup();
            List<JRadioButton> buttons = new ArrayList<JRadioButton>();
            buttonsPerToken.put(token, buttons);
            for (NameableTrain train : trains) {
                radioButton = new JRadioButton();
                radioButton.setSelected(train.equals(preAssignedTrain));
                addItem(radioButton, ++x, y);
                group.add(radioButton);
                buttons.add(radioButton);
                radioButton.addActionListener(this);
            }
            radioButton = new JRadioButton();
            addItem(radioButton, ++x, y);
            group.add(radioButton);
            buttons.add(radioButton);
            radioButton.addActionListener(this);
        }

        doneButton = new JButton(LocalText.getText("Done"));
        cancelButton = new JButton(LocalText.getText("Cancel"));

        doneButton.setMnemonic(KeyEvent.VK_D);
        cancelButton.setMnemonic(KeyEvent.VK_C);

        doneButton.addActionListener(this);
        cancelButton.addActionListener(this);

        buttonPane.add(doneButton);
        buttonPane.add(cancelButton);

        pane.add(selectionPane, BorderLayout.NORTH);
        pane.add(buttonPane, BorderLayout.SOUTH);

        getContentPane().add(pane);

    }

    private void addItem(JComponent item, int x, int y) {
        gc.gridx = x;
        gc.gridy = y;
        // item.setBorder(border); // Does not work for radio buttons
        selectionPane.add(item, gc);
    }

    public boolean hasChanged() {
        return changed;
    }

    public AssignNamedTrains getUpdatedAction() {
        return action;
    }

    public void actionPerformed(ActionEvent arg0) {

        Object source = arg0.getSource();

        if (source.equals(doneButton)) {
            action.setPostTrainPerToken(trainPerToken);
            setVisible(false);
        } else if (source.equals(cancelButton)) {
            changed = false;
            setVisible(false);
        } else if (source instanceof JRadioButton) {

            JRadioButton radioButton = (JRadioButton) source;
            int x = -1;
            int y = -1;

            // Find the coordinates of the selected button
            for (NamedTrainToken token : tokens) {
                List<JRadioButton> buttons = buttonsPerToken.get(token);
                for (JRadioButton button : buttons) {
                    if (button == radioButton) {
                        x = buttons.indexOf(button);
                        y = tokens.indexOf(token);
                        break;
                    }
                }
                if (x == -1) continue;

                log.debug("RadioButton: x=" + x + " y=" + y);
                changed = true;

                if (x < trains.size()) {
                    trainPerToken.set(y, trains.get(x));
                } else {
                    trainPerToken.set(y, null);
                }

                // Deselect any other tokens with the same train
                if (x >= 0 && tokens.size() > 1) {
                    for (NamedTrainToken otherToken : tokens) {
                        if (otherToken != token) {
                            List<JRadioButton> otherButtons =
                                    buttonsPerToken.get(otherToken);
                            JRadioButton otherButton = otherButtons.get(x);
                            if (otherButton.isSelected()) {
                                otherButtons.get(trains.size()).setSelected(
                                        true);
                                trainPerToken.set(tokens.indexOf(otherToken),
                                        null);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

}
