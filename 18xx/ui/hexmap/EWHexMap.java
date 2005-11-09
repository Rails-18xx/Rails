package ui.hexmap;

import java.awt.Dimension;
import java.util.ArrayList;

import ui.Scale;

import game.*;

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
		hexes = new ArrayList();

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
					GUIEWHex hex = new GUIEWHex(
							(cx + scale	* ((GUIHex.SQRT3 * i) + (GUIHex.SQRT3 / 2 * (j & 1)))),
							(cy + j * 1.5 * scale), 
							scale, this, i, j);
					
					hex.setName(mh.getName());
					hex.setTileId(mh.getPreprintedTileId());
					hex.setTileOrientation(mh
							.getPreprintedTileOrientation());
					hex.setTileFilename(mh.getTileFileName());
					hex.setHexModel(mh);

					imageLoader.loadTile(mh.getPreprintedTileId());
					hex.setTileImage(imageLoader.getTile(mh
							.getPreprintedTileId()));
					hex.x_adjust = hex.x_adjust_arr[hex.tileOrientation];
					hex.y_adjust = hex.y_adjust_arr[hex.tileOrientation];
					hex.rotation = hex.rotation_arr[hex.tileOrientation];
					
					h[i][j] = hex;
					hexes.add(hex);
				}
			}
		}
        preferredSize = new Dimension(
                (int)Math.round((hexArray.length +1 ) * GUIHex.SQRT3 * scale),
                (int)Math.round((hexArray[0].length + 1) * 1.5 * scale));
	}
}
