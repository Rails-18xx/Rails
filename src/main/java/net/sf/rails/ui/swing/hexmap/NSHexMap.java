package net.sf.rails.ui.swing.hexmap;

import java.awt.*;

import net.sf.rails.game.MapOrientation;


/**
 * Class NSHexMap displays a basic hex map with
 * {@link net.sf.rails.game.MapOrientation#NS NS} exit orientation.
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
        double colAdj = col - minimum.getCol() + peakMargin + offset;
//        log.debug("x-Coordinate for col= " + col + " -> colAdj = " + colAdj);
        return Math.round(scale * 3 * colAdj);
    }
    
    protected double calcYCoordinates(int row, double offset) {
       double rowAdj = (row - minimum.getCol())/2.0 + flatMargin + offset; 
//       log.debug("y-Coordinate for row= " + row + " -> rowAdj = " + rowAdj);
       return Math.round(scale * 2 * MapOrientation.SQRT3 * rowAdj);
    }

    protected void setOriginalSize() {
        originalSize = new Dimension( (int) calcXCoordinates(maximum.getCol(), peakMargin), 
                (int) calcYCoordinates(maximum.getRow(), flatMargin));
    }

}
