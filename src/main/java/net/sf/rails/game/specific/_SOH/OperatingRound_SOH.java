package net.sf.rails.game.specific._SOH;

import net.sf.rails.game.*;
import net.sf.rails.game.special.SpecialTileLay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.LayTile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OperatingRound_SOH extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_SOH.class);

    public OperatingRound_SOH(GameManager parent, String id) {

        super(parent, id);

        steps = new GameDef.OrStep[]{
                GameDef.OrStep.INITIAL,
                GameDef.OrStep.TRADE_SHARES,
                GameDef.OrStep.LAY_TRACK,
                GameDef.OrStep.LAY_TOKEN,
                GameDef.OrStep.CALC_REVENUE,
                GameDef.OrStep.PAYOUT,
                GameDef.OrStep.BUY_TRAIN,
                GameDef.OrStep.FINAL
        };
    }

    /**
     * The tile laying rule for SOH is: either two yellow,
     * or (from phase 3) one yellow and one upgrade, in any sequence,
     * and possibly on the same hex.
     *
     * @param colour           The colour name as String
     * @param oldAllowedNumber The (old) number of allowed lays for that colour
     */
    protected void updateAllowedTileColours(String colour, int oldAllowedNumber) {

        List<String> coloursToRemove = new ArrayList<>();

        int allowance;

        if (oldAllowedNumber > 1) {
            // First yellow tile laid: reduce yellow tile allowance only
            tileLaysPerColour.put(colour, oldAllowedNumber - 1);
        } else {
            // Green or brown upgrade laid: reduce all tile allowances
            for (String key : tileLaysPerColour.viewKeySet()) {
                allowance = tileLaysPerColour.get(key);
                if (allowance <= 1) {
                    coloursToRemove.add(key);
                } else {
                    tileLaysPerColour.put(key, allowance - 1);
                }
            }
        }

        // Two-step removal to prevent ConcurrentModificationException.
        for (String key : coloursToRemove) {
            tileLaysPerColour.remove(key);
        }
    }

    /**
     * Calculate cost of bridge building (connecting tracks across a river)
     *
     * @param action      The LayTile action being processed
     * @return The total cost of all bridges being built, or zero if none is built
     */
    @Override
    protected int tileLayCost(LayTile action) {

        int cost = super.tileLayCost(action);

        SpecialTileLay stl = action.getSpecialProperty();
        if (stl != null
                && stl.getOriginalCompany().getId().equalsIgnoreCase(GameDef_SOH.KKI)) {
            return cost;
        }

        MapHex hex = action.getChosenHex();
        Tile tile = action.getLaidTile();
        int rotation = action.getOrientation();

        HexSidesSet newBridges = mapManager.findNewBridgeSides(hex, tile, rotation);
        if (newBridges == null || newBridges.isEmpty()) return cost;

        int bridgeCost = 0;
        Iterator it = newBridges.iterator();
        HexSide side;
        while (it.hasNext()) {
            side = (HexSide) it.next();
            log.info("New bridge: {}", side);
            bridgeCost += GameDef_SOH.BRIDGE_COST;
        }

        log.info("Bridge cost={}", bridgeCost);

        return cost + bridgeCost;
    }

    /**
     * The AS SpecialLayTile action should be enabled only
     * if the normal tile lays are exhausted, otherwise it turns out
     * that the *first* tile lay executes the special property.
     * @param forReal False if only called to test if this SP can be used.
     */
    @Override
    protected List<LayTile> getSpecialTileLays(boolean forReal) {

        if (forReal && possibleActions.contains(LayTile.class)
                && operatingCompany.value().ownsPrivate(GameDef_SOH.AS)) {
            // Postpone adding the AS third tile lay SP until the normal lays are exhausted
            return new ArrayList<>();
        } else {
            return super.getSpecialTileLays(forReal);
        }
    }
}
