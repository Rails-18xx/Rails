/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/UpgradesPanel.java,v 1.1 2005/10/30 19:55:06 evos Exp $
 * 
 * Created on 30-Oct-2005
 * Change Log:
 */
package ui;

import java.awt.*;

import game.TileI;

import javax.swing.*;
import javax.swing.border.*;

/**
 * @author Erik Vos
 */
public class UpgradesPanel extends JPanel {
    
    private TileI[] currentTiles;
    // TEMPORARILY REPRESENT TILES BY A JLabel
    private JLabel[] comps = new JLabel[0];
    Border border = new EtchedBorder();
    
    public UpgradesPanel () {
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
        add (new JLabel ("<html><center><b>Select<br>upgrade<br>tile</b>"));
    }
    
    private void clear () {
        for (int i=0; i<comps.length; i++) {
            this.remove(comps[i]);
        }
        currentTiles = new TileI[0];
        comps = new JLabel[0];
    }
    
    public void display (TileI[] tiles) {
        
        clear();
        if (tiles != null && tiles.length > 0) { 
        
	        currentTiles = tiles;
	        JPanel tilePanel;
	        TileI tile;
	        comps = new JLabel[tiles.length];
	        for (int i=0; i<tiles.length; i++) {
	            tile = tiles[i];
	            comps[i] = new JLabel("Tile #"+tile.getId());
	            comps[i].setBorder(border);
	            add (comps[i]);
	            //System.out.println("Upgrade tile: "+tile.getId());
	        }
        }
        repaint();
        this.getParent().repaint();

    }

}
