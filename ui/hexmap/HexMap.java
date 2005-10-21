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
 * Base class that stores common info for HexMap independant of Hex orientations. 
 */
public abstract class HexMap extends JComponent implements MouseListener,
		WindowListener
{
	// Abstract Methods
	protected abstract void setupHexesGUI();
	
	// GUI hexes need to be recreated for each object, since scale varies.
	protected GUIHex[][] h;
	protected ArrayList hexes;
	
	protected int scale = 2 * Scale.get();
	protected int cx = 6 * scale;
	protected int cy = 2 * scale;

	protected ImageLoader imageLoader = new ImageLoader();
	private boolean hexSelected = false;

	void setupHexes()
	{
		setupHexesGUI();
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
				this.repaint();
			}
		}
	}

	/** Look for the Hex matching the Label in the terrain static map */
	/* EV: useful, but needs to be rewritten and moved to MapHex */
	public static MapHex getHexByLabel(String terrain, String label)
	{
	    /*
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

				// entrances
				GUIHex[] gameEntrances = (GUIHex[]) entranceHexes.get(terrain);
				return gameEntrances[y].getMapHexModel();

			default:
				Log.error("Label " + label + " is invalid");
		}
		y = 6 - y - (int) Math.abs(((x - 3) / 2));
		GUIHex[][] correctHexes = (GUIHex[][]) terrainH.get(terrain);
		return correctHexes[x][y].getMapHexModel();
		*/
		return null;
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
		try
		{
			super.paintComponent(g);
			
			// Abort if called too early.
			Rectangle rectClip = g.getClipBounds();
			if (rectClip == null)
			{
				return;
			}

			/*
			 * FIXME: The repaint bugs are caused by something near here.
			 * Changing this from an iterator to a for loop affects the bug.
			 * Only the first element in the arraylist is always being repainted
			 * correctly. All others aren't. It seems like the clipping area's
			 * coordinates are not being translated into the whole window's
			 * coordinates. The paint draws from 0,0 even if the clipped area is
			 * located elsewhere.
			 */
			System.out.println(rectClip);

			for (int x = (hexes.size() - 1); x > 0; x--)
			{
				GUIHex hex = (GUIHex) hexes.get(x);
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
		return new Dimension(60 * Scale.get(), 45 * Scale.get());
	}

	public void mouseClicked(MouseEvent arg0)
	{
		Point point = arg0.getPoint();

		try
		{
			GUIHex hex = getHexContainingPoint(point);

			// Temporary, to check for correct neighbour setting
			StringBuffer b = new StringBuffer();
			b.append("Clicked hex ")
					.append(hex.getName())
					.append(" Neighbors:");
			MapHex[] nb = hex.getHexModel().getNeighbors();
			for (int i = 0; i < 6; i++)
			{
				if (nb[i] != null)
					b.append(" ").append(i).append(":").append(nb[i].getName());
			}
			System.out.println(b.toString());

			if (hex.isSelected() && hexSelected == true)
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

			this.repaint();

			/* Remove this statement to enable subsequent clicks again */
			// setToolTipText (hex.getToolTip());
		}
		catch (NullPointerException e)
		{
			// No hex clicked
			if (hexSelected)
			{
				unselectAllHexes();
				hexSelected = false;
			}
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

	public void windowActivated(WindowEvent e)
	{
		// TODO Auto-generated method stub
	}

	public void windowClosed(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowClosing(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowDeactivated(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

}
