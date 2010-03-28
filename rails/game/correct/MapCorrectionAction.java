package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.correct.MapCorrectionManager.*;
import rails.game.MapHex;
import rails.game.MapManager;
import rails.game.TileI;
import rails.game.TileManager;
import rails.game.action.PossibleAction;
import rails.util.Util;

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
    transient private List<TileI> tiles = null;
    private int[] tileIds;
    
    /** Orientation: how to lay the tile */
    private int orientation;

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
        locationCoordinates = location.getName(); 
    }
    
    public List<TileI> getTiles() {
        return tiles;
    }
    
    public TileI getChosenTile() {
        if (step.ordinal() > ActionStep.SELECT_TILE.ordinal())
            return tiles.get(0);
        else
            return null;
    }
    
    public void setTiles(List<TileI> tiles) {
        this.tiles = tiles;
        this.tileIds = new int[tiles.size()];
        for (int i = 0; i < tiles.size(); i++)
            tileIds[i] = tiles.get(i).getId();
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
    
    private void setNextStep(ActionStep step) {
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
    
    public void selectTile(TileI chosenTile) {
        List<TileI> tiles = new ArrayList<TileI>();
        tiles.add(chosenTile);
        setTiles(tiles);
        setNextStep(ActionStep.SELECT_ORIENTATION);
    }

    public void selectOrientation(int orientation) {
        setOrientation(orientation);
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
    public boolean equals(PossibleAction action) {
        if (!(action instanceof MapCorrectionAction)) return false;
        MapCorrectionAction a = (MapCorrectionAction) action;
        return (a.step == this.step);
    }

    @Override
    public String toString(){
        StringBuffer b = new StringBuffer("MapCorrectionAction");
        if (acted) {
            b.append(" (acted)");
        } else {
            b.append(" (not acted)");
        }
        b.append(" Step=" + step);
        if (nextStep != null) 
            b.append(" NextStep=" + nextStep);
        if (step.ordinal() > ActionStep.SELECT_HEX.ordinal()) 
            b.append(" Hex=" + location.getName());
        if (step == ActionStep.SELECT_TILE)
            b.append(" Possible tiles=" + tiles);
        if (step.ordinal() > ActionStep.SELECT_TILE.ordinal())
            b.append(" Chosen tile=" + tiles);
        if (step.ordinal() > ActionStep.SELECT_ORIENTATION.ordinal())
            b.append(" Orientation=" + orientation);
        return b.toString();
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
            
        MapManager mmgr = gameManager.getMapManager();
        if (Util.hasValue(locationCoordinates))
            location = mmgr.getHex(locationCoordinates);

        TileManager tmgr = gameManager.getTileManager();
        if (tileIds != null && tileIds.length > 0) {
            tiles = new ArrayList<TileI>();
            for (int i = 0; i < tileIds.length; i++) {
                tiles.add(tmgr.getTile(tileIds[i]));
            }
        }
    }
}
