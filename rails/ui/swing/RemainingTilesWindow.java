/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/RemainingTilesWindow.java,v 1.1 2008/01/17 21:13:48 evos Exp $*/
package rails.ui.swing;

import rails.game.*;
import rails.game.action.LayTile;
import rails.game.action.LayToken;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
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
public class RemainingTilesWindow extends JFrame implements WindowListener, ActionListener
{
    private GameUIManager gameUIManager;
    private ORUIManager orUIManager;
	private ORWindow orWindow;
	private GridLayout gridLayout;

	private List<JLabel> labels = new ArrayList<JLabel>();
	private List<TileI> shownTiles = new ArrayList<TileI>();
	
	private final static int COLUMNS = 10;
	
	protected static Logger log = Logger.getLogger(RemainingTilesWindow.class.getPackage().getName());

	public RemainingTilesWindow(ORWindow orWindow)
	{
		super();
        this.orWindow = orWindow;
        
		getContentPane().setLayout(gridLayout = new GridLayout(0, COLUMNS, 5, 5));

		setTitle("Rails: Remaining Tiles");
		//setLocation(10, 10);
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
	    int i, externalId, count;
	    JLabel label;
	    BufferedImage hexImage;
	    ImageIcon hexIcon;
	    String initialText;
	    
	    // Build the grid with tiles in the sequence as
	    // these have been defined in Tiles.xml
	    List<Integer> tileIds = tmgr.getTileIds();
	    log.debug("There are "+tileIds.size()+" tiles known in this game");
	    
	    for (int tileId : tileIds) {
	        if (tileId <= 0) continue;
	        
	        tile = tmgr.getTile(tileId);
	        externalId = tile.getExternalId();
	        
            hexImage = GameUIManager.getImageLoader().getTile(tileId);
            hexIcon = new ImageIcon(hexImage);
            hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                    (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE),
                    (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE*0.8),
                    Image.SCALE_SMOOTH));
            
	        label = new JLabel (makeCaption(tile), hexIcon, JLabel.CENTER);
	        label.setVerticalTextPosition(JLabel.BOTTOM);
	        label.setHorizontalTextPosition(JLabel.CENTER);
	        label.setVisible(true);
	        
	        getContentPane().add(label);
	        shownTiles.add(tile);
	        labels.add(label);
	        
	    }
	    
	}
	
	private String makeCaption (TileI tile) {
		
        int count = tile.countFreeTiles();
        String text = "#" + tile.getExternalId() + ": ";
        if (count == -1) {
        	text += "+";	
        } else {
        	text += count;
        }
        return text;
	}
	
	public void refresh() {
		
		for (int i=0; i<shownTiles.size(); i++) {
			labels.get(i).setText(makeCaption(shownTiles.get(i)));
		}
		
		setVisible(true);
	    
	}
	
	public void actionPerformed (ActionEvent actor) {
	    
	}

    public ORUIManager getORUIManager() {
        return orUIManager;
    }
    
    public GameUIManager getGameUIManager() {
        return gameUIManager;
    }

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(LocalText.getText("MAP"));
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

	public void activate(OperatingRound or)
	{
		refresh();
		pack();
		setVisible(true);
		requestFocus();
	}

	/**
	 * Round-end settings
	 * 
	 */
	public void finish()
	{
	}
}
