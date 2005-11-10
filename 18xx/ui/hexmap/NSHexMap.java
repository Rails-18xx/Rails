package ui.hexmap;


import java.awt.Dimension;
import java.util.ArrayList;

import game.*;
import ui.*;

/**
 * Class NSHexMap displays a basic hex map with NS exit orientation.
 */

public class NSHexMap extends HexMap
{
    public NSHexMap()
    {
        setupHexes();
        cx = 0;
        cy = -scale/2;
    }

    protected void setupHexesGUI()
    {
        hexes = new ArrayList();
        
        scale = Scale.get();

        MapManager mmgr = MapManager.getInstance();
        hexArray = mmgr.getHexes();
        MapHex mh; 
        h = new GUIHex[hexArray.length][hexArray[0].length];
        for (int i = 0; i < hexArray.length; i++)
        {
            for (int j = 0; j < hexArray[0].length; j++)
            {
                mh = hexArray[i][j];
                if (mh != null) {
                     GUINSHex hex = new GUINSHex(
                    		 (int)Math.round(cx + 3 * i * scale),
                    		 (int)Math.round(cy + (2 * j + (i & 1)) * GUIHex.SQRT3 * scale), 
                    		 scale, i, j);

                    hex.setName(mh.getName());
                    hex.setTileId(mh.getPreprintedTileId());
                    hex.setTileOrientation(mh.getPreprintedTileOrientation());
                    hex.setTileFilename(mh.getTileFileName());
                    hex.setHexModel(mh);
                    
                    imageLoader.loadTile(hexArray[i][j].getPreprintedTileId());
                    hex.setTileImage(imageLoader.getTile(hexArray[i][j].getPreprintedTileId()));
                	hex.x_adjust = hex.x_adjust_arr[hex.tileOrientation];
                	hex.y_adjust = hex.y_adjust_arr[hex.tileOrientation];
                	hex.rotation = hex.rotation_arr[hex.tileOrientation];
                	
                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
        preferredSize = new Dimension(
                (hexArray.length +1 ) * 3 * scale,
                (int)Math.round((hexArray[0].length + 1) * 2 * GUIHex.SQRT3 * scale));
    }
}
