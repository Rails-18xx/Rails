/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import javax.swing.*;

/**
 * This is the UI for the LogWindow.
 * It displays logged messages to the user during the game.
 * 
 *  My head is a wheel.
 * 
 * @author Erik Vos
 */
public class LogWindow extends JFrame
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
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
}
