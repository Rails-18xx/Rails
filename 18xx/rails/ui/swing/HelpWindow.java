/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/HelpWindow.java,v 1.5 2008/06/04 19:00:33 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * This is the UI for the LogWindow. It displays logged messages to the user
 * during the rails.game.
 * 
 * My head is a wheel.
 */
public class HelpWindow extends JFrame implements WindowListener {
    private static final long serialVersionUID = 1L;
    private JEditorPane message;
    private JScrollPane messageScroller;
    private JScrollBar vbar;
    private JPanel messagePanel;
    private static HelpWindow helpWindow;

    public static void displayHelp(String text) {

        if (helpWindow == null) helpWindow = new HelpWindow();
        helpWindow.display(text);
    }

    public HelpWindow() {
        helpWindow = this;

        message = new JEditorPane("text/html", "");
        message.setBackground(new Color(255, 255, 210));
        message.setOpaque(true);
        message.setEditable(false);
        messagePanel = new JPanel(new GridBagLayout());
        messageScroller =
                new JScrollPane(message,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        vbar = messageScroller.getVerticalScrollBar();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        messagePanel.add(messageScroller, gbc);
        setContentPane(messagePanel);

        setSize(400, 400);
        setLocation(600, 000);

        messagePanel.setBorder(BorderFactory.createEtchedBorder());

        setTitle("Help");
        addWindowListener(this);
    }

    private void display(String text) {
        if (text == null) text = "";
        helpWindow.message.setText(text);
        if (text.equals("")) {
            setVisible(false);
        } else {
            helpWindow.vbar.setValue(helpWindow.vbar.getMaximum());
            setState(Frame.NORMAL);
            pack();
            setVisible(true);
            toFront();
        }
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowClosing(WindowEvent e) {
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}
}
