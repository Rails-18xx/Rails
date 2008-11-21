/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ReportWindow.java,v 1.7 2008/11/21 20:41:47 evos Exp $*/
package rails.ui.swing;

import rails.game.*;
import rails.util.LocalText;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * This is the UI for the LogWindow. It displays logged messages to the user
 * during the rails.game.
 */
public class ReportWindow extends JFrame implements WindowListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private JTextArea message;
    private JScrollPane messageScroller;
    private JScrollBar vbar;
    private JPanel messagePanel;
    private ReportWindow messageWindow;
    private GameManager gameManager;

    public ReportWindow(GameManager gameManager) {
        messageWindow = this;
        this.gameManager = gameManager;

        message = new JTextArea();
        message.setEditable(false);
        message.setLineWrap(false);
        message.setBackground(Color.WHITE);
        message.setOpaque(true);
        message.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messagePanel = new JPanel(new GridBagLayout());
        messageScroller =
                new JScrollPane(message,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        vbar = messageScroller.getVerticalScrollBar();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        messagePanel.add(messageScroller, gbc);
        setContentPane(messagePanel);

        setSize(400, 400);
        setLocation(600, 400);
        setTitle("Rails: Game log");
        addWindowListener(this);
        addKeyListener(this);

    }

    public void addLog() {
        String newText = ReportBuffer.get();
        if (newText.length() > 0) {
            message.append(newText);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
                }
            });
        }
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowClosing(WindowEvent e) {
        StatusWindow.uncheckMenuItemBox(LocalText.getText("REPORT"));
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameManager.getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

}
