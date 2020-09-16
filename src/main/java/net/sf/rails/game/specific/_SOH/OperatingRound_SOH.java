package net.sf.rails.game.specific._SOH;

import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;

import java.util.ArrayList;
import java.util.List;

public class OperatingRound_SOH extends OperatingRound {

    public OperatingRound_SOH (GameManager parent, String id) {

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
     * @param colour The colour name as String
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
}
