package ui;

import java.awt.*;
import javax.swing.*;

import util.*;

public class MessagePanel extends JPanel
{

	JLabel message;

	//GridBagLayout gb;
	//GridBagConstraints gbc = new GridBagConstraints();
	Color background = new Color(225, 225, 225);

	public MessagePanel()
	{
		super(/*new GridBagLayout()*/);
		//gb = (GridBagLayout) getLayout();

		setBackground(background);
		setLines(1);
		setBorder(BorderFactory.createLoweredBevelBorder());

		message = new JLabel("A message to you.....");
		message.setBackground(background);
		message.setVerticalAlignment(SwingConstants.CENTER);
		message.setHorizontalAlignment(SwingConstants.CENTER);
		message.setOpaque(true);
		//gbc.gridx = gbc.gridy = 0;
		//gbc.insets = new Insets(0, 0, 0, 0);
		//gbc.anchor = GridBagConstraints.WEST;
		//add(message, gbc);
		add(message);
		message.setVisible(true);
		setVisible(true);
	}

	public void setMessage(String messageText)
	{
		if (Util.hasValue(messageText))
		{
			int lines = messageText.split("<[Bb][Rr]>").length + 1;
			setLines (lines);
			message.setText("<html>"+messageText+"</html>");
		}
	}
	
	public void setLines (int numberOfLines) {
	    setSize (1000, numberOfLines*12);
	}

}
