/*
 * Created on Aug 4, 2005
 */
package ui.hexmap;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

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
	// Abstract Methods
	protected abstract void setupHexesGUI();

	// GUI hexes need to be recreated for each object, since scale varies.
	protected GUIHex[][] h;
	MapHex[][] hexArray;
	protected ArrayList hexes;

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
		Dimension dim = getPreferredSize();
		dim.height /= 2;
		dim.width /= 2;
		return dim;
	}

	public Dimension getPreferredSize()
	{
		return preferredSize;
	}

	public void mouseClicked(MouseEvent arg0)
	{
		Point point = arg0.getPoint();

		try
		{
			GUIHex clickedHex = getHexContainingPoint(point);

			if (baseTokenLayingEnabled) {
			    
			    if (selectedHex != null) {
			        // REMOVE TOKEN FROM PREVIOUSLY SELECTED HEX
			    }
			    selectedHex = clickedHex;
			    // DROP A TOKEN - HOW?
			    
			} else if (clickedHex == selectedHex)
			{
				selectedHex.rotateTile();
				repaint(selectedHex.getBounds());
			}
			else
			{
				if (selectedHex != null)
				{
					selectedHex.removeTile();
					selectedHex.setSelected(false);
					repaint(selectedHex.getBounds());
					selectedHex = null;
				}
				if (clickedHex.getHexModel().isUpgradeableNow())
				{
					clickedHex.setSelected(true);
					selectedHex = clickedHex;
					repaint(selectedHex.getBounds());
				} else {
					JOptionPane.showMessageDialog (this, "This hex cannot be upgraded now");
				}
			}

			// FIXME: Kludgy, but it forces the upgrades panel to be drawn
			// correctly.
			upgradesPanel.setVisible(false);
			upgradesPanel.setVisible(true);
		}
		catch (NullPointerException e)
		{
			// No hex clicked
			if (selectedHex != null)
			{
				selectedHex.removeTile();
				selectedHex.setSelected(false);
				repaint(selectedHex.getBounds());
				selectedHex = null;
			}
		}

		showUpgrades();

		// System.out.println("Tokens: " +
		// selectedHex.getHexModel().getTokens());
	}

	public void processDone()
	{
	    if (baseTokenLayingEnabled) {
	        if (selectedHex != null) selectedHex.fixToken();
	    } else {
	        if (selectedHex != null) selectedHex.fixTile(tileLayingEnabled);
		}
	}

	public void processCancel()
	{
	    if (baseTokenLayingEnabled) {
	        if (selectedHex != null) selectedHex.removeToken();
	        GameUILoader.statusWindow.orWindow.layBaseToken(null);
	    } else {
	        if (selectedHex != null) selectedHex.removeTile();
	        if (tileLayingEnabled)
	            GameUILoader.statusWindow.orWindow.layTile(null, null, 0);
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
		if (selectedHex == null || baseTokenLayingEnabled)
		{
			upgradesPanel.setUpgrades(null);
		}
		else
		{
			ArrayList upgrades = (ArrayList) selectedHex.getCurrentTile()
					.getValidUpgrades(selectedHex.getHexModel(),
					        GameManager.getCurrentPhase());
			if (upgrades == null)
			{
				upgradesPanel.setUpgrades(null);
			}
			else
			{
				upgradesPanel.setUpgrades(upgrades);
			}
		}

		invalidate();
		upgradesPanel.showUpgrades(tileLayingEnabled || baseTokenLayingEnabled);
	}

	public void enableTileLaying(boolean enabled)
	{
		tileLayingEnabled = enabled;
		if (enabled == false && selectedHex != null)
		{
			selectedHex.removeTile();
			selectedHex.setSelected(false);
			repaint(selectedHex.getBounds());
			selectedHex = null;
		}
	}
	
	public void enableBaseTokenLaying (boolean enabled) {
	    
	    baseTokenLayingEnabled = enabled;
	    
	    if (enabled == false && selectedHex != null) {
	        selectedHex.removeToken();
	        selectedHex.setSelected(false);
	        repaint(selectedHex.getBounds());
	        selectedHex = null;
	    }
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

				System.out.println("Special tile lay allowed on hex "
						+ stl.getLocation().getName() + ", extra="
						+ stl.isExtra());
			}
		}
	}

}
