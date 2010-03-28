package rails.game.correct;

import java.util.HashMap;
import java.util.List;

import rails.game.correct.MapCorrectionAction.*;

import rails.game.GameManager;
import rails.game.MapHex;
import rails.game.ReportBuffer;
import rails.game.TileI;
import rails.game.TileManager;
import rails.util.LocalText;

public class MapCorrectionManager extends CorrectionManager {
    
    public static enum ActionStep {
        SELECT_HEX,SELECT_TILE,SELECT_ORIENTATION,FINISHED,CANCELLED;
    }
    
    private MapCorrectionAction activeTileAction = null;
    
    protected MapCorrectionManager(GameManager gm) {
        super(gm, CorrectionType.CORRECT_MAP);
    }
    
    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = super.createCorrections();
        
        if (isActive()) {
            if (activeTileAction == null)
                activeTileAction = new MapCorrectionAction();
            actions.add(activeTileAction);
        }
        
        return actions;
    }
    
    @Override
    public boolean executeCorrection(CorrectionAction action){
        if (action instanceof MapCorrectionAction)
            return execute((MapCorrectionAction) action);
        else
             return super.executeCorrection(action);
    }

    private boolean execute(MapCorrectionAction action){
        
        MapHex hex = action.getLocation();
        TileI chosenTile = action.getChosenTile();

        TileManager tmgr = gameManager.getTileManager();
        TileI preprintedTile = tmgr.getTile(hex.getPreprintedTileId());

        // already finished, thus on reload
        ActionStep executeStep = action.getStep();
        if (executeStep != ActionStep.FINISHED)
            executeStep = action.moveToNextStep();
        
        switch (executeStep) {
        case SELECT_TILE: 
            // create list of possible up and downgrades
            List<TileI> possibleTiles = tmgr.getAllUpgrades(preprintedTile);
            if (preprintedTile == hex.getCurrentTile())
                possibleTiles.remove(hex.getCurrentTile()); // remove preprinted tile if still laid
            action.setTiles(possibleTiles);
            break;
        
        case SELECT_ORIENTATION:
            // default orientation for preprinted files
            if (preprintedTile == chosenTile)
                action.selectOrientation(hex.getPreprintedTileRotation());
            break;
        case FINISHED:
            // lays tiles
            gameManager.getMoveStack().start(false);
            
            int orientation = action.getOrientation();
            hex.upgrade(chosenTile, orientation, new HashMap<String,Integer>());
            
            String msg = LocalText.getText("CorrectMapLaysTileAt",
                    chosenTile.getExternalId(), hex.getName(), hex.getOrientationName(orientation));
            ReportBuffer.add(msg);
            gameManager.addToNextPlayerMessages(msg, true);

            activeTileAction = null;
            break;
        
        case CANCELLED:
            activeTileAction = null;
        }
        
        return true;
    }
}
