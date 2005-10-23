/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Track.java,v 1.1 2005/10/23 18:02:00 evos Exp $
 * 
 * Created on 23-Oct-2005
 * Change Log:
 */
package game;

/**
 * Represents a piece of track on one tile.
 * <p>Endpoints can be:
 * <br>- a hex side (number >= 0), or
 * <br>- a station (number < 0)
 * @author Erik Vos
 */
public class Track {
    
    int startPoint;
    int endPoint;
    
    public Track (int startPoint, int endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }
    
    public boolean hasPoint (int point) {
        return startPoint == point || endPoint == point;
    }
    
    public int[] points () {
        return new int[] {startPoint, endPoint};
    }

}
