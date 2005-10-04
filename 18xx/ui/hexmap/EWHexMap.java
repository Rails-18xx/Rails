package ui.hexmap;

import game.*;

import java.awt.Color;

/**
 * Class EWHexMap displays a basic hex map with EW exit orientation.
 */

public class EWHexMap extends HexMap
{

	public EWHexMap()
	{
		setupHexes();
	}

	protected void setupHexesGUI()
	{
		hexes.clear();

		MapManager mmgr = MapManager.getInstance();
		MapHex[][] hexArray = mmgr.getHexes();
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
	}

	protected void setupEntrancesGUI()
	{
		// Initialize entrances.
		entrances[0] = new GUIEWHex(cx + 15 * scale, (int) Math.round(cy + 1
				* scale), scale, this, -1, 0);
		entrances[1] = new GUIEWHex(cx + 21 * scale, (int) Math.round(cy + 10
				* scale), scale, this, -1, 1);
		entrances[2] = new GUIEWHex(cx + 17 * scale, (int) Math.round(cy + 22
				* scale), scale, this, -1, 2);
		entrances[3] = new GUIEWHex(cx + 2 * scale, (int) Math.round(cy + 21
				* scale), scale, this, -1, 3);
		entrances[4] = new GUIEWHex(cx - 3 * scale, (int) Math.round(cy + 10
				* scale), scale, this, -1, 4);
		entrances[5] = new GUIEWHex(cx + 1 * scale, (int) Math.round(cy + 1
				* scale), scale, this, -1, 5);

		hexes.add(entrances[0]);
		hexes.add(entrances[1]);
		hexes.add(entrances[2]);
		hexes.add(entrances[3]);
		hexes.add(entrances[4]);
		hexes.add(entrances[5]);
	}
}
