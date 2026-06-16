package net.sf.rails.game.ai;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.Tile;
import rails.game.action.LayTile;

import java.util.HashMap;
import java.util.Map;

/**
 * Data class for a valid tile lay option.
 * This is based on the blueprint and the constructor call in ORUIManager.
 * We've added a parse() method for the playground.
 */
public class TileLayOption {
    
    private final MapHex hex;
    private final Tile tile;
    private final int orientation;
    private final LayTile originatingAction;
    private String tileId;

    // --- Added for Playground Parsing ---
    private final String hexId;
    private final int cost;
    
    // Constructor for real game (called by ORUIManager)
    public TileLayOption(MapHex hex, Tile tile, int orientation, LayTile originatingAction) {
        this.hex = hex;
        this.tile = tile;
        this.orientation = orientation;
        this.originatingAction = originatingAction;
        
        // Populate string/primitive fields for playground compatibility
        this.hexId = (hex != null) ? hex.getId() : "N/A";
        this.tileId = (tile != null) ? tile.getId() : "N/A";
        this.cost = (hex != null) ? hex.getTileCost() : 0;
    }

    

    // Constructor for playground (called by StaticGameContext)
    private TileLayOption(String hexId, String tileId, int orientation, int cost) {
        this.hexId = hexId;
        this.tileId = tileId;
        this.orientation = orientation;
        this.cost = cost;
        
        // Real objects will be null in the playground
        this.hex = null; 
        this.tile = null;
        this.originatingAction = null;
        this.tileId = tileId;
    }

    /**
     * Parses a string from the state file into a TileLayOption for the playground.
     */

     public static TileLayOption parse(String s) {
    // Example: HEX=I7, TILE=8, ROT=0, COST=0
    Map<String, String> parts = parseKeyValueString(s);
    String tileId = parts.get("TILE");
    int cost = Integer.parseInt(parts.get("COST"));

    // Use the playground constructor
    return new TileLayOption(
        parts.get("HEX"),
        tileId, // Pass the tileId string
        Integer.parseInt(parts.get("ROT")),
        cost
    );
}

    
    @Override
    public String toString() {
        // Use the string/primitive fields, as the real objects might be null
        return String.format("[TileLay: Hex=%s, Tile=%s, Rot=%d, Cost=%d]", hexId, tileId, orientation, cost);
    }
    
    // --- Getters ---
    public MapHex hex() { return hex; }
    public Tile tile() { return tile; }
    public int orientation() { return orientation; }
    public LayTile originatingAction() { return originatingAction; }
    
    // --- Getters for Playground (and safe scoring) ---
    public String getHexId() { return hexId; }
    public String getTileId() { return tileId; }
    public int getCost() { return cost; }


    // Helper to parse "KEY=VALUE, KEY=VALUE"
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

