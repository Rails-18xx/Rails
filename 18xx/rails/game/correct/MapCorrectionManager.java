package rails.game.correct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.game.BaseToken;
import rails.game.City;
import rails.game.DisplayBuffer;
import rails.game.GameManager;
import rails.game.MapHex;
import rails.game.ReportBuffer;
import rails.game.Station;
import rails.game.TileI;
import rails.game.TileManager;
import rails.game.TokenI;
import rails.util.LocalText;

public class MapCorrectionManager extends CorrectionManager {
    
    public static enum ActionStep {
        SELECT_HEX,SELECT_TILE,SELECT_ORIENTATION,RELAY_BASETOKENS,FINISHED,CANCELLED;
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
        else // any other action, could be a correctionMode action
             return super.executeCorrection(action);
    }

    private boolean execute(MapCorrectionAction action){

        if (action.getStep() == ActionStep.FINISHED) {
            // already finished, thus on reload
            action.setNextStep(ActionStep.FINISHED);
        }
        
        MapHex hex = action.getLocation();
     
        TileI chosenTile = action.getChosenTile();
        TileManager tmgr = gameManager.getTileManager();
        TileI preprintedTile = tmgr.getTile(hex.getPreprintedTileId());

        // check conditions 
        String errMsg = null;
        while (true) {
            // check if chosenTile is still available (not for preprinted)
            if (chosenTile != null && chosenTile.getExternalId() > 0 && chosenTile != hex.getCurrentTile()
                    && chosenTile.countFreeTiles() == 0) {
                errMsg =
                    LocalText.getText("TileNotAvailable",
                            chosenTile.getExternalId());
                // return to step of hex selection
                action.selectHex(hex);
                break;
            }
            // check if chosenTile contains enough slots
            List<BaseToken> baseTokens = hex.getBaseTokens();
            if (chosenTile != null && baseTokens != null && !baseTokens.isEmpty()) {
                List<Station> stations = chosenTile.getStations();
                int nbSlots = 0;
                if (stations != null) {
                    for (Station station:stations) {
                        nbSlots += station.getBaseSlots();
                    }
                }
                if (baseTokens.size() > nbSlots) {
                    errMsg =
                        LocalText.getText("CorrectMapNotEnoughSlots", chosenTile.getExternalId());
                    // return to step of hex selection
                    action.selectHex(hex);
                    break;
                }
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CorrectMapCannotLayTile",
                    chosenTile.getExternalId(),
                    hex.getName(),
                    errMsg ));
            ;
            // stay at the same step
            action.setNextStep(action.getStep());
        }

        ActionStep nextStep;
        // not yet finished, move to next step
        if (action.getStep() != ActionStep.FINISHED)
            nextStep = action.getNextStep();
        else
            nextStep = ActionStep.FINISHED;

        // preparation for the next step
        switch (nextStep) {
        case SELECT_TILE: 
            // create list of possible up and downgrades
            List<TileI> possibleTiles = tmgr.getAllUpgrades(preprintedTile, hex);
            if (preprintedTile == hex.getCurrentTile())
                possibleTiles.remove(hex.getCurrentTile()); // remove preprinted tile if still laid
            action.setTiles(possibleTiles);
            break;
        case SELECT_ORIENTATION:
            // default orientation for preprinted files
            if (preprintedTile == chosenTile) {
                action.selectOrientation(hex.getPreprintedTileRotation());
                return execute(action);
            } else if (chosenTile.getFixedOrientation() != -1) {
                action.selectOrientation(chosenTile.getFixedOrientation());
                return execute(action);
            } else {
                break;
            }
        case RELAY_BASETOKENS:
            // check if relays are necessary:
                // A. downgrades or equalgrades to a tile with two stations or more
            if (chosenTile.getNumStations() >= 2 
                    && hex.getCurrentTile().getColourNumber() >= chosenTile.getColourNumber()
                // B. or the current tile requires relays
                    || hex.getCurrentTile().relayBaseTokensOnUpgrade()) {
                // define tokens for relays
                List<BaseToken> tokens = new ArrayList<BaseToken>();
                for (City oldCity:hex.getCities()) {
                    for (TokenI token:oldCity.getTokens()) {
                        if (token instanceof BaseToken) {
                            tokens.add((BaseToken)token);
                        }
                    }
                }
                action.setTokensToRelay(tokens);
                // define possible stations
                action.setPossibleStations(chosenTile.getStations());
                break;
            } else {
                action.selectRelayBaseTokens(null);
                // move to FINISHED
                return execute(action);
            }
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

        if (action.getStep() != ActionStep.FINISHED) {
            action.moveToNextStep();
        }
        
        return true;
    }
}
