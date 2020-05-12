package net.sf.rails.ui.swing.hexmap;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.special.SpecialTileLay;
import rails.game.action.LayTile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * A HexTileUpgrade combines a TileUpgrade with a MapHex and valid Rotations
 */
public class TileHexUpgrade extends HexUpgrade implements Iterable<HexSide> {

    public enum Invalids implements HexUpgrade.Invalids {
        NO_VALID_ORIENTATION, HEX_BLOCKED, HEX_RESERVED, NO_TILES_LEFT,
        NOT_ALLOWED_FOR_HEX, NOT_ALLOWED_FOR_PHASE, COLOUR_NOT_ALLOWED,
        COLOUR_RIGHT_MISSING, NO_ROUTE_TO_NEW_TRACK, NOT_ENOUGH_CASH;

        @Override
        public String toString() {
            return LocalText.getText("TILE_UPGRADE_INVALID_" + this.name());
        }

    }

    // static fields
    private final TileUpgrade upgrade;
    private final LayTile action;

    // validation fields
    private HexSidesSet rotations;
    private boolean permissiveRoutePossible;
    private final EnumSet<Invalids> invalids = EnumSet.noneOf(Invalids.class);

    // ui fields
    private HexSide selectedRotation;

    private TileHexUpgrade(GUIHex hex, TileUpgrade upgrade, LayTile action) {
        super(hex);
        this.upgrade = upgrade;
        this.action = action;
    }

    public static Set<TileHexUpgrade> create(GUIHex hex, HexSidesSet connected, Collection<Station> stations,
                                             LayTile action, String routeAlgorithm) {

        // use that to define available upgrades
        ImmutableSet.Builder<TileHexUpgrade> upgrades = ImmutableSet.builder();
        for (TileUpgrade upgrade : hex.getHex().getCurrentTile().getTileUpgrades()) {
            TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
            if (routeAlgorithm.equalsIgnoreCase("PERMISSIVE")) {
                hexUpgrade.findValidRotations(connected, stations, false);
            } else if (routeAlgorithm.equalsIgnoreCase("RESTRICTIVE")) {
                hexUpgrade.findValidRotations(connected, stations, true);
            } else if (routeAlgorithm.equalsIgnoreCase("SEMI-RESTRICTIVE")) {
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

    public static Set<TileHexUpgrade> createLocated(GUIHex hex, LayTile action) {
        ImmutableSet.Builder<TileHexUpgrade> upgrades = ImmutableSet.builder();

        if (action.getTiles() == null || action.getTiles().isEmpty()) {
            for (TileUpgrade upgrade : hex.getHex().getCurrentTile().getTileUpgrades()) {
                TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
                hexUpgrade.findValidRotations(null, null, true);
                upgrades.add(hexUpgrade);
            }
        } else {
            for (Tile targetTile : action.getTiles()) {
                TileUpgrade upgrade = hex.getHex().getCurrentTile().getSpecificUpgrade(targetTile);
                TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
                hexUpgrade.findValidRotations(null, null, true);
                upgrades.add(hexUpgrade);
            }
        }
        return upgrades.build();
    }

    public static Set<TileHexUpgrade> createCorrection(GUIHex hex, LayTile action) {
        ImmutableSet.Builder<TileHexUpgrade> upgrades = ImmutableSet.builder();

        for (TileUpgrade upgrade : hex.getHex().getCurrentTile().getTileUpgrades()) {
            TileHexUpgrade hexUpgrade = new TileHexUpgrade(hex, upgrade, action);
            hexUpgrade.findValidRotations(null, null, true);
            upgrades.add(hexUpgrade);
        }
        return upgrades.build();
    }

    private void findValidRotations(HexSidesSet connectedSides, Collection<Station> stations, boolean restrictive) {
        MapHex modelHex = hex.getHex();

        // encode HexSides according to the tile current orientation
        if (connectedSides != null) {
            connectedSides = HexSidesSet.rotated(connectedSides, modelHex.getCurrentTileRotation());
        }
        // check invalid sides
        HexSidesSet invalidSides = null;
        if (modelHex.getInvalidSides() != null) {
            invalidSides = HexSidesSet.rotated(modelHex.getInvalidSides(), modelHex.getCurrentTileRotation());
        }

        if (requiresConnection()) {
            HexSidesSet permissive = upgrade.getAllowedRotations(connectedSides, invalidSides,
                    modelHex.getCurrentTileRotation(), stations, false);
            if (restrictive) {
                rotations = upgrade.getAllowedRotations(connectedSides, invalidSides,
                        modelHex.getCurrentTileRotation(), stations, true);
                permissiveRoutePossible = !permissive.isEmpty();
            } else {
                rotations = permissive;
            }
        } else {
            rotations = upgrade.getAllowedRotations(null, invalidSides,
                    modelHex.getCurrentTileRotation(), stations, restrictive);
        }
        // initialize selected Rotation
        selectedRotation = rotations.getNext(HexSide.defaultRotation());
    }

    private boolean validate(Phase phase) {
        invalids.clear();

        /*MBR 25.11.2018
        Check if the action causes this Validation is a Special Tile Lay
        If that is the case the current support special action modifications need to be
        excluded from the validation:
        As of the time of this writing, the following actions are supported.
        A private blocking hex might be unblocked by laying a tile
        The tilelay might be free of cost, or carry a discount
        Future Powers consist of Tile lays in a different Colour than the current Phase (1822)
        */
        if (action.getType()== action.SPECIAL_PROPERTY) {

            SpecialTileLay sp = (SpecialTileLay)action.getSpecialProperty();
            if (!sp.isFree()) {
                if (notEnoughCash(0)) {
                    invalids.add(Invalids.NOT_ENOUGH_CASH);
                }
            }
            if (!(action.getSpecialProperty().getLocations().contains(hex.getHex()))) {
                if (hexIsBlocked()) {
                    invalids.add(Invalids.HEX_BLOCKED);
                }
                if (hexIsReserved()) {
                    invalids.add(Invalids.HEX_RESERVED);
                }
            }
        } else {
            if (hexIsBlocked()) {
                invalids.add(Invalids.HEX_BLOCKED);
            }
            if (hexIsReserved()) {
                invalids.add(Invalids.HEX_RESERVED);
            }
            if (notEnoughCash(0)) {
                invalids.add(Invalids.NOT_ENOUGH_CASH);
            }
        }
        if (noTileAvailable()) {
            invalids.add(Invalids.NO_TILES_LEFT);
        }
      //TODO: Add 1822 Private Powers to remove a small station with a private power
        if (notAllowedForHex()) {
            invalids.add(Invalids.NOT_ALLOWED_FOR_HEX);
        }
        //TODO: Add 1822 Private Powers to upgrade a Tile one phase ahead
        if (notAllowedForPhase(phase)) {
            invalids.add(Invalids.NOT_ALLOWED_FOR_PHASE);
        }
        if (tileColourNotAllowed(phase)) {
            invalids.add(Invalids.COLOUR_NOT_ALLOWED);
        }
        if (tileColourRightMissing()) {
            invalids.add(Invalids.COLOUR_RIGHT_MISSING);
        }
        if (noRouteToNewTrack()) {
            invalids.add(Invalids.NO_ROUTE_TO_NEW_TRACK);
        } else if (noValidRotation()) {
            invalids.add(Invalids.NO_VALID_ORIENTATION);
        }

        return invalids.isEmpty();
    }

    public boolean noValidRotation() {
        return rotations.isEmpty();
    }

    public boolean hexIsBlocked() {
        return hex.getHex().isBlockedByPrivateCompany();
    }

    public boolean hexIsReserved() {
        if ( hex.getHex().isReservedForCompany() && hex.getHex().getReservedForCompany() != action.getCompany()) {
            //check that the hex has not been upgraded already...
            if (hex.getHex().isPreprintedTileCurrent()) {
            return true;
            }
        }
        return false;

    }

    public boolean noTileAvailable() {
        return upgrade.getTargetTile().getFreeCount() == 0;
    }

    public boolean notAllowedForHex() {
        return !upgrade.isAllowedForHex(hex.getHex());
    }

    public boolean notAllowedForPhase(Phase phase) {
        return !upgrade.isAllowedForPhase(phase);
    }

    public boolean tileColourNotAllowed(Phase phase) {
        return !phase.isTileColourAllowed(upgrade.getTargetTile().getColourText());
    }

    public boolean tileColourRightMissing() {
        if (action.getTileColours() == null) {
            return false;
        }
        Integer tileLays = action.getTileColours().get(upgrade.getTargetTile().getColourText());
        return (tileLays == null || tileLays == 0);
    }

    public boolean noRouteToNewTrack() {
        return noValidRotation() && permissiveRoutePossible;
    }

    public boolean notEnoughCash() {
        // correction action does not require cash
        if (action.getType() == LayTile.CORRECTION) {
            return false;
        }
        return action.getCompany().getCash() < this.getCost();
    }

    public boolean notEnoughCash(int discount) {
        // correction action does not require cash
        if (action.getType() == LayTile.CORRECTION) {
            return false;
        }
        return action.getCompany().getCash() < (this.getCost()-discount);
    }

    public boolean requiresConnection() {
        // Yellow Tile on Company Home
        if (upgrade.getTargetTile().getColourText().equalsIgnoreCase(TileColour.YELLOW.name())
                && hex.getHex().isHomeFor(action.getCompany())) {
            return false;
            // Special Property with specified hexes and require connection
            // TODO: Do we require the second test
        } else if (action.getType() == LayTile.SPECIAL_PROPERTY
                && action.getSpecialProperty().getLocations().contains(hex)) {
            // FIXME: action.getSpecialProperty().getLocations() above can't actually contain a GUIHex
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

    public HexSide getCurrentRotation() {
        return selectedRotation;
    }

    public Iterator<HexSide> iterator() {
        return rotations.iterator();
    }

    // HexUpgrade interface method

    @Override
    public boolean hasSingleSelection() {
        return rotations.onlySingle();
    }

    @Override
    public void firstSelection() {
        selectedRotation = rotations.getNext(HexSide.defaultRotation());
    }

    @Override
    public void nextSelection() {
        selectedRotation = rotations.getNext(selectedRotation.next());
    }

    @Override
    public Set<HexUpgrade.Invalids> getInvalids() {
        return ImmutableSet.<HexUpgrade.Invalids>copyOf(invalids);
    }

    @Override
    public boolean isValid() {
        return invalids.isEmpty();
    }

    @Override
    public int getCost() {
        return action.getPotentialCost(hex.getHex());
    }

    @Override
    public Image getUpgradeImage(int zoomStep) {
        HexSide imageRotation;
        if (selectedRotation == null) {
            imageRotation = HexSide.defaultRotation();
        } else {
            imageRotation = selectedRotation;
        }

        // get unscaled image for this orientation
        BufferedImage hexImage = GUITile.getTileImage(upgrade.getTargetTile(), imageRotation, zoomStep);

        return hexImage;
    }

    @Override
    public String getUpgradeText() {
        Tile tile = upgrade.getTargetTile();

        StringBuilder text = new StringBuilder();
        text.append("<HTML>" + tile.toText());
        if (!tile.isUnlimited()) {
            // line break before # of available tiles
            text.append("<BR>");
            text.append(" (" + tile.getFreeCount() + ")");
        }
        // text for special property
        if (action.getSpecialProperty() != null) {
            text.append(
                    "<BR> <font color=red> ["
                            + action.getSpecialProperty().getOriginalCompany().getId()
                            + "] </font>");
        }
        text.append("</HTML>");
        return text.toString();
    }

    @Override
    public String getUpgradeToolTip() {

        StringBuilder tt = new StringBuilder("<html>");

        if (!isValid()) {
            tt.append(invalidToolTip());
        }

        Tile tile = upgrade.getTargetTile();
        tt.append("<b>Tile</b>: ").append(tile.toText());
        if (tile.hasStations()) {
            int cityNumber = 0;
            // Tile has stations, but
            for (Station st : tile.getStations()) {
                cityNumber++; // = city.getNumber();
                tt.append("<br>  ").append(st.toText()).append(
                        cityNumber) // .append("/").append(st.getNumber())
                        .append(": value ");
                tt.append(st.getValue());
                if (st.getBaseSlots() > 0) {
                    tt.append(", ").append(st.getBaseSlots()).append(
                            " slots");
                }
            }
        }
        tt.append("</html>");
        return tt.toString();
    }

    private String invalidToolTip() {
        StringBuilder tt = new StringBuilder();

        tt.append("<b><u>");
        tt.append(LocalText.getText("TILE_UPGRADE_TT_INVALID"));
        tt.append("</u></b><br>");

        tt.append("<b>");
        for (Invalids invalid : invalids) {
            tt.append(invalid.toString() + "<br>");
        }
        tt.append("</b>");

        return tt.toString();
    }

    @Override
    public int getCompareId() {
        return 2;
    }

    @Override
    public Comparator<HexUpgrade> getComparator() {
        return new Comparator<HexUpgrade>() {
            @Override
            public int compare(HexUpgrade u1, HexUpgrade u2) {
                if (u1 instanceof TileHexUpgrade && u2 instanceof TileHexUpgrade) {
                    TileHexUpgrade tu1 = (TileHexUpgrade) u1;
                    TileHexUpgrade tu2 = (TileHexUpgrade) u2;
                    return ComparisonChain.start()
                            .compare(tu1.getAction(), tu2.getAction())
                            .compare(tu1.getUpgrade().getTargetTile(), tu2.getUpgrade().getTargetTile())
                            .compare(tu1.getHex().getHex().getId(), tu2.getHex().getHex().getId())
                            .result()
                            ;
                }
                return 0;
            }
        };
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Hex", hex.toString())
                .add("Upgrade", upgrade)
                .add("rotations", rotations)
                .toString();
    }

    /**
     * sets both validation and visibility for upgrades
     */
    public static void validates(Iterable<TileHexUpgrade> upgrades, Phase current) {
        for (TileHexUpgrade upgrade : upgrades) {
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
