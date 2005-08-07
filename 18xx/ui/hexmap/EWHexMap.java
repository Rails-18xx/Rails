package ui.hexmap;


import java.awt.Color;

/**
 * Class EWHexMap displays a basic hex map with EW exit orientation.
 */

public class EWHexMap extends HexMap
{
    public EWHexMap()
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
                    GUIEWHex hex = new GUIEWHex(
                        (cx + scale * ((GUIHex.SQRT3 * i) + (GUIHex.SQRT3/2 * (j & 1)))),
                        (cy + j * 1.5 * scale),
                        scale, this, i, j);
                    
                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
    }

    protected void setupEntrancesGUI()
    {
        // Initialize entrances.
        entrances[0] = new GUIEWHex(cx + 15 * scale,
            (int)Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new GUIEWHex(cx + 21 * scale,
            (int)Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new GUIEWHex(cx + 17 * scale,
            (int)Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new GUIEWHex(cx + 2 * scale,
            (int)Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new GUIEWHex(cx - 3 * scale,
            (int)Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new GUIEWHex(cx + 1 * scale,
            (int)Math.round(cy + 1 * scale), scale, this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);
    }
}
