/*
 * Created on Aug 4, 2005
 */
package ui.hexmap;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.*;

import game.*;
import game.special.SpecialTileLay;
import ui.*;

/**
 * Base class that stores common info for HexMap independant of Hex
 * orientations.
 */
public abstract class HexMap extends JComponent implements MouseListener,
		MouseMotionListener
{
    /* Substeps in tile and token laying */
    public static final int INACTIVE = 0;
    public static final int SELECT_HEX_FOR_TILE = 1;
    public static final int SELECT_TILE = 2;
    public static final int ROTATE_OR_CONFIRM_TILE = 3;
    public static final int SELECT_HEX_FOR_TOKEN = 4;
    public static final int CONFIRM_TOKEN = 5;
    
    /* Message key per substep */
    protected static final String[] messageKey = new String[] {
        "", "SelectAHexForTile", "SelectATile", "RotateTile", "SelectAHexForToken",
        "ConfirmToken"
    };
    
    
    protected int subStep = INACTIVE;
    

	// Abstract Methods
	protected abstract void setupHexesGUI();

	// GUI hexes need to be recreated for each object, since scale varies.
	protected GUIHex[][] h;
	MapHex[][] hexArray;
	protected ArrayList hexes;
	protected ORWindow window;

	protected int scale = 2 * Scale.get();
	protected int cx;
	protected int cy;

	protected static GUIHex selectedHex = null;
	protected UpgradesPanel upgradesPanel = null;
	protected Dimension preferredSize;

	/**
	 * Is tile laying enabled? If not, one can play with tiles, but the "Done"
	 * button is disabled.
	 */
	protected boolean tileLayingEnabled = false;
	protected java.util.List extraTileLays = new ArrayList();
	protected java.util.List unconnectedTileLays = new ArrayList();

	protected boolean baseTokenLayingEnabled = false;

	public void setupHexes()
	{
		setupHexesGUI();
	}

	/**
	 * Return the GUIBattleHex that contains the given point, or null if none
	 * does.
	 */
	GUIHex getHexContainingPoint(Point2D.Double point)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.contains(point))
			{
				return hex;
			}
		}

		return null;
	}

	GUIHex getHexContainingPoint(Point point)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.contains(point))
			{
				return hex;
			}
		}

		return null;
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		try
		{
			// Abort if called too early.
			Rectangle rectClip = g.getClipBounds();
			if (rectClip == null)
			{
				return;
			}

			Iterator it = hexes.iterator();
			while (it.hasNext())
			{
				GUIHex hex = (GUIHex) it.next();
				Rectangle hexrect = hex.getBounds();

				if (g.hitClip(hexrect.x,
						hexrect.y,
						hexrect.width,
						hexrect.height))
				{
					hex.paint(g);
				}
			}
		}
		catch (NullPointerException ex)
		{
			// If we try to paint before something is loaded, just retry later.
		}
	}

	public Dimension getMinimumSize()
	{
		Dimension dim = new Dimension();
		Rectangle r = ((GUIHex) h[h.length][h[0].length]).getBounds();
		dim.height = r.height;
		dim.width = r.width;
		return dim;
	}

	public Dimension getPreferredSize()
	{
		return preferredSize;
	}

	public void setSubStep (int subStep) {
	    this.subStep = subStep;
	    if (window != null) {
	        window.setMessage(messageKey[subStep]);
	    }
	    if (upgradesPanel != null) {
			upgradesPanel.setDoneText(Game.getText(subStep<4 ? "LayTile" : "LayToken"));
			upgradesPanel.setCancelText(Game.getText(subStep<4 ? "NoTile" : "NoToken"));
			upgradesPanel.setDoneEnabled(subStep == 3 || subStep == 5);
	    }
	        
	}

	public void mouseClicked(MouseEvent arg0)
	{
		Point point = arg0.getPoint();

		GUIHex clickedHex = getHexContainingPoint(point);
		
		if (baseTokenLayingEnabled)
		{
				selectHex(clickedHex);

				//upgradesPanel.setCancelText(Game.getText("Cancel"));
				if (selectedHex != null) {
				    setSubStep (CONFIRM_TOKEN);
				} else {
				    setSubStep (SELECT_HEX_FOR_TOKEN);
				}
		}
		else if (tileLayingEnabled)
		{
		    if (subStep == ROTATE_OR_CONFIRM_TILE && clickedHex == selectedHex) {
		        
				selectedHex.rotateTile();
				repaint(selectedHex.getBounds());
				
		    } else {
		        
		        if (selectedHex != null && clickedHex != selectedHex) {
		            selectedHex.removeTile();
					selectHex(null);
		        }
				if (clickedHex != null) {
				    if (clickedHex.getHexModel().isUpgradeableNow()) {
				        selectHex(clickedHex);
				        setSubStep (SELECT_TILE);
				    } else {
				        JOptionPane.showMessageDialog(this,
							"This hex cannot be upgraded now");
				    }
				}
		    }
		}

		// FIXME: Kludgy, but it forces the upgrades panel to be drawn
		// correctly.
		if (upgradesPanel != null) {
		    upgradesPanel.setVisible(false);
		    upgradesPanel.setVisible(true);
			showUpgrades();
		}
	}

	private void selectHex(GUIHex clickedHex)
	{
	    if (selectedHex != null && clickedHex != selectedHex) {
			selectedHex.setSelected(false);
			repaint(selectedHex.getBounds());
			selectedHex = null;
	    }

	    if (clickedHex != null)	{
			clickedHex.setSelected(true);
			selectedHex = clickedHex;
			repaint(selectedHex.getBounds());
	    }
	}

	public void processDone()
	{
		setSubStep (INACTIVE);
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
			{
				if (selectedHex.getHexModel().getStations().size() == 1)
					selectedHex.fixToken(0);
				else
				{
					Object[] stations = selectedHex.getHexModel()
							.getStations()
							.toArray();
					Station station = (Station) JOptionPane.showInputDialog(this,
							"Which station to place the token in?",
							"Which station?",
							JOptionPane.PLAIN_MESSAGE,
							null,
							stations,
							stations[0]);

					selectedHex.fixToken(selectedHex.getHexModel()
							.getStations()
							.indexOf(station));
				}
			}
		}
		else
		{
			if (selectedHex != null)
				selectedHex.fixTile(tileLayingEnabled);
		}
	}

	public void processCancel()
	{
		setSubStep (INACTIVE);
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeToken();
			GameUILoader.statusWindow.getOrWindow().getORPanel().layBaseToken(null,
					0);
		}
		else
		{
			if (selectedHex != null)
				selectedHex.removeTile();
			if (tileLayingEnabled)
				GameUILoader.statusWindow.getOrWindow().getORPanel().layTile(null,
						null,
						0);
		}

	}

	public void mouseEntered(MouseEvent arg0)
	{
	}

	public void mouseExited(MouseEvent arg0)
	{
	}

	public void mousePressed(MouseEvent arg0)
	{
	}

	public void mouseReleased(MouseEvent arg0)
	{
	}

	public GUIHex getSelectedHex()
	{
		return selectedHex;
	}

	public boolean isHexSelected()
	{
		return selectedHex != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent arg0)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent arg0)
	{
		Point point = arg0.getPoint();
		GUIHex hex = getHexContainingPoint(point);
		setToolTipText(hex != null ? hex.getToolTip() : "");
	}

	public void setUpgradesPanel(UpgradesPanel upgradesPanel)
	{
		this.upgradesPanel = upgradesPanel;
	}

	public void showUpgrades()
	{
	    if (upgradesPanel == null) return;
	    
		if (selectedHex == null || baseTokenLayingEnabled)
		{
			upgradesPanel.setUpgrades(null);
		}
		else
		{
			ArrayList upgrades = (ArrayList) selectedHex.getCurrentTile()
					.getValidUpgrades(selectedHex.getHexModel(),
							GameManager.getCurrentPhase());
			upgradesPanel.setUpgrades(upgrades);
		}

		invalidate();
		upgradesPanel.showUpgrades();
	}
	
	public void enableTileLaying(boolean enabled)
	{
	    if (!tileLayingEnabled && enabled) {
	        /* Start tile laying step */
			setSubStep (SELECT_HEX_FOR_TILE);
	    } else if (tileLayingEnabled && !enabled) {
		    /* Finish tile laying step */
		    if (selectedHex != null) {
				selectedHex.removeTile();
				selectedHex.setSelected(false);
				repaint(selectedHex.getBounds());
				selectedHex = null;
		    }
			setSubStep (INACTIVE);
		}
		tileLayingEnabled = enabled;
	}

	public void enableBaseTokenLaying(boolean enabled)
	{

	    if (!baseTokenLayingEnabled && enabled) {
	        /* Start token laying step */
			setSubStep (SELECT_HEX_FOR_TOKEN);
	    } else if (baseTokenLayingEnabled && !enabled) {
		    /* Finish token laying step */
		    if (selectedHex != null) {
				selectedHex.removeToken();
				selectedHex.setSelected(false);
				repaint(selectedHex.getBounds());
				selectedHex = null;
		    }
			setSubStep (INACTIVE);
		}
		baseTokenLayingEnabled = enabled;
	}

	public void setSpecials(java.util.List specials)
	{
		extraTileLays.clear();
		unconnectedTileLays.clear();
		if (specials != null)
		{
			Iterator it = specials.iterator();
			SpecialTileLay stl;
			while (it.hasNext())
			{
				stl = (SpecialTileLay) it.next();
				if (stl.isExercised())
					continue;
				unconnectedTileLays.add(stl);
				if (stl.isExtra())
					extraTileLays.add(stl);

				//System.out.println("Special tile lay allowed on hex "
				//		+ stl.getLocation().getName() + ", extra="
				//		+ stl.isExtra());
			}
		}
	}
	
	public void setWindow (ORWindow window) {
	    this.window = window;
	}

}
