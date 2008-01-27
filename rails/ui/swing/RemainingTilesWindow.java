/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/RemainingTilesWindow.java,v 1.3 2008/01/27 23:27:54 wakko666 Exp $*/
package rails.ui.swing;

import rails.game.*;
import rails.game.model.ModelObject;
import rails.ui.swing.elements.Field;
import rails.ui.swing.hexmap.GUIHex;
import rails.util.LocalText;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

import org.apache.log4j.Logger;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class RemainingTilesWindow extends JFrame implements WindowListener,
	ActionListener {
    private static final long serialVersionUID = 1L;
    private GameUIManager gameUIManager;
    private ORUIManager orUIManager;

    private List<Field> labels = new ArrayList<Field>();
    private List<TileI> shownTiles = new ArrayList<TileI>();

    private final static int COLUMNS = 10;

    protected static Logger log = Logger.getLogger(RemainingTilesWindow.class
	    .getPackage().getName());

    public RemainingTilesWindow(ORWindow orWindow) {
	super();

	getContentPane().setLayout(new GridLayout(0, COLUMNS, 5, 5));

	setTitle("Rails: Remaining Tiles");
	setVisible(false);
	setSize(800, 600);
	addWindowListener(this);

	init();

	this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	this.setLocationRelativeTo(orWindow);
	pack();
	setVisible(true);
    }

    private void init() {

	TileManagerI tmgr = TileManager.get();
	TileI tile;
	Field label;
	BufferedImage hexImage;
	ImageIcon hexIcon;

	// Build the grid with tiles in the sequence as
	// these have been defined in Tiles.xml
	List<Integer> tileIds = tmgr.getTileIds();
	log.debug("There are " + tileIds.size() + " tiles known in this game");

	for (int tileId : tileIds) {
	    if (tileId <= 0)
		continue;

	    tile = tmgr.getTile(tileId);

	    hexImage = GameUIManager.getImageLoader().getTile(tileId);
	    hexIcon = new ImageIcon(hexImage);
	    hexIcon
		    .setImage(hexIcon.getImage()
			    .getScaledInstance(
				    (int) (hexIcon.getIconWidth()
					    * GUIHex.NORMAL_SCALE * 0.8),
				    (int) (hexIcon.getIconHeight()
					    * GUIHex.NORMAL_SCALE * 0.8),
				    Image.SCALE_SMOOTH));

	    label = new Field((ModelObject) tile, hexIcon, Field.CENTER);
	    label.setVerticalTextPosition(Field.BOTTOM);
	    label.setHorizontalTextPosition(Field.CENTER);
	    label.setVisible(true);

	    getContentPane().add(label);
	    shownTiles.add(tile);
	    labels.add(label);

	}

    }

    public void actionPerformed(ActionEvent actor) {

    }

    public ORUIManager getORUIManager() {
	return orUIManager;
    }

    public GameUIManager getGameUIManager() {
	return gameUIManager;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
	StatusWindow.uncheckMenuItemBox(LocalText.getText("MAP"));
	dispose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void activate() {
	setVisible(true);
	requestFocus();
    }

    /**
         * Round-end settings
         * 
         */
    public void finish() {
    }
}
