package rails.game;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/** Track configuration defines a set of tracks (e.g. on a Tile) */

public class TrackConfig{
    
    private final Set<Track> tracks;
    private final HashMultimap<Station, TrackPoint> stationTracks = HashMultimap.create();
    private final HashMultimap<HexSide, TrackPoint> sideTracks = HashMultimap.create();
    
    private final Tile tile;
    
    private TrackConfig(Tile tile, Set<Track> tracks, int rotation) {
        this.tile = tile;
        this.tracks = ImmutableSet.copyOf(tracks);
        for (Track t:tracks) {
            classifyTrack(t);
        }
    }
    
    public TrackConfig(Tile tile, Set<Track> tracks) {
        this(tile, tracks, 0);
    }
    
    @Deprecated
    public static TrackConfig createByRotation(TrackConfig baseConfig, int rotation) {
        return createByRotation(baseConfig, HexSide.get(rotation));
    }

    public static TrackConfig createByRotation(TrackConfig baseConfig, HexSide rotation) {
        Set<Track> tracks = Sets.newHashSet();
        for (Track t:baseConfig.getTracks()) {
            tracks.add(t.createByRotation(rotation));
        }
        return new TrackConfig(baseConfig.getTile(), tracks);
    }

    
    public static TrackConfig createByStationMapping(TrackConfig baseConfig, Map<Station, Station> mapping) {
        Set<Track> tracks = Sets.newHashSet();
        for (Track t:baseConfig.getTracks()) {
            Track n = t.createByStationMapping(mapping);
            if (n.getStart() == n.getEnd()) continue; // due to merged stations
            tracks.add(n);
        }
        return new TrackConfig(baseConfig.getTile(), tracks);
    }
    
    // special case in 1856: downgrade of village tiles possible
    public static TrackConfig createByDowngrade(TrackConfig baseConfig, Station station) {
        Set<Track> newTracks = Sets.newHashSet();
        Set<TrackPoint> stationConnects = baseConfig.getStationTracks(station);
        for (TrackPoint start:stationConnects) {
            for (TrackPoint end:stationConnects) {
                if (start == end) continue;
                // connect all sides that are reachable
                newTracks.add(new Track(start, end));
            }
        }
        return new TrackConfig(baseConfig.getTile(), newTracks);
    }

    private void classifyTrack(Track track) {
        TrackPoint start = track.getStart();
        TrackPoint end = track.getEnd();
        if (start.getTrackPointType() == TrackPoint.Type.STATION) {
            stationTracks.put((Station)start, end);
        } else {
            sideTracks.put((HexSide)start, end);
        }
        if (end.getTrackPointType() == TrackPoint.Type.STATION) {
            stationTracks.put((Station)end, start);
        } else {
            sideTracks.put((HexSide)end, start);
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(tracks);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TrackConfig)) return false;
        return (this.tracks.equals(((TrackConfig)other).tracks));
    }
    
    public Tile getTile() {
        return tile;
    }
    
    public Set<Track> getTracks(){
        return tracks;
    }
    
    public int size() {
        return tracks.size();
    }
    
    public boolean hasNoStationTracks() {
        return stationTracks.isEmpty();
    }
    
    public Set<TrackPoint> getStationTracks(Station station) {
        return stationTracks.get(station);
    }
    
    public Set<TrackPoint> getSideTracks(HexSide side) {
        return sideTracks.get(side);
    }
    
    public boolean hasSideTracks(HexSide side) {
        return sideTracks.containsKey(side);
    }
    
    @Override
    public String toString() {
        return "Track on tile " + tile.toString() + ": " + tracks.toString();
    }

    /**
     * For a combination of MapHex, Tile, rotation and station provide
     * a description of the connections
     * @deprecated Use {@link #getConnectionString(MapHex,Tile,HexSide,Station)} instead
     */
    public static String getConnectionString(MapHex hex, Tile tile, int rotation,
            Station station) {
                return getConnectionString(hex, tile, rotation, station);
            }

    /**
     * For a combination of MapHex, Tile, rotation and station provide
     * a description of the connections
     */
    public static String getConnectionString(MapHex hex, Tile tile, HexSide rotation,
            Station station) {
        StringBuffer b = new StringBuffer("");
        for (TrackPoint endPoint : tile.getTracksPerStation(station)) {
            if (endPoint.getTrackPointType() == TrackPoint.Type.STATION) continue;
            HexSide direction = (HexSide)endPoint.rotate(rotation);
            if (b.length() > 0) b.append(",");
            b.append(hex.getOrientationName(direction));
        }
        return b.toString();
    }
}
