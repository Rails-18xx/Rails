/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * This is the UI for the LogWindow.
 * It displays logged messages to the user during the game.
 * 
 *  My head is a wheel.
 * 
 * @author Erik Vos
 * @author Brett
 */
public class LogWindow extends JFrame implements WindowListener
{

	private JLabel message;
	private JScrollPane messageScroller;
	private JScrollBar vbar;
	private JPanel messagePanel;
	private static LogWindow messageWindow;

	private static StringBuffer buffer = new StringBuffer("<html></html>");

	public LogWindow()
	{
		messageWindow = this;

		message = new JLabel("");
		message.setBackground(Color.WHITE);
		message.setOpaque(true);
		message.setVerticalAlignment(SwingConstants.TOP);
		messagePanel = new JPanel(new GridBagLayout());
		messageScroller = new JScrollPane(message,
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
		setLocation(600, 400);

		messagePanel.setBorder(BorderFactory.createEtchedBorder());

		setTitle("Rails: Game log");
		addWindowListener(this);
	}

	public static void addLog()
	{
		String newText = Log.getMessageBuffer();
		if (newText.length() > 0)
		{
			buffer.insert(buffer.length() - 7, newText.replaceAll("\n", "<br>"));

			messageWindow.message.setText(buffer.toString());
			messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
		}
	}
	
	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(StatusWindow.logString);
		dispose();		
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}
}
