package rails.game;

/**
 * Represents a piece of track on one tile. <p> Endpoints can be: <br>- a hex
 * side (number >= 0), or <br>- a station (number < 0)
 */
public final class Track {

    private int startPoint;
    private int endPoint;

    public Track(int startPoint, int endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public boolean hasPoint(int point) {
        return startPoint == point || endPoint == point;
    }

    public int[] points() {
        return new int[] { startPoint, endPoint };
    }

    // TODO: Check if returning -99 is a valid error handling!
    public int getEndPoint(int startPoint) {

        if (startPoint == this.startPoint) {
            return this.endPoint;
        } else if (startPoint == this.endPoint) {
            return this.startPoint;
        } else {
            return -99;
        }
    }

    public String toString() {
        return ("Track " + startPoint + "/" + endPoint);
    }

}
