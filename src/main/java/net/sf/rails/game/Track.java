package net.sf.rails.game;

import java.util.Map;

/**
 * Represents a piece of track on one tile. Start and end of a track are TrackPoints:
 * Either TileSide or Station objects. 
 */
public class Track {

    private final TrackPoint start;
    private final TrackPoint end;
    private final int hashCode;
    
    public Track(TrackPoint start, TrackPoint end) {
        if (start.getTrackPointNumber() < end.getTrackPointNumber()) {
            this.start = start;
            this.end = end;
        } else {
            this.start = end;
            this.end = start;
        }
        hashCode = 12 * this.start.getTrackPointNumber() + this.end.getTrackPointNumber();
    }
    
    public Track createByRotation(HexSide rotation) {
        return new Track(start.rotate(rotation), end.rotate(rotation));
    }

    public Track createByStationMapping(Map<Station, Station> mapping){
        TrackPoint s = start;
        if (mapping.containsKey(start)) {
            s = mapping.get(start);
        }
        TrackPoint e = end;
        if (mapping.containsKey(end)) {
            e = mapping.get(end);
        }
        return new Track(s, e);
    }
   
    public TrackPoint getStart() {
        return start;
    }

    public TrackPoint getEnd() {
        return end;
    }

    public TrackPoint getOpposite(TrackPoint other) {

        if (other == this.start) {
            return this.end;
        } else if (other == this.end) {
            return this.start;
        } else {
            throw new IllegalArgumentException();
        }
    }

    // Object methods
    @Override
    public String toString() {
        return ("Track " + start + "->" + end + " (hc = " + hashCode + ")");
    }
    
    // Two tracks are equal if they share the same hashcode
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Track)) return false;
        return this.hashCode() == other.hashCode();
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

}
