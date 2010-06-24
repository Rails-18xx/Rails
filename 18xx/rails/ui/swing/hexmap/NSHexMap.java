/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/NSHexMap.java,v 1.15 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;

/**
 * Class NSHexMap displays a basic hex map with NS exit orientation.
 */

public class NSHexMap extends HexMap {
    private static final long serialVersionUID = 1L;

    public NSHexMap() {
        // tile x-reference in NS is 1/3 of the baseline 
        tileXOffset = -0.333;
        // tile y-reference in NS is baseline
        tileYOffset = -0.5;
        
        // coordinate margins
        coordinateXMargin = coordinatePeakMargin;
        coordinateYMargin = coordinateFlatMargin;
    }

    protected double calcXCoordinates(int col, double offset) {
        double colAdj = col - minCol + peakMargin + offset;
//        log.debug("x-Coordinate for col= " + col + " -> colAdj = " + colAdj);
        return Math.round(scale * 3 * colAdj);
    }
    
    protected double calcYCoordinates(int row, double offset) {
       double rowAdj = (row - minRow)/2.0 + flatMargin + offset; 
//       log.debug("y-Coordinate for row= " + row + " -> rowAdj = " + rowAdj);
       return Math.round(scale * 2 * GUIHex.SQRT3 * rowAdj);
    }

    protected void setSize() {
        preferredSize = new Dimension( (int) calcXCoordinates(maxCol, peakMargin), 
                (int) calcYCoordinates(maxRow, flatMargin));
    }

}
