/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/EWHexMap.java,v 1.4 2007/12/04 20:25:19 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.Dimension;
import java.util.ArrayList;

import rails.game.*;
import rails.ui.swing.ORUIManager;


/**
 * Class EWHexMap displays a basic hex map with EW exit orientation.
 */

public class EWHexMap extends HexMap
{

	public EWHexMap()
	{
		setupHexes();
		cx = scale / 2;
		cy = 0;
	}

	protected void setupHexesGUI()
	{
		hexes = new ArrayList<GUIHex>();

		MapManager mmgr = MapManager.getInstance();
		hexArray = mmgr.getHexes();
		MapHex mh;
		h = new GUIHex[hexArray.length][hexArray[0].length];
		for (int i = 0; i < hexArray.length; i++)
		{
			for (int j = 0; j < hexArray[0].length; j++)
			{
				mh = hexArray[i][j];
				if (mh != null)
				{
					GUIHex hex = new GUIHex(this, (cx + scale
							* ((GUIHex.SQRT3 * i) + (GUIHex.SQRT3 / 2 * (j & 1)))),
							(cy + j * 1.5 * scale),
							scale,
							i,
							j);

					hex.setHexModel(mh);
					hex.originalTileId = hex.currentTileId;

					h[i][j] = hex;
					hexes.add(hex);
				}
			}
		}
		preferredSize = new Dimension((int) Math.round((hexArray.length + 1)
				* GUIHex.SQRT3 * scale),
				(int) Math.round((hexArray[0].length + 1) * 1.5 * scale));
	}
}
