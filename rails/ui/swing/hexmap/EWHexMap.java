/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/EWHexMap.java,v 1.10 2009/11/02 23:30:36 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.Dimension;
import java.util.ArrayList;

import rails.game.MapHex;

/**
 * Class EWHexMap displays a basic hex map with EW exit orientation.
 */

public class EWHexMap extends HexMap {
    private static final long serialVersionUID = 1L;

    public EWHexMap() {
        cx = scale / 2;
        cy = 0;
    }

    @Override
	protected void setupHexesGUI() {
        hexes = new ArrayList<GUIHex>();

        hexArray = mapManager.getHexes();
        MapHex mh;
        h = new GUIHex[hexArray.length][hexArray[0].length];
        for (int i = 0; i < hexArray.length; i++) {
            for (int j = 0; j < hexArray[0].length; j++) {
                mh = hexArray[i][j];
                if (mh != null) {
                    GUIHex hex =
                            new GUIHex(
                                    this,
                                    (cx + scale
                                          * ((GUIHex.SQRT3 * i) + (GUIHex.SQRT3 / 2 * (j & 1)))),
                                    (cy + j * 1.5 * scale), scale, i, j);

                    hex.setHexModel(mh);
                    hex.originalTileId = hex.currentTileId;
                    hexesByName.put(mh.getName(), hex);

                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
        preferredSize =
                new Dimension(
                        (int) Math.round((hexArray.length + 1) * GUIHex.SQRT3
                                         * scale),
                        (int) Math.round((hexArray[0].length + 1) * 1.5 * scale));
    }
}
