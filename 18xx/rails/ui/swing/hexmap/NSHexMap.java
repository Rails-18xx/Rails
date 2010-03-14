/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/NSHexMap.java,v 1.14 2010/03/14 09:14:04 stefanfrey Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;
import java.util.ArrayList;

import rails.game.MapHex;
import rails.ui.swing.Scale;

/**
 * Class NSHexMap displays a basic hex map with NS exit orientation.
 */

public class NSHexMap extends HexMap {
    private static final long serialVersionUID = 1L;

    public NSHexMap() {
        cx = 0;
        cy = -scale / 2;
        scale = defaultScale = Scale.get();

    }

    @Override
    protected void setupHexesGUI() {
        hexes = new ArrayList<GUIHex>();

        hexArray = mapManager.getHexes();
        MapHex mh;
        int ii, jj;
        
        h = new GUIHex[hexArray.length][hexArray[0].length];
        for (int i = minX; i < hexArray.length; i++) {
            ii = i - minX + 1;
            for (int j = minY; j < hexArray[0].length; j++) {
                jj = j - minY + 1;
                mh = hexArray[i][j];
                if (mh != null) {
                    GUIHex hex =
                            new GUIHex(this, Math.round(cx + 3 * ii
                                                              * scale),
                                    (int) Math.round(cy + (2 * jj + (ii & 1))
                                                     * GUIHex.SQRT3 * scale),
                                    scale, ii, jj);

                    hex.setHexModel(mh);
                    hex.originalTileId = hex.currentTileId;
                    hexesByName.put(mh.getName(), hex);

                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
        setSize();
    }

    protected void setSize() {
        preferredSize =
                new Dimension(
                        (int) Math.round((hexArray.length + 1) * 3 * scale * zoomFactor),
                        (int) Math.round((hexArray[0].length + 1) * 2
                                         * GUIHex.SQRT3 * scale * zoomFactor));
    }

    @Override
    protected void scaleHexesGUI() {

        hexArray = mapManager.getHexes();
        GUIHex hex;
        int ii, jj;
        
        for (int i = 0; i < hexArray.length; i++) {
            ii = i - minX + 1;
            for (int j = 0; j < hexArray[0].length; j++) {
                jj = j - minY + 1;
                hex = h[i][j];
                if (hex != null) {
                    hex.scaleHex(cx + 3 * ii * scale,
                                 cy + (2 * jj + (ii & 1)) * GUIHex.SQRT3 * scale,
                                 scale, zoomFactor);
                }
            }
        }
        setSize();
    }

    @Override
    public void paint(Graphics g) {

        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        String label;
        int ii, jj;

        boolean lettersGoHorizontal = mapManager.lettersGoHorizontal();
        int xOffset = mapManager.letterAHasEvenNumbers() ? 1 : 0;
        int xLeft = cx + 10;
        int xRight = (cx + 5 + 3 * scale * hexArray.length);
        int yTop = 20;
        int yBottom = (int)(cy + 20 + 2 * hexArray[0].length * GUIHex.SQRT3 * scale);

        for (int i = 1; i < hexArray.length; i++) {
            ii = i - minX + 1;
            label = lettersGoHorizontal
                    ? String.valueOf((char)('@'+i))
                    : String.valueOf(i);
            g2.drawString(label,
                    (cx - 30 -3*label.length() + 3 * scale * (ii + xOffset)),
                    yTop);
            g2.drawString(label,
                    (cx - 30 -3*label.length() + 3 * scale * (ii + xOffset)),
                    yBottom);
        }

        for (int j = 1; j < 2 * hexArray[0].length; j++) {
            jj = j - minY + 1;
            label = lettersGoHorizontal
                    ? String.valueOf(j)
                    : String.valueOf((char)('@'+j));
            g2.drawString(label,
                    xLeft,
                    (int)(cy + 56 + jj * GUIHex.SQRT3 * scale));
            g2.drawString(label,
                    xRight,
                    (int)(cy + 56 + jj * GUIHex.SQRT3 * scale));
        }
    }
}
