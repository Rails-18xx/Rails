package ui;

import java.awt.*;

import javax.swing.*;


/**
 * @author Erik Vos
 */
public class MyLabel extends JLabel {
     	
       	int row;
       	int col;
       	
       	public MyLabel (String text) {
       	    this (text, 0, 0);
       	}

       	public MyLabel (String text, int row, int col) {
       		super (text, SwingConstants.CENTER);
       		this.row = row;
       		this.col = col;
    		this.setOpaque(true);
      	}
       	
       	protected int getCol () {
       		return col;
       	}
       	
       	protected int getRow() {
       		return row;
        }
       	
}
