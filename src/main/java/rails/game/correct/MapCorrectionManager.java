package rails.game.correct;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.Tile;
import net.sf.rails.game.TileManager;

public class MapCorrectionManager extends CorrectionManager {

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
        
        // 1. Menu Click -> Wizard
        if (action instanceof CorrectionModeAction) {
            if (!isActive()) {
                if (!getParent().isReloading()) {
                    runWizard();
                }
                return true; 
            }
            return super.executeCorrection(action);
        }

        // 2. Execution Logic
        if (action instanceof MapCorrectionAction) {
            return execute((MapCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
       javax.swing.SwingUtilities.invokeLater(() -> {
            MapManager mm = getRoot().getMapManager();
            
            // Step 1: Select Hex
            List<String> hexNames = mm.getHexes().stream()
                    .map(MapHex::getId)
                    .sorted()
                    .collect(Collectors.toList());

            if (hexNames.isEmpty()) {
                DisplayBuffer.add(this, "No hexes found on map.");
                return;
            }

            String selectedHex = (String) JOptionPane.showInputDialog(
                null, 
                "Select Hex to Correct:",
                "Map Correction (1/3)",
                JOptionPane.QUESTION_MESSAGE,
                null, 
                hexNames.toArray(), 
                hexNames.get(0)
            );
            if (selectedHex == null) return;

            // Step 2: Select Tile (Text Input for ID)
            String tileId = JOptionPane.showInputDialog(
                null, 
                "Enter Tile Number (e.g. '6' or '57'):",
                "Map Correction (2/3)",
                JOptionPane.QUESTION_MESSAGE
            );
            if (tileId == null || tileId.trim().isEmpty()) return;
            tileId = tileId.trim();

            // Step 3: Select Rotation
            Integer[] rotations = {0, 1, 2, 3, 4, 5};
            Integer selectedRot = (Integer) JOptionPane.showInputDialog(
                null, 
                "Select Rotation:",
                "Map Correction (3/3)",
                JOptionPane.QUESTION_MESSAGE,
                null, 
                rotations, 
                rotations[0]
            );
            if (selectedRot == null) return;

            // Step 4: Create Action
            MapCorrectionAction mca = new MapCorrectionAction(getRoot(), selectedHex, tileId, selectedRot);
            getParent().process(mca);
        });
    }

    private boolean execute(MapCorrectionAction action) {
        MapManager mm = getRoot().getMapManager();
        TileManager tm = getRoot().getTileManager();

        // [FIX] Use getHex instead of getMapHex
        MapHex hex = mm.getHex(action.getHexName());
        if (hex == null) {
            DisplayBuffer.add(this, "Error: Hex not found: " + action.getHexName());
            return false;
        }

        // Try to get a tile instance
        Tile tile = tm.getTile(action.getTileNumber());
        
        if (tile == null) {
            DisplayBuffer.add(this, "Error: Tile ID not found: " + action.getTileNumber());
            return false;
        }

hex.upgrade(tile, HexSide.get(action.getRotation()), null);

        String msg = String.format("Correction: Laid Tile %s on %s (Rot: %d)", 
                action.getTileNumber(), action.getHexName(), action.getRotation());
        
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);
        
        // Safe UI Refresh
        if (getParent().getGameUIManager() != null && !getParent().isReloading()) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }
        
        return true;
    }
}