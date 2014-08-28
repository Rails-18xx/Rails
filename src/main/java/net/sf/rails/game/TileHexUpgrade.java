package net.sf.rails.game;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.rails.common.LocalText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.LayTile;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;

/**
 * A HexTileUpgrade combines a TileUpgrade with a MapHex and valid Rotations
 */
public class TileHexUpgrade extends MapUpgrade implements Iterable<HexSide>, Comparable<TileHexUpgrade> {
    private static final Logger log =
            LoggerFactory.getLogger(TileHexUpgrade.class);
    
    
    public enum Validation {
        NO_VALID_ORIENTATION, HEX_BLOCKED, NO_TILES_LEFT,
        NOT_ALLOWED_FOR_HEX, NOT_ALLOWED_FOR_PHASE, COLOUR_NOT_ALLOWED, 
        NO_ROUTE_TO_NEW_TRACK;

        @Override
        public String toString() {
            return LocalText.getText("TILE_" + this.name());
        }
        
    }

    // static fields
    private final MapHex hex;
    private final TileUpgrade upgrade;
    private final LayTile action;

    // result fields
    private HexSidesSet rotations;
    private boolean permissiveRoutePossible;
    private EnumSet<Validation> invalids;
    
    
    private TileHexUpgrade(MapHex hex, TileUpgrade upgrade, LayTile action) {
        this.hex = hex;
        this.upgrade = upgrade;
        this.action = action;
        log.debug("New TileHexUpgrade, hex = " + hex + ", upgrade = " + upgrade + ", action = " + action);
    }
    
    public static Set<TileHexUpgrade> create(MapHex hex, HexSidesSet connected, Collection<Station> stations,
            LayTile action, String routeAlgorithm) {
        
        // use that to define available upgrades
        ImmutableSet.Builder<TileHexUpgrade> upgrades = ImmutableSet.builder();
        for (TileUpgrade upgrade:hex.getCurrentTile().getTileUpgrades()) {
            TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
            if (routeAlgorithm.equalsIgnoreCase("PERMISSIVE")) {
                hexUpgrade.findValidRotations(connected, stations, false);
            } else if (routeAlgorithm.equalsIgnoreCase("RESTRICTIVE")) {
                hexUpgrade.findValidRotations(connected, stations, true);
            }
            else if (routeAlgorithm.equalsIgnoreCase("SEMI-RESTRICTIVE")) {
                if (upgrade.getTargetTile().hasStations()) {
                    hexUpgrade.findValidRotations(connected, stations, false);
                } else {
                    hexUpgrade.findValidRotations(connected, stations, true);
                }
            }
            upgrades.add(hexUpgrade);
       }
        return upgrades.build();
    }
    
    public static Set<TileHexUpgrade> createLocated(MapHex hex, LayTile action) {
        ImmutableSet.Builder<TileHexUpgrade> upgrades = ImmutableSet.builder();

        if (action.getTiles() == null || action.getTiles().isEmpty()) {
            for (TileUpgrade upgrade:hex.getCurrentTile().getTileUpgrades()) {
                TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
                hexUpgrade.findValidRotations(null, null, true);
                upgrades.add(hexUpgrade);
            }
        } else {
            for (Tile targetTile:action.getTiles()) {
                TileUpgrade upgrade = hex.getCurrentTile().getSpecificUpgrade(targetTile);
                TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
                hexUpgrade.findValidRotations(null, null, true);
                upgrades.add(hexUpgrade);
            }
        }
        return upgrades.build();
    }

    public void findValidRotations(HexSidesSet connectedSides, Collection<Station> stations, boolean restrictive) {
        // encode HexSides according to the tile current orientation
        if (connectedSides != null) {
            connectedSides = HexSidesSet.rotated(connectedSides, hex.getCurrentTileRotation());
        }
        // check invalid sides
        HexSidesSet invalidSides = null;
        if (hex.getInvalidSides() != null) {
            invalidSides = HexSidesSet.rotated(hex.getInvalidSides(), hex.getCurrentTileRotation());
        }

        if (requiresConnection()) {
            HexSidesSet permissive = upgrade.getAllowedRotations(connectedSides, invalidSides,
                    hex.getCurrentTileRotation(), stations, false);
            if (restrictive) {
                rotations = upgrade.getAllowedRotations(connectedSides, invalidSides,
                    hex.getCurrentTileRotation(), stations, true);
                permissiveRoutePossible = !permissive.isEmpty();
            } else {
                rotations = permissive;
            }
        } else {
            rotations = upgrade.getAllowedRotations(null, invalidSides,
                    hex.getCurrentTileRotation(), stations, restrictive);
        }
    }
    
    public boolean validate(Phase phase) {
        invalids = EnumSet.noneOf(Validation.class);
//        if (hexIsBlocked()) {
//           invalids.add(Validation.HEX_BLOCKED);
//        }
        if (noTileAvailable()) {
            invalids.add(Validation.NO_TILES_LEFT);
        } 
        if (notAllowedForHex()) {
            invalids.add(Validation.NOT_ALLOWED_FOR_HEX);
        }
        if (notAllowedForPhase(phase)) {
            invalids.add(Validation.NOT_ALLOWED_FOR_PHASE);
        }
        if (tileColourNotAllowed(phase)) {
            invalids.add(Validation.COLOUR_NOT_ALLOWED);
        }
        if (noRouteToNewTrack()) {
            invalids.add(Validation.NO_ROUTE_TO_NEW_TRACK);
        } else if (noValidRotation()) {
            invalids.add(Validation.NO_VALID_ORIENTATION);
        }  
        return invalids.isEmpty();
    }
    
    public EnumSet<Validation> getInvalids() {
        return invalids;
    }
    
    @Override
    public boolean isValid() {
        if (invalids== null) {
            return true;
        }
        return invalids.isEmpty();
    }
    
    public boolean noValidRotation() {
        return rotations.isEmpty();
    }
    
//    public boolean hexIsBlocked() {
//        return hex.isBlockedForTileLays();
//    }

    public boolean noTileAvailable() {
        return upgrade.getTargetTile().getFreeCount() == 0;
    }
    
    public boolean notAllowedForHex() {
        return !upgrade.isAllowedForHex(hex);
    }
    
    public boolean notAllowedForPhase(Phase phase) {
        return !upgrade.isAllowedForPhase(phase);
    }
    
    public boolean tileColourNotAllowed(Phase phase) {
        return !phase.isTileColourAllowed(upgrade.getTargetTile().getColourText());
    }
    
    public boolean noRouteToNewTrack() {
        return noValidRotation() && permissiveRoutePossible;
    }
    
    public boolean requiresConnection() {
        // Yellow Tile on Company Home
        if (upgrade.getTargetTile().getColourText().equalsIgnoreCase(TileColour.YELLOW.name())
                && hex.isHomeFor(action.getCompany())) {
            return false;
        // Special Property with specified hexes and require connection 
         // TODO: Do we require the second test
        } else if (action.getType() == LayTile.SPECIAL_PROPERTY
                && action.getSpecialProperty().getLocations().contains(hex)) {
            return action.getSpecialProperty().requiresConnection();
        }
        return true;
            
    }
    
    public LayTile getAction() {
        return action;
    }
    
    /**
     * @return the upgrade
     */
    public TileUpgrade getUpgrade() {
        return upgrade;
    }

    /**
     * @return the rotations
     */
    public HexSidesSet getRotations() {
        return rotations;
    }
    
    public String toString() {
        return Objects.toStringHelper(this).add("Hex", hex.toString()).
                add("Upgrade", upgrade).add("rotations", rotations).toString();
    }

    public Iterator<HexSide> iterator() {
        return rotations.iterator();
    }

    public int compareTo(TileHexUpgrade other) {
        return ComparisonChain.start()
                .compare(other.isValid(), this.isValid())
                .compare(this.getUpgrade().getTargetTile(), other.getUpgrade().getTargetTile())
                .result();
    }
    
    // Location interface method
    public MapHex getLocation() {
        return hex;
    }
    
    /**
     * sets both validation and enable for upgrades
     */
    public static void validateAndEnable(Iterable<TileHexUpgrade> upgrades, Phase current) {
        for (TileHexUpgrade upgrade:upgrades) {
            if (upgrade.validate(current)) {
                upgrade.setVisible(true);
            } else if (upgrade.tileColourNotAllowed(current)) {
                upgrade.setVisible(false);
            } else {
                upgrade.setVisible(true);
            }
        }
    }
    
}
