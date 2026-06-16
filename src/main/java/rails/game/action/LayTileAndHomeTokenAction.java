package rails.game.action;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Round;
import net.sf.rails.game.RailsRoot;
import java.util.Map;

/**
 * An atomic action that holds all data for laying a tile AND its corresponding 
 * home token in a single step. This is used for the "Baden" opening.
 *
 * This class is just a data container. The logic to process it will be
 * handled inside OperatingRound.process().
 */
public class LayTileAndHomeTokenAction extends LayTile {

    protected int chosenStation;

    /**
     * Constructor for the atomic Lay+Token action.
     */
    public LayTileAndHomeTokenAction(RailsRoot root, MapHex hex, Tile tile, int orientation, int station) {
        // We must call a valid super() constructor.
        // We use the one that takes a root and a (null) map of tile colours.
        super(root, (Map<String, Integer>) null);
        
        this.setChosenHex(hex);
        this.setLaidTile(tile);
        this.setOrientation(orientation);
        
        // This is our new, extra data
        this.chosenStation = station;
        
        // Use the public setter for the type
        this.setType(LayTile.LOCATION_SPECIFIC); 
    }

    /**
     * @return The station number (1-indexed) where the token will be placed.
     */
    public int getChosenStation() {
        return chosenStation;
    }
    
    @Override
    public String toString() {
        // The 'tile' field is private in the superclass.
        // We must use the public getter 'getLaidTile()'.
        String tileId = (getLaidTile() != null) ? getLaidTile().getId() : "null";
        
        return "LayTileAndHomeTokenAction: Tile " + tileId +
               " on " + getChosenHex().getId() +
               " (Rot: ".concat(String.valueOf(getOrientation())) +
               "), Token on Station " + chosenStation;
    }
}
