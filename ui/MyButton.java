/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MyButton.java,v 1.1 2005/06/09 15:13:28 evos Exp $
 * 
 * Created on 27-May-2005
 * Change Log:
 */
package ui;

import java.awt.*;

import javax.swing.*;

/**
 * @author Erik Vos
 */
public class MyButton extends JToggleButton {
       	
       	int row;
       	int col;
       	
       	static Color bgColour = new Color (224, 224, 255);

       	MyButton (String text, int row, int col) {
       		super (text);
       		this.row = row;
       		this.col = col;
       		this.setBackground(bgColour);
       		this.setOpaque (true);
       		this.setMargin(new Insets(0,2,0,2));
       	}
       	
       	protected int getCol () {
       		return col;
       	}
       	
       	protected int getRow() {
       		return row;
        }
       	
 }
