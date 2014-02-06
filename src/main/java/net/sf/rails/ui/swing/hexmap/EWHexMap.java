package net.sf.rails.ui.swing.hexmap;

import java.awt.*;

import net.sf.rails.game.MapOrientation;


/**
 * Class EWHexMap displays a basic hex map with
 * {@link net.sf.rails.game.MapOrientation#EW EW} exit orientation.
 */

public class EWHexMap extends HexMap {
    private static final long serialVersionUID = 1L;

    public EWHexMap() {
        // tile x-reference in EW is left side
        tileXOffset = -0.5f;
        // tile y-reference in EW is 1/3 of the baseline 
        tileYOffset = 0.333f;
        
        // coordinate margins
        coordinateXMargin = coordinateFlatMargin;
        coordinateYMargin = coordinatePeakMargin;
    }
    
    @Override
    protected double calcXCoordinates(int col, double offset) {
        double colAdj = (col - minimum.getCol()) / 2.0f + flatMargin + offset;
//        log.debug("x-Coordinate for col= " + col + " -> colAdj = " + colAdj);
        return Math.round(scale * 2 * MapOrientation.SQRT3 * colAdj);
    }
    
    @Override
    protected double calcYCoordinates(int row, double offset) {
       double rowAdj = row  - minimum.getRow()  + peakMargin+ offset; 
//       log.debug("y-Coordinate for row= " + row + " -> rowAdj = " + rowAdj);
       return Math.round(scale * 3 * rowAdj);
    }
    
    protected void setOriginalSize() {
        originalSize = new Dimension( (int) calcXCoordinates(maximum.getCol(), flatMargin), 
                        (int) calcYCoordinates(maximum.getRow(), peakMargin));
    }

}
