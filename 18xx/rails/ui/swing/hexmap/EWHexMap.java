/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/EWHexMap.java,v 1.16 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;

/**
 * Class EWHexMap displays a basic hex map with EW exit orientation.
 */

public class EWHexMap extends HexMap {
    private static final long serialVersionUID = 1L;

    public EWHexMap() {
        // tile x-reference in EW is left side
        tileXOffset = -0.5;
        // tile y-reference in EW is 1/3 of the baseline 
        tileYOffset = 0.333;
        
        // coordinate margins
        coordinateXMargin = coordinateFlatMargin;
        coordinateYMargin = coordinatePeakMargin;
    }
    
    @Override
    protected double calcXCoordinates(int col, double offset) {
        double colAdj = (col - minCol)/2.0 + flatMargin + offset;
//        log.debug("x-Coordinate for col= " + col + " -> colAdj = " + colAdj);
        return Math.round(scale * 2 * GUIHex.SQRT3 * colAdj);
    }
    
    @Override
    protected double calcYCoordinates(int row, double offset) {
       double rowAdj = row  - minRow  + peakMargin+ offset; 
//       log.debug("y-Coordinate for row= " + row + " -> rowAdj = " + rowAdj);
       return Math.round(scale * 3 * rowAdj);
    }
    
    @Override
    protected void setSize() {
        preferredSize = new Dimension( (int) calcXCoordinates(maxCol, flatMargin), 
                        (int) calcYCoordinates(maxRow, peakMargin));
    }

}
