package ui.hexmap;


import game.MapHex;
import game.MapManager;

import java.awt.Color;

/**
 * Class NSHexMap displays a basic hex map with NS exit orientation.
 */

public class NSHexMap extends HexMap
{
    public NSHexMap()
    {
        setOpaque(true);
        setBackground(Color.white);
        setupHexes();
    }

    protected void setupHexesGUI()
    {
        hexes.clear();
        
        // Initialize hex array.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                     GUINSHex hex = new GUINSHex
                        ((int)Math.round(cx + 3 * i * scale),
                        (int)Math.round(cy + (2 * j + (i & 1)) *
                        GUIHex.SQRT3 * scale), scale, this, i, j);

                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
    }

    protected void setupHexesGUI_EV()
    {
        hexes.clear();
        
        scale /= 2;
        cx /= 2;
        cy /= 2;

        // Initialize hex array.
        /*
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                */
        MapManager mmgr = MapManager.getInstance();
        MapHex[][] hexArray = mmgr.getHexes();
        MapHex mh; 
        h = new GUIHex[hexArray.length][hexArray[0].length];
        for (int i = 0; i < hexArray.length; i++)
        {
            for (int j = 0; j < hexArray[0].length; j++)
            {
                mh = hexArray[i][j];
                if (mh != null) {
                     GUINSHex hex = new GUINSHex
                        ((int)Math.round(cx + 3 * i * scale),
                        (int)Math.round(cy + (2 * j + (i & 1)) *
                        GUIHex.SQRT3 * scale), scale, this, i, j);

                    hex.setName(hexArray[i][j].getName());
                    hex.setTileId(hexArray[i][j].getPreprintedTileId());
                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
    }

    protected void setupEntrancesGUI()
    {
        // Initialize entrances.
        entrances[0] = new GUINSHex(cx + 15 * scale,
            (int)Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new GUINSHex(cx + 21 * scale,
            (int)Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new GUINSHex(cx + 17 * scale,
            (int)Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new GUINSHex(cx + 2 * scale,
            (int)Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new GUINSHex(cx - 3 * scale,
            (int)Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new GUINSHex(cx + 1 * scale,
            (int)Math.round(cy + 1 * scale), scale, this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);
    }

}
