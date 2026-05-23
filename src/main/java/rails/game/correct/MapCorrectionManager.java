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
            if (!isActive()) {
                if (!getParent().isReloading()) {
                    runWizard();
                }
                return true;
            }
            return super.executeCorrection(action);
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

    public void completeCorrection(String tileId) {
        if (manifestWindow != null)
            manifestWindow.setVisible(false);

        Integer[] rotations = { 0, 1, 2, 3, 4, 5 };
        Integer selectedRot = (Integer) JOptionPane.showInputDialog(null, "Select Rotation:",
                "Correction", JOptionPane.QUESTION_MESSAGE, null, rotations, rotations[0]);
        if (selectedRot == null)
            return;

        MapCorrectionAction mca = new MapCorrectionAction(getRoot(), this.pendingHexCorrection, tileId, selectedRot);
        getParent().process(mca);
    }

// --- START FIX ---
public void completeCorrection(String tileId, int rotation) {
    if (manifestWindow != null) manifestWindow.setVisible(false);
    
    // 1. Process the map change
    MapCorrectionAction mca = new MapCorrectionAction(getRoot(), this.pendingHexCorrection, tileId, rotation);
    boolean success = getParent().process(mca);
    
    // 2. Turn off the Correction Mode
    // We create a new action to toggle the state off. 
    // Passing 'false' for the active parameter tells the manager to deactivate.
    CorrectionModeAction deactivateAction = new CorrectionModeAction(getRoot(), CorrectionType.CORRECT_MAP, false);
    this.executeCorrection(deactivateAction);
    
    // 3. Cleanup UI
    var orUI = getParent().getGameUIManager().getORUIManager();
    if (orUI != null && orUI.getHexMap() != null) {
        orUI.getHexMap().selectHex(null);
        orUI.getHexMap().repaintAll(new java.awt.Rectangle(orUI.getHexMap().getSize()));
    }
}
// --- END FIX ---




}