/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MessagePanel.java,v 1.2 2006/02/05 21:30:19 evos Exp $
 * 
 * Created on 28-Jan-2006
 * Change Log:
 */
package ui;

import game.Game;

import java.awt.*;
import javax.swing.*;

import util.Util;


/**
 * @author Erik Vos
 */
public class MessagePanel extends JPanel {
    
    JLabel message;
    
    GridBagLayout gb;
    GridBagConstraints gbc = new GridBagConstraints();
    Color background = new Color (225,225,225);

    public MessagePanel() {
    
	    super (new GridBagLayout());
		gb = (GridBagLayout) getLayout();
	
		setBackground(background);
		setSize(1000, 12);
		setBorder(BorderFactory.createLoweredBevelBorder());

		message = new JLabel("A message to you.....");
		message.setBackground(background);
		message.setOpaque(true);
		gbc.gridx = gbc.gridy = 0;
		gbc.insets = new Insets (0,0,0,0);
		gbc.anchor = GridBagConstraints.WEST;
		add(message, gbc);
		message.setVisible(true);
		setVisible(true);
	}
    
    public void setMessage (String messageKey) {
        if (Util.hasValue(messageKey)) {
            message.setText(Game.getText(messageKey));
        }
   }

}
