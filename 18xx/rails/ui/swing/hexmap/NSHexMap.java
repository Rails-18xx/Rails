/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/NSHexMap.java,v 1.12 2010/01/31 22:22:36 macfreek Exp $*/
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
        h = new GUIHex[hexArray.length][hexArray[0].length];
        for (int i = 0; i < hexArray.length; i++) {
            for (int j = 0; j < hexArray[0].length; j++) {
                mh = hexArray[i][j];
                if (mh != null) {
                    GUIHex hex =
                            new GUIHex(this, Math.round(cx + 3 * i
                                                              * scale),
                                    (int) Math.round(cy + (2 * j + (i & 1))
                                                     * GUIHex.SQRT3 * scale),
                                    scale, i, j);

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
        for (int i = 0; i < hexArray.length; i++) {
            for (int j = 0; j < hexArray[0].length; j++) {
                hex = h[i][j];
                if (hex != null) {
                    hex.scaleHex(cx + 3 * i * scale,
                                 cy + (2 * j + (i & 1)) * GUIHex.SQRT3 * scale,
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

        boolean lettersGoHorizontal = mapManager.lettersGoHorizontal();
        int xOffset = mapManager.letterAHasEvenNumbers() ? 1 : 0;
        int xLeft = cx + 10;
        int xRight = (cx + 5 + 3 * scale * hexArray.length);
        int yTop = 20;
        int yBottom = (int)(cy + 20 + 2 * hexArray[0].length * GUIHex.SQRT3 * scale);

        for (int i = 1; i < hexArray.length; i++) {
            label = lettersGoHorizontal
                    ? String.valueOf((char)('@'+i))
                    : String.valueOf(i);
            g2.drawString(label,
                    (cx - 30 -3*label.length() + 3 * scale * (i + xOffset)),
                    yTop);
            g2.drawString(label,
                    (cx - 30 -3*label.length() + 3 * scale * (i + xOffset)),
                    yBottom);
        }

        for (int j = 1; j < 2 * hexArray[0].length; j++) {
            label = lettersGoHorizontal
                    ? String.valueOf(j)
                    : String.valueOf((char)('@'+j));
            g2.drawString(label,
                    xLeft,
                    (int)(cy + 56 + j * GUIHex.SQRT3 * scale));
            g2.drawString(label,
                    xRight,
                    (int)(cy + 56 + j * GUIHex.SQRT3 * scale));
        }
    }
}
