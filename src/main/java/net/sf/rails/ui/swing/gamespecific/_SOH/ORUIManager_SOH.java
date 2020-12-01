package net.sf.rails.ui.swing.gamespecific._SOH;

import net.sf.rails.game.*;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.specific._SOH.GameDef_SOH;
import net.sf.rails.ui.swing.ORUIManager;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.LayTile;

import java.util.Set;

public class ORUIManager_SOH extends ORUIManager {

    private static final Logger log = LoggerFactory.getLogger(ORUIManager_SOH.class);

    /**
     * Additional validation of potential tile upgrades.
     * Used in SOH to prevent showing an upgrade that
     * incorrectly uses the free Bridge special property .
     * @param upgrades All allowed tile upgrades
     * @param layTile  The possible action being investigated
     */
    protected void gameSpecificTileUpgradeValidation (Set<TileHexUpgrade> upgrades,
                                                      LayTile layTile) {
        SpecialProperty sp = layTile.getSpecialProperty();

        if (sp == null
                // Only the free bridge building private 2 KKI must be considered
                || !sp.getOriginalCompany().getId().equalsIgnoreCase(GameDef_SOH.KKI)) return;

        MapHex hex;
        MapManager mmgr = gameUIManager.getRoot().getMapManager();
        HexSidesSet bridgeSides;
        for (TileHexUpgrade upgrade : upgrades) {
            hex = upgrade.getHex().getHex();
            for (HexSide rotation : upgrade.getRotations()) {
                bridgeSides = mmgr.findNewBridgeSides(hex,
                                        upgrade.getUpgrade().getTargetTile(),
                                        rotation.getTrackPointNumber());
                log.info ("Hex={} upgrade={} rotation={}", hex, upgrade, rotation);
                mmgr.logHexSides (bridgeSides, "+++++ Bridges");
                if (bridgeSides == null) {
                    // No bridges, then no free-bridge upgrade
                    upgrade.setVisible(false);
                }
            }

        }
    }

}
