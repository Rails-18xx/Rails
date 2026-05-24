package rails.game.correct;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.Tile;
import net.sf.rails.game.TileManager;
import net.sf.rails.game.round.SelectionRound;

import rails.game.correct.CorrectionManager;
import rails.game.correct.MapCorrectionManager;
import rails.game.correct.CorrectionType;

public class MapCorrectionManager extends CorrectionManager {

    private String pendingHexCorrection = null;
    private net.sf.rails.ui.swing.RemainingTilesWindow manifestWindow = null;

    private MapCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_MAP);
    }

    public static MapCorrectionManager create(GameManager parent) {
        return new MapCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<>();
        actions.add(new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_MAP, isActive()));
        return actions;
    }


@Override
    public boolean executeCorrection(CorrectionAction action) {
        if (action instanceof CorrectionModeAction) {
            boolean wasActive = isActive();
            
            // 1. Delegate to the base class FIRST to toggle the internal active state boolean
            boolean result = super.executeCorrection(action);
            
            // 2. Only spin up the SelectionRound wizard layout if transitioning from inactive to active
            if (!wasActive && isActive() && !getParent().isReloading()) {
                runWizard();
            }
            return result;
        }

        if (action instanceof MapCorrectionAction) {
            return execute((MapCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }


    private boolean execute(MapCorrectionAction action) {
        MapManager mm = getRoot().getMapManager();
        TileManager tm = getRoot().getTileManager();

        MapHex hex = mm.getHex(action.getHexName());
        if (hex == null) {
            DisplayBuffer.add(this, "Error: Hex not found: " + action.getHexName());
            return false;
        }

        Tile tile = tm.getTile(action.getTileNumber());
        if (tile == null) {
            DisplayBuffer.add(this, "Error: Tile ID not found: " + action.getTileNumber());
            return false;
        }

        hex.upgrade(tile, HexSide.get(action.getRotation()), null);

        String msg = String.format("Correction: Laid Tile %s on %s (Rot: %d)",
                action.getTileNumber(), action.getHexName(), action.getRotation());

        ReportBuffer.add(this, msg);

        if (getParent().getGameUIManager() != null && getParent().getGameUIManager().getORUIManager() != null) {
            var orUI = getParent().getGameUIManager().getORUIManager();
            var hexMap = orUI.getHexMap();
            if (hexMap != null) {
                // Reset all hexes to NORMAL state
                for (net.sf.rails.ui.swing.hexmap.GUIHex guiHex : hexMap.getGuiHexList()) {
                    guiHex.setState(net.sf.rails.ui.swing.hexmap.GUIHex.State.NORMAL);
                }
                // Force UI to redraw in normal state
                hexMap.repaintAll(new java.awt.Rectangle(hexMap.getSize()));
            }
        }

        return true;
    }

    // Inside MapCorrectionManager.java
    private void runWizard() {
        String uniqueId = "CorrectionSelect_" + System.currentTimeMillis();
        net.sf.rails.game.round.SelectionRound selectionRound = new net.sf.rails.game.round.SelectionRound(getParent(),
                uniqueId, "SELECT_HEX");

        getParent().setInterruptedRound(getParent().getCurrentRound());
        getParent().setRound(selectionRound);

        // Ensure this uses the correct method to access the map (verify in
        // ORUIManager.java)

        if (getParent().getGameUIManager() != null && getParent().getGameUIManager().getORUIManager() != null) {
            net.sf.rails.ui.swing.ORUIManager orUI = getParent().getGameUIManager().getORUIManager();
            net.sf.rails.ui.swing.hexmap.HexMap hexMap = orUI.getHexMap();
            if (hexMap != null) {
                for (net.sf.rails.ui.swing.hexmap.GUIHex guiHex : hexMap.getGuiHexList()) {
                    guiHex.setState(net.sf.rails.ui.swing.hexmap.GUIHex.State.SELECTABLE);
                }
                hexMap.repaintAll(new java.awt.Rectangle(hexMap.getSize()));
            }
        }

    }

    public void onHexSelected(String hexId) {
        this.pendingHexCorrection = hexId;

        // Deselect all other hexes, keeping only the clicked one selected
        if (getParent().getGameUIManager() != null && getParent().getGameUIManager().getORUIManager() != null) {
            net.sf.rails.ui.swing.ORUIManager orUI = getParent().getGameUIManager().getORUIManager();
            net.sf.rails.ui.swing.hexmap.HexMap hexMap = orUI.getHexMap();
            if (hexMap != null) {
                for (net.sf.rails.ui.swing.hexmap.GUIHex guiHex : hexMap.getGuiHexList()) {
                    if (guiHex.getHex().getId().equals(hexId)) {
                        guiHex.setState(net.sf.rails.ui.swing.hexmap.GUIHex.State.SELECTED);
                    } else {
                        guiHex.setState(net.sf.rails.ui.swing.hexmap.GUIHex.State.NORMAL);
                    }
                }
                hexMap.repaintAll(new java.awt.Rectangle(hexMap.getSize()));
            }
        }

        // Open the existing RemainingTilesWindow
        if (manifestWindow == null) {

            // Access the window via the ORUIManager, which is guaranteed to be available in
            // an OR
            net.sf.rails.ui.swing.ORUIManager orUI = getParent().getGameUIManager().getORUIManager();
            // The orWindow reference is standard in the ORUIManager
            manifestWindow = new net.sf.rails.ui.swing.RemainingTilesWindow(orUI.getORWindow());

        }
        manifestWindow.activate();

        // NOTE: To make this fully functional, we need to intercept the click in
        // RemainingTilesWindow to call back to this manager.
        // For now, this opens the visual manifest.
    }



public void completeCorrection(String tileId, int rotation) {
        if (manifestWindow != null) {
            manifestWindow.setVisible(false);
        }
        
        // 1. Roll back the game engine from SelectionRound out to the original phase round
        net.sf.rails.game.round.RoundFacade roundToResume = getParent().getInterruptedRound();
        if (roundToResume != null) {
            getParent().setInterruptedRound(null);
            getParent().setRound(roundToResume);
            if (getParent().getUIHints() != null) {
                getParent().getUIHints().setCurrentRoundType(roundToResume.getClass());
            }
        }
        
        // 2. Turn off Correction Mode BEFORE processing to ensure the state boolean is clean
        if (isActive()) {
            CorrectionModeAction deactivateAction = new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_MAP, false);
            super.executeCorrection(deactivateAction);
        }
        
        // 3. Create the correction action
        MapCorrectionAction mca = new MapCorrectionAction(getRoot(), this.pendingHexCorrection, tileId, rotation);
        
        // 4. Inject it into the possible actions list so GameManager validation allows it
        getParent().getPossibleActions().add(mca);
        
        // 5. Process the action formally through the engine (handles ChangeStack and executedActions natively)
        boolean success = getParent().process(mca);
        
        // 6. Resume the underlying round logic
        if (roundToResume != null) {
            roundToResume.resume();
        }
        
        // 7. Cleanup UI states
        var orUI = getParent().getGameUIManager().getORUIManager();
        if (orUI != null && orUI.getHexMap() != null) {
            for (net.sf.rails.ui.swing.hexmap.GUIHex guiHex : orUI.getHexMap().getGuiHexList()) {
                guiHex.setState(net.sf.rails.ui.swing.hexmap.GUIHex.State.NORMAL);
            }
            orUI.getHexMap().selectHex(null);
            orUI.getHexMap().repaintAll(new java.awt.Rectangle(orUI.getHexMap().getSize()));
        }
        
        // 8. Clear transient actions and rebuild standard operation panels
        getParent().getPossibleActions().clear();
        if (getParent().getCurrentRound() != null) {
            getParent().getCurrentRound().setPossibleActions();
        }
    }




}
