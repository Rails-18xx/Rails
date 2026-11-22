package net.sf.rails.game.ai;

// [FIX] Moved this class to net.sf.rails.game.ai

import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;
import rails.game.action.LayBaseToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Data class for a valid token lay option.
 * We've added a parse() method for the playground.
 */
public class TokenLayOption {

    private MapHex hex;
    private Stop stop;
    private LayBaseToken originatingAction;

    // --- Added for Playground Parsing ---
    private String hexId;
    private int stopIndex;
    private int cost;
    
    // Constructor for real game (assumed)
    public TokenLayOption(MapHex hex, Stop stop, LayBaseToken originatingAction) {
        this.hex = hex;
        this.stop = stop;
        this.originatingAction = originatingAction;
        
        this.hexId = (hex != null) ? hex.getId() : "N/A";
        this.stopIndex = (stop != null) ? stop.getRelatedStationNumber() : -1;
        this.cost = (originatingAction != null) ? originatingAction.getCost() : 0;
    }

    // Constructor for playground
    private TokenLayOption(String hexId, int stopIndex, int cost) {
        this.hexId = hexId;
        this.stopIndex = stopIndex;
        this.cost = cost;

        this.hex = null;
        this.stop = null;
        this.originatingAction = null;
    }

    public static TokenLayOption parse(String s) {
        Map<String, String> parts = parseKeyValueString(s);
        return new TokenLayOption(
            parts.get("HEX"),
            Integer.parseInt(parts.get("STOP")),
            Integer.parseInt(parts.get("COST"))
        );
    }
    
    @Override
    public String toString() {
        return String.format("[TokenLay: Hex=%s, Stop=%d, Cost=%d]", hexId, stopIndex, cost);
    }
    
    // --- Getters ---
    public MapHex hex() { return hex; }
    public Stop stop() { return stop; }
    public LayBaseToken originatingAction() { return originatingAction; }
    
    // --- Getters for Playground ---
    public String getHexId() { return hexId; }
    public int getStopIndex() { return stopIndex; }
    public int getCost() { return cost; }

    // Helper
    private static Map<String, String> parseKeyValueString(String s) {
        Map<String, String> map = new HashMap<>();
        for (String part : s.split(",")) {
            String[] kv = part.trim().split("=");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}
