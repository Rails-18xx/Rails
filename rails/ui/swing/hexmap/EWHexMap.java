/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/EWHexMap.java,v 1.11 2009/12/08 19:31:49 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;
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

    @Override
	public void paint(Graphics g) {

    	super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        String label;

        boolean lettersGoHorizontal = mapManager.lettersGoHorizontal();
        int xOffset = mapManager.letterAHasEvenNumbers() ? 1 : 0;
        int xLeft = cx + 10;
        int xRight = (int)(cx + 5 + scale * (GUIHex.SQRT3/2 * 2*hexArray.length));
        int yTop = cy + 10;
        int yBottom = (int)(cy - 10 + hexArray[0].length * 1.5 * scale);

        for (int i = 1; i < 2*hexArray.length; i++) {
        	label = lettersGoHorizontal
        			? String.valueOf((char)('@'+i))
        			: String.valueOf(i);
        	g2.drawString(label,
        			(int) (cx + (26-3*label.length()) + scale * (GUIHex.SQRT3/2 * (i + xOffset))),
        			yTop);
        	g2.drawString(label,
        			(int) (cx + (26-3*label.length()) + scale * (GUIHex.SQRT3/2 * (i + xOffset))),
        			yBottom);
        }

        for (int j = 1; j < hexArray[0].length; j++) {
        	label = lettersGoHorizontal
					? String.valueOf(j)
					: String.valueOf((char)('@'+j));
        	g2.drawString(label,
        			xLeft,
        			(int)(cy - 10 + j * 1.5 * scale));
        	g2.drawString(label,
        			xRight,
        			(int)(cy - 10 + j * 1.5 * scale));
        }


    }

}
