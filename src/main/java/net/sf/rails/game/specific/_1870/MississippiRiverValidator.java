package net.sf.rails.game.specific._1870;

import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Station;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Track;
import net.sf.rails.game.TrackPoint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MississippiRiverValidator {

    private static final Logger log = LoggerFactory.getLogger(MississippiRiverValidator.class);

    private static final Map<String, RiverTopology> RIVER_HEXES = new HashMap<>();

    enum StationBank {
        WEST, EAST, NONE
    }

// --- START FIX ---
    static {
        // Rails EW Hex Side Mapping (Pointy-Topped):
        // 0 = SW, 1 = W, 2 = NW, 3 = NE, 4 = E, 5 = SE

        // C18 (St. Louis) - West Bank. Special validation applies, but topology needed to register as river.
        addTopology(Arrays.asList("C18"), Set.of(0, 1), Set.of(2, 3, 4, 5), StationBank.WEST);

        // F19
        addTopology(Arrays.asList("F19"), Set.of(0, 1, 2), Set.of(3, 4, 5), StationBank.EAST);

        // A16, B17, E18, M14, O18
        addTopology(Arrays.asList("A16", "B17", "E18", "M14", "O18"), Set.of(0, 1), Set.of(3, 4), StationBank.NONE);

        // D17, L13
        addTopology(Arrays.asList("D17", "L13"), Set.of(1, 2, 3), Set.of(5), StationBank.NONE);

        // G18, H17, I16, J15, K14
        addTopology(Arrays.asList("G18", "H17", "I16", "J15", "K14"), Set.of(2, 3), Set.of(5, 0), StationBank.NONE);

        // N15
        addTopology(Arrays.asList("N15"), Set.of(0, 1, 2), Set.of(4), StationBank.NONE);

        // N17
        addTopology(Arrays.asList("O16"), Set.of(1), Set.of(3, 4, 5), StationBank.NONE);
    }

    private static void addTopology(List<String> hexes, Set<Integer> west, Set<Integer> east, StationBank stationBank) {
        RiverTopology topo = new RiverTopology(west, east, stationBank);
        for (String hex : hexes) {
            RIVER_HEXES.put(hex, topo);
        }
    }

    public static boolean isCrossingRiver(Tile tile, MapHex hex, int orientation) {
        RiverTopology topo = RIVER_HEXES.get(hex.getId());
        if (topo == null) {
            return false; // Not a river hex
        }

        String hexId = hex.getId();
       
        // 1. Handle Special City Exceptions (Memphis K16 and St. Louis C18)
        // These override normal edge-to-edge checks because the city itself is the barrier limit
        if ("K16".equals(hexId) || "C18".equals(hexId)) {
            Set<Integer> allowedSides = "K16".equals(hexId) 
                ? new HashSet<>(Arrays.asList(3, 4, 5, 0)) // Memphis: NE, E, SE, SW
                : new HashSet<>(Arrays.asList(1, 0));      // St. Louis: W, SW

            for (Track track : tile.getTracks()) {
                for (TrackPoint tp : Arrays.asList(track.getStart(), track.getEnd())) {
                    if (tp instanceof HexSide) {
                        int absSide = (tp.getTrackPointNumber() + orientation) % 6;
                        if (!allowedSides.contains(absSide)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        // 2. Standard Topology Check: Edge-to-Edge Crossings
        // A crossing only occurs if track directly connects the explicitly defined West Bank to the East Bank.
        for (Integer absWest : topo.westBank) {
            for (Integer absEast : topo.eastBank) {
                int tileWest = (absWest - orientation + 6) % 6;
                int tileEast = (absEast - orientation + 6) % 6;

                if (tileConnectsSides(tile, tileWest, tileEast)) {
                    return true;
                }
            }
        }

        // 3. Station-to-Edge Crossings
        if (topo.stationBank != StationBank.NONE) {
            for (Station station : tile.getStations()) {
                for (Track track : tile.getTracks()) {
                    TrackPoint start = track.getStart();
                    TrackPoint end = track.getEnd();

                    if (start.equals(station) || end.equals(station)) {
                        TrackPoint edge = start.equals(station) ? end : start;

                        if (edge instanceof HexSide) {
                            int relativeSide = edge.getTrackPointNumber();
                            int absSide = (relativeSide + orientation) % 6;

                            if (topo.stationBank == StationBank.EAST && topo.westBank.contains(absSide)) {
                                return true;
                            }
                            if (topo.stationBank == StationBank.WEST && topo.eastBank.contains(absSide)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
// --- END FIX ---

    private static boolean tileConnectsSides(Tile tile, int ts1, int ts2) {
        String side1 = "side " + ts1;
        String side2 = "side " + ts2;

        // 1. Direct tracks
        for (Track track : tile.getTracks()) {
            String start = track.getStart().toString();
            String end = track.getEnd().toString();

            if ((start.equals(side1) && end.equals(side2)) ||
                    (start.equals(side2) && end.equals(side1))) {
                return true;
            }
        }

        // 2. Connections through stations
        for (Station station : tile.getStations()) {
            boolean hasS1 = false;
            boolean hasS2 = false;
            String stationId = station.toString();

            for (Track track : tile.getTracks()) {
                String start = track.getStart().toString();
                String end = track.getEnd().toString();

                if (start.equals(stationId) || end.equals(stationId)) {
                    if (start.equals(side1) || end.equals(side1))
                        hasS1 = true;
                    if (start.equals(side2) || end.equals(side2))
                        hasS2 = true;
                }
            }
            if (hasS1 && hasS2)
                return true;
        }

        return false;
    }

    private static class RiverTopology {
        final Set<Integer> westBank;
        final Set<Integer> eastBank;
        StationBank stationBank;

        RiverTopology(Set<Integer> west, Set<Integer> east, StationBank stationBank) {
            this.westBank = west;
            this.eastBank = east;
            this.stationBank = stationBank;
        }
    }

    /**
     * Helper to identify if a hex is part of the Mississippi River system.
     */
    public static boolean isRiverHex(MapHex hex) {
        return RIVER_HEXES.containsKey(hex.getId());
    }
    
}