package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

import com.google.common.base.Objects;

import rails.game.action.PossibleAction;
import rails.game.correct.MapCorrectionManager.*;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.Station;
import net.sf.rails.game.Tile;
import net.sf.rails.game.TileManager;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * Correction action for tile and token lays
 * 
 * Rails 2.0: updated equals and toString methods
 */

// TODO: Add implementation of token lays
// TODO: Make it compatible to the standard tile and token lays by reworking step-mechanism
// TODO: Add support for relay of tokens
public class MapCorrectionAction extends CorrectionAction {

    /** The Constant serialVersionUID. */
    public static final long serialVersionUID = 1L;

    /** Sequence: Indicates the enrichment of the action */
    transient private ActionStep step = null;
    private String stepName;

    transient private ActionStep nextStep = null;
    private String nextStepName;
    
    /* Conditions */
    
    /** Location: where to lay the tile */
    transient private MapHex location = null;
    private String locationCoordinates;
    
    /** Tiles: which tile(s) to lay */
    transient private List<Tile> tiles = null;
    private String[] sTileIds;
    // FIXME: Rewrite this with Rails1.x version flag
    private int[] tileIds;
    
    /** Orientation: how to lay the tile */
    private int orientation;
    
    /** RelayBaseTokens: how to relay the base tokens */
    transient private List<BaseToken> tokensToRelay;
    //private String[]tokensToRelayOwner;
    transient private List<Station> stationsForRelay;
    //private int[] stationForRelayId;
    transient private Collection<Station> possibleStations;
    //private int[] possibleStationsId;
                
    /**
     * Instantiates a new map tile correction action.
     * start with select hex
     */
    public MapCorrectionAction() {
        setStep(ActionStep.SELECT_HEX);
        setNextStep(null);
        setCorrectionType(CorrectionType.CORRECT_MAP);
    }
    
    public MapHex getLocation() {
        return location;
    }
    
    private void setLocation(MapHex location) {
        this.location = location;
        locationCoordinates = location.getId(); 
    }
    
    public List<Tile> getTiles() {
        return tiles;
    }
    
    public Tile getChosenTile() {
        if (nextStep.ordinal() > ActionStep.SELECT_TILE.ordinal())
            return tiles.get(0);
        else
            return null;
    }
    
    void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
        this.sTileIds = new String[tiles.size()];
        for (int i = 0; i < tiles.size(); i++)
            sTileIds[i] = tiles.get(i).getId();
    }
    
    public List<Station> getStationsForRelay() {
        return stationsForRelay;
    }
    
    private void setStationsForRelay(List<Station> stations) {
        this.stationsForRelay = stations;
    }

    public List<BaseToken> getTokensToRelay() {
        return tokensToRelay;
    }
    
    void setTokensToRelay(List<BaseToken> tokens) {
        this.tokensToRelay = tokens;
    }
    
    public Collection<Station> getPossibleStations() {
        return possibleStations;
    }
    
    void setPossibleStations(Collection<Station> possibleStations) {
        this.possibleStations = possibleStations;
    }
    
    public int getOrientation(){
        return orientation;
    }
    
    private void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public ActionStep getNextStep() {
        return nextStep;
    }
    
    void setNextStep(ActionStep step) {
        this.nextStep = step;
        if (step == null)
            nextStepName = null;
        else
            nextStepName = step.name();
    }
    
    public ActionStep getStep() {
        return step;
    }
    
    private void setStep(ActionStep step) {
        this.step = step;
        stepName = step.name();
    }
    
    public void selectHex(MapHex chosenHex) {
        setLocation(chosenHex);
        setNextStep(ActionStep.SELECT_TILE);
    }
    
    public void selectTile(Tile chosenTile) {
        List<Tile> tiles = new ArrayList<Tile>();
        tiles.add(chosenTile);
        setTiles(tiles);
        setNextStep(ActionStep.SELECT_ORIENTATION);
    }

    public void selectConfirmed() {
        setNextStep(ActionStep.RELAY_BASETOKENS);
    }
    
    public void selectOrientation(int orientation) {
        setOrientation(orientation);
        setNextStep(ActionStep.RELAY_BASETOKENS);
    }
    
    public void selectRelayBaseTokens(List<Station> chosenStations) {
        setStationsForRelay(chosenStations);
        setNextStep(ActionStep.FINISHED);
    }
    
    public void selectCancel() {
        setNextStep(ActionStep.CANCELLED);
    }
    
    public ActionStep moveToNextStep() {
        setStep(nextStep);
        setNextStep(null);
        if (step != ActionStep.FINISHED)
            this.acted = false;
        return step;
    }
    
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // finish if asOptions check, no asOption attributes
        if (asOption) return true;

        // check asAction attributes
        MapCorrectionAction action = (MapCorrectionAction)pa; 
        return Objects.equal(this.location, action.location)
                && Objects.equal(this.tiles, action.tiles)
                && Objects.equal(this.orientation, action.orientation)
        ;
    }

    @Override
    public String toString(){
        return super.toString() + 
                RailsObjects.stringHelper(this)
                .addToStringOnlyActed("location", location)
                .addToStringOnlyActed("tiles", tiles)
                .addToStringOnlyActed("orientation", orientation)
        ;
    }
    
    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        
        if (Util.hasValue(correctionName))
            correctionType = CorrectionType.valueOf(correctionName);

        if (Util.hasValue(stepName))
            step = ActionStep.valueOf(stepName);

        if (Util.hasValue(nextStepName))
            nextStep = ActionStep.valueOf(nextStepName);
            
        MapManager mmgr = getRoot().getMapManager();
        if (Util.hasValue(locationCoordinates))
            location = mmgr.getHex(locationCoordinates);

        TileManager tmgr = getRoot().getTileManager();
        if (sTileIds != null && sTileIds.length > 0) {
            tiles = new ArrayList<Tile>();
            for (int i = 0; i < sTileIds.length; i++) {
                tiles.add(tmgr.getTile(sTileIds[i]));
            }
        }
        // FIXME: Rewrite this with Rails1.x version flag
        if (tileIds != null && tileIds.length > 0) {
            tiles = new ArrayList<Tile>();
            for (int i = 0; i < tileIds.length; i++) {
                tiles.add(tmgr.getTile(String.valueOf(tileIds[i])));
            }
        }
    }
}
