/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/EWHexMap.java,v 1.14 2010/03/11 20:38:19 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;
import java.util.ArrayList;

import rails.game.MapHex;
import rails.ui.swing.Scale;

/**
 * Class EWHexMap displays a basic hex map with EW exit orientation.
 */

public class EWHexMap extends HexMap {
    private static final long serialVersionUID = 1L;

    public EWHexMap() {
        scale = defaultScale = 2 * Scale.get();
        //cx = scale / 2;
        cx = -scale/2;
        cy = scale/2;
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
                            new GUIHex(
                                    this,
                                    (cx + scale
                                          * ((GUIHex.SQRT3 * ii) + (GUIHex.SQRT3 / 2 * (j & 1)))),
                                    (cy + jj * 1.5 * scale), scale, ii, jj);

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
                        (int) Math.round((maxX-minX + 3) * GUIHex.SQRT3
                                         * scale * zoomFactor),
                        (int) Math.round((maxY-minY + 3) * 1.5 * scale * zoomFactor));
    }


    @Override
    protected void scaleHexesGUI  () {

        hexArray = mapManager.getHexes();
        GUIHex hex;
        int ii, jj;
        for (int i = minX; i < hexArray.length; i++) {
            ii = i - minX + 1;
            for (int j = minY; j < hexArray[0].length; j++) {
                jj = j - minY + 1;
                hex = h[i][j];
                if (hex != null) {
                    hex.scaleHex(cx + scale * ((GUIHex.SQRT3 * ii) + (GUIHex.SQRT3 / 2 * (jj & 1))),
                                 cy + jj * 1.5 * scale,
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
        int xOffset = (mapManager.letterAHasEvenNumbers() ? 0 : 1) + (minCol % 2);
        int xLeft = cx + scale;
        int xRight = (int)(cx + scale * (3 + GUIHex.SQRT3/2 * (maxCol-minCol+1)));
        int yTop = cy;
        int yBottom = (int)(cy + scale + (maxRow-minRow+1) * 1.5 * scale);

        for (int i = minCol; i <= maxCol; i++) {
            ii = i - minCol + 1;
            label = lettersGoHorizontal
                    ? String.valueOf((char)('@'+i))
                    : String.valueOf(i);
            g2.drawString(label,
                    (int) (cx + (26-3*label.length()) + scale * (GUIHex.SQRT3/2 * (ii + xOffset))),
                    yTop);
            g2.drawString(label,
                    (int) (cx + (26-3*label.length()) + scale * (GUIHex.SQRT3/2 * (ii + xOffset))),
                    yBottom);
        }

        for (int j = minRow; j <= maxRow; j++) {
            jj = j - minRow + 1;
            label = lettersGoHorizontal
                    ? String.valueOf(j)
                    : String.valueOf((char)('@'+j));
            g2.drawString(label,
                    xLeft,
                    (int)(cy - 10 + jj * 1.5 * scale));
            g2.drawString(label,
                    xRight,
                    (int)(cy - 10 + jj * 1.5 * scale));
        }


    }

}
