package net.sf.rails.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;


/**
 * Abstract class to be extended by classes that are Points on a hex tile
 */
public abstract class TrackPoint implements Comparable<TrackPoint> {

    public static enum Type {SIDE, STATION};
    
    public abstract int getTrackPointNumber();
    
    public abstract Type getTrackPointType();
    
    public abstract TrackPoint rotate(HexSide rotation);
    
    // Object methods
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TrackPoint)) return false;
        return this.getTrackPointNumber() == ((TrackPoint)other).getTrackPointNumber();
    }
    
    @Override
    public int hashCode() {
        return getTrackPointNumber();
    }
    
    // Comparable methods
    public int compareTo(TrackPoint other) {
        return ((Integer)this.getTrackPointNumber()).compareTo(other.getTrackPointNumber());
    }
    
    // Patterns for Track tags
    private static final Pattern sidePattern = Pattern.compile("side(\\d+)");
    private static final Pattern cityPattern = Pattern.compile("city(\\d+)");
    
    static TrackPoint create(Tile tile, String trackString) throws ConfigurationException {
        int trackPointNr = parseTrackPointNumber(trackString);
        if (trackPointNr >= 0) {
            return HexSide.get(trackPointNr);
        } else { // trackPointNr is negative
            return tile.getStation(-trackPointNr);
        }
    }
    
    static int parseTrackPointNumber(String trackString) throws ConfigurationException {
        Matcher m;
        if ((m = sidePattern.matcher(trackString)).matches()) {
            int sideNr =  (Integer.parseInt(m.group(1)) + 3) % 6;
            return sideNr;
        } else if ((m = cityPattern.matcher(trackString)).matches()) {
            int stationNr = Integer.parseInt(m.group(1));
            return -stationNr;
        }
        // Should add some validation!
        throw new ConfigurationException(LocalText.getText("InvalidTrackEnd")
                + ": " + trackString);
        
    }
    
}
