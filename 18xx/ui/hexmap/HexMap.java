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
import ui.*;

/**
 * @author blentz
 */
public abstract class HexMap extends JPanel implements MouseListener,
		Scrollable
{

	// GUI hexes need to be recreated for each object, since scale varies.
	protected GUIHex[][] h = new GUIHex[6][6];

	/** ne, e, se, sw, w, nw */
	protected GUIHex[] entrances = new GUIHex[6];

	protected ArrayList hexes = new ArrayList(33);

	// The game state hexes can be set up once for each terrain type.
	protected static Map terrainH = new HashMap();
	protected static Map terrainHexes = new HashMap();
	protected static Map entranceHexes = new HashMap();
	protected static Map startlistMap = new HashMap();
	protected static Map subtitleMap = new HashMap();
	protected static Map towerStatusMap = new HashMap();
	protected static Map hazardNumberMap = new HashMap();
	protected static Map hazardSideNumberMap = new HashMap();

	// BUG: There's bugs with how this map is populated by setupNeighbors().
	// This will need significant reworking.
	protected static final boolean[][] show = {
			{ false, true, true, true, true, true },
			{ true, true, true, true, true, true },
			{ false, true, true, true, true, true },
			{ true, true, true, true, true, true },
			{ false, true, true, true, true, true },
			{ true, true, true, true, true, true } };

	protected int scale = 2 * Scale.get();
	protected int cx = 6 * scale;
	protected int cy = 2 * scale;

	protected ImageLoader imageLoader = new ImageLoader();

	// For scrollable implementation.
	private int maxUnitIncrement = 1;

	private boolean hexSelected = false;
	
	// //////////
	// Abstract Methods
	// /////////
	protected abstract void setupHexesGUI();

	protected abstract void setupEntrancesGUI();

	/**
	 * Add terrain, hexsides, elevation, and exits to hexes. Cliffs are
	 * bidirectional; other hexside obstacles are noted only on the high side,
	 * since they only interfere with uphill movement.
	 */
	private static synchronized void setupHexesGameState(String terrain,
			GUIHex[][] h, boolean serverSideFirstLoad)
	{
		ArrayList directories = null;
		String rndSourceName = null;
		BattleHex[][] hexModel = new BattleHex[h.length][h[0].length];
		for (int i = 0; i < h.length; i++)
		{
			for (int j = 0; j < h[0].length; j++)
			{
				if (show[i][j])
				{
					hexModel[i][j] = new BattleHex(i, j);
				}
			}
		}
		try
		{
			if ((rndSourceName == null) || (!serverSideFirstLoad))
			{ // static Battlelands
				// InputStream batIS = ResourceLoader.getInputStream(
				// terrain + ".xml", directories);

				/*
				 * BattlelandLoader bl = new BattlelandLoader(batIS, hexModel);
				 * List tempTowerStartList = bl.getStartList(); if
				 * (tempTowerStartList != null) { startlistMap.put(terrain,
				 * tempTowerStartList); } towerStatusMap.put(terrain, new
				 * Boolean(bl.isTower())); subtitleMap.put(terrain,
				 * bl.getSubtitle());
				 */
			}

			/* count all hazards & hazard sides */

			/* slow & inefficient... */
			final String[] hazards = null; // BattleHex.getTerrains();
			HashMap t2n = new HashMap();
			for (int i = 0; i < hazards.length; i++)
			{
				int count = 0;
				for (int x = 0; x < 6; x++)
				{
					for (int y = 0; y < 6; y++)
					{
						if (show[x][y])
						{
							if (hexModel[x][y].getTerrain().equals(hazards[i]))
							{
								count++;
							}
						}
					}
				}
				if (count > 0)
				{
					t2n.put(hazards[i], new Integer(count));
				}
			}
			hazardNumberMap.put(terrain, t2n);
			char[] hazardSides = BattleHex.getHexsides();
			HashMap s2n = new HashMap();
			for (int i = 0; i < hazardSides.length; i++)
			{
				int count = 0;
				for (int x = 0; x < 6; x++)
				{
					for (int y = 0; y < 6; y++)
					{
						if (show[x][y])
						{
							for (int k = 0; k < 6; k++)
							{
								if (hexModel[x][y].getHexside(k) == hazardSides[i])
								{
									count++;
								}
							}
						}
					}
				}
				if (count > 0)
				{
					s2n.put(new Character(hazardSides[i]), new Integer(count));
				}
			}
			hazardSideNumberMap.put(terrain, s2n);
			// map model into GUI
			for (int i = 0; i < hexModel.length; i++)
			{
				BattleHex[] row = hexModel[i];
				for (int j = 0; j < row.length; j++)
				{
					BattleHex hex = row[j];
					if (show[i][j])
					{
						h[i][j].setHexModel(hex);
					}
				}
			}
		}
		catch (Exception e)
		{
			Log.error("Battleland " + terrain + " loading failed : " + e);
			e.printStackTrace();
		}
	}

	void setupHexes()
	{
		setupHexesGUI();
		// setupHexesGUI();
		// setupHexesGameState(terrain, h, false);
		// setupNeighbors(h);
		// setupEntrances();
	}

	/**
	 * Add references to neighbor hexes.
	 * 
	 * TODO: This assumes only a 6x6 hex grid. We'll need to modify this to
	 * apply to an arbitrarily sized grid.
	 * 
	 */
	protected static void setupNeighbors(GUIHex[][] h)
	{
		for (int i = 0; i < h.length; i++)
		{
			for (int j = 0; j < h[0].length; j++)
			{
				if (show[i][j])
				{
					if (j > 0 && show[i][j - 1])
					{
						h[i][j].setNeighbor(0, h[i][j - 1]);
					}

					if (i < 5 && show[i + 1][j - ((i + 1) & 1)])
					{
						h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);
					}

					if (i < 5 && j + (i & 1) < 6 && show[i + 1][j + (i & 1)])
					{
						h[i][j].setNeighbor(2, h[i + 1][j + (i & 1)]);
					}

					if (j < 5 && show[i][j + 1])
					{
						h[i][j].setNeighbor(3, h[i][j + 1]);
					}

					if (i > 0 && j + (i & 1) < 6 && show[i - 1][j + (i & 1)])
					{
						h[i][j].setNeighbor(4, h[i - 1][j + (i & 1)]);
					}

					if (i > 0 && show[i - 1][j - ((i + 1) & 1)])
					{
						h[i][j].setNeighbor(5, h[i - 1][j - ((i + 1) & 1)]);
					}
				}
			}
		}
	}

	// TODO: This needs to be changed
	protected static void setupEntrancesGameState(GUIHex[] entrances,
			GUIHex[][] h)
	{
		entrances[0].setNeighbor(3, h[3][0]);
		entrances[0].setNeighbor(4, h[4][1]);
		entrances[0].setNeighbor(5, h[5][1]);

		entrances[1].setNeighbor(3, h[5][1]);
		entrances[1].setNeighbor(4, h[5][2]);
		entrances[1].setNeighbor(5, h[5][3]);
		entrances[1].setNeighbor(0, h[5][4]);

		entrances[2].setNeighbor(4, h[5][4]);
		entrances[2].setNeighbor(5, h[4][5]);
		entrances[2].setNeighbor(0, h[3][5]);

		entrances[3].setNeighbor(5, h[3][5]);
		entrances[3].setNeighbor(0, h[2][5]);
		entrances[3].setNeighbor(1, h[1][4]);
		entrances[3].setNeighbor(2, h[0][4]);

		entrances[4].setNeighbor(0, h[0][4]);
		entrances[4].setNeighbor(1, h[0][3]);
		entrances[4].setNeighbor(2, h[0][2]);

		entrances[5].setNeighbor(1, h[0][2]);
		entrances[5].setNeighbor(2, h[1][1]);
		entrances[5].setNeighbor(3, h[2][1]);
		entrances[5].setNeighbor(4, h[3][0]);
	}

	protected void setupEntrances()
	{
		setupEntrancesGUI();
		setupEntrancesGameState(entrances, h);
	}

	void unselectAllHexes()
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.isSelected())
			{
				hex.unselect();
				hex.repaint();
			}
		}
	}

	void unselectHexByLabel(String label)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.isSelected() && label.equals(hex.getHexModel().getLabel()))
			{
				hex.unselect();
				hex.repaint();
				return;
			}
		}
	}

	void unselectHexesByLabels(Set labels)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.isSelected()
					&& labels.contains(hex.getHexModel().getLabel()))
			{
				hex.unselect();
				hex.repaint();
			}
		}
	}

	void selectHexByLabel(String label)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (!hex.isSelected() && label.equals(hex.getHexModel().getLabel()))
			{
				hex.select();
				hex.repaint();
				return;
			}
		}
	}

	void selectHexesByLabels(Set labels)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (!hex.isSelected()
					&& labels.contains(hex.getHexModel().getLabel()))
			{
				hex.select();
				hex.repaint();
			}
		}
	}

	/**
	 * Do a brute-force search through the hex array, looking for a match.
	 * Return the hex, or null.
	 */
	GUIHex getGUIHexByLabel(String label)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.getHexModel().getLabel().equals(label))
			{
				return hex;
			}
		}

		Log.error("Could not find GUIHex " + label);
		return null;
	}

	/** Look for the Hex matching the Label in the terrain static map */
	public static BattleHex getHexByLabel(String terrain, String label)
	{
		int x = 0;
		int y = Integer.parseInt(new String(label.substring(1)));
		switch (label.charAt(0))
		{
			case 'A':
			case 'a':
				x = 0;
				break;

			case 'B':
			case 'b':
				x = 1;
				break;

			case 'C':
			case 'c':
				x = 2;
				break;

			case 'D':
			case 'd':
				x = 3;
				break;

			case 'E':
			case 'e':
				x = 4;
				break;

			case 'F':
			case 'f':
				x = 5;
				break;

			case 'X':
			case 'x':

				/* entrances */
				GUIHex[] gameEntrances = (GUIHex[]) entranceHexes.get(terrain);
				return gameEntrances[y].getBattleHexModel();

			default:
				Log.error("Label " + label + " is invalid");
		}
		y = 6 - y - (int) Math.abs(((x - 3) / 2));
		GUIHex[][] correctHexes = (GUIHex[][]) terrainH.get(terrain);
		return correctHexes[x][y].getBattleHexModel();
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

	Set getAllHexLabels()
	{
		Set set = new HashSet();
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			BattleHex hex = (BattleHex) it.next();
			set.add(hex.getLabel());
		}
		return set;
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
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
				if (!hex.getBattleHexModel().isEntrance()
						&& rectClip.intersects(hex.getBounds()))
				{
					hex.paint(g);
				}
			}

			/* always antialias this, the font is huge */
			Object oldantialias = g2
					.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			Font oldFont = g.getFont();
			FontMetrics fm;
			String dn = "FOO"; // getMasterHex().getTerrainDisplayName();
			String bn = null; // getMasterHex().getTerrainName();
			String sub = null; // (String)subtitleMap.get(terrain);

			if (sub == null)
			{
				sub = (dn.equals(bn) ? null : bn);
			}

			// g.setFont(ResourceLoader.defaultFont.deriveFont((float)48));
			fm = g.getFontMetrics();
			int tma = fm.getMaxAscent();
			g.drawString(dn, 80, 4 + tma);

			if (sub != null)
			{
				// g.setFont(ResourceLoader.defaultFont.deriveFont((float)24));
				fm = g.getFontMetrics();
				int tma2 = fm.getMaxAscent();
				g.drawString(sub, 80, 4 + tma + 8 + tma2);
			}

			/* reset antialiasing */
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldantialias);
			g.setFont(oldFont);
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
		return new Dimension(75 * Scale.get(), 50 * Scale.get());
	}

	public void mouseClicked(MouseEvent arg0)
	{
		Point point = arg0.getPoint();

		try
		{
			GUIHex hex = getHexContainingPoint(point);
			
			if(hex.isSelected() && hexSelected == true)
			{
				hex.x_adjust = hex.x_adjust_arr[hex.arr_index];
				hex.y_adjust = hex.y_adjust_arr[hex.arr_index];
				hex.rotation = hex.rotation_arr[hex.arr_index];
				
				hex.rotateHexCW();
			}
			else if (!hex.isSelected() && hexSelected == false)
			{
				hex.setSelected(true);
				hexSelected = true;
			}
			else
			{
				unselectAllHexes();
				hex.setSelected(true);
				hexSelected = true;
			}
			
			//FIXME: THIS REPAINT IS BROKEN
			hex.repaint();
			
			/* Remove this statement to enable subsequent clicks again */
			// setToolTipText (hex.getToolTip());
		}
		catch (NullPointerException e)
		{
			// No hex clicked, no rotation needed.
		}
	}

	public void mouseEntered(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public Dimension getPreferredScrollableViewportSize()
	{
		return getMinimumSize();
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect,
			int orientation, int direction)
	{
		if (orientation == SwingConstants.HORIZONTAL)
		{
			return visibleRect.width - maxUnitIncrement;
		}
		else
		{
			return visibleRect.height - maxUnitIncrement;
		}
	}

	public boolean getScrollableTracksViewportHeight()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean getScrollableTracksViewportWidth()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation, int direction)
	{
		// Get the current position.
		int currentPosition = 0;
		if (orientation == SwingConstants.HORIZONTAL)
			currentPosition = visibleRect.x;
		else
			currentPosition = visibleRect.y;

		// Return the number of pixels between currentPosition
		// and the nearest tick mark in the indicated direction.
		if (direction < 0)
		{
			int newPosition = currentPosition
					- (currentPosition / maxUnitIncrement) * maxUnitIncrement;
			return (newPosition == 0) ? maxUnitIncrement : newPosition;
		}
		else
		{
			return ((currentPosition / maxUnitIncrement) + 1)
					* maxUnitIncrement - currentPosition;
		}
	}

}
