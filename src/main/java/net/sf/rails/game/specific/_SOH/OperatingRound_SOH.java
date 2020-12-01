package net.sf.rails.game.specific._SOH;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.PlayerShareUtils;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.special.SpecialTileLay;
import net.sf.rails.game.state.Owner;
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

    /**
     * A pre-check for bankruptcy in emergency train buying
     * before anything is done.<br>
     * Developed for Steam Over Holland, where treasury shares
     * must be sold first (in a TreasuryShareRound), and
     * player certificates later (in a Share SellingRound).
     *
     * This pre-check avoids having to invoke the above-mentioned
     * two classes if bankruptcy is unavoidable, in which case
     * nothing is sold and no cash is moved.
     *
     * To be called <i>after</i> a train has been selected,
     * and only if treasury cash is insufficient, because
     * buying a train from another company may save the current company.
     *
     * Currently only handles company bankruptcy, as in SOH.
     * Can possibly be extended to cover player bankruptcy.
     * See also the Javadoc in OperatingRound.
     * @param owner The company needing a train.
     *              The Owner type allows to specify a player,
     *              if that would help in other games.
     * @param cashToRaise The extra cash required to buy the selected train.
     * @return True if bankruptcy is inevitable.
     */
    public boolean willBankruptcyOccur(Owner owner,
                                       int cashToRaise) {
        int raisableCash = 0;

        if (owner instanceof PublicCompany) {

            // Company cash
            PublicCompany company = (PublicCompany) owner;
            raisableCash = company.getCash();
            log.info ("Company cash: {}", raisableCash);
            if (raisableCash >= cashToRaise) return false; // escaped

            int maxSharesInPool = gameManager.getParmAsInt(
                    GameDef.Parm.POOL_SHARE_LIMIT) / company.getShareUnit();

            // Sell treasury shares
            if (company.canHoldOwnShares() && gameManager.getParmAsBoolean (
                    GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES)) {
                // Treasury shares to be sold first
                int sharesInTreasury = company.getPortfolioModel().getShareNumber(company);
                int sharesInPool = pool.getShareNumber(company);
                int sharesToSell = Math.min (sharesInTreasury, maxSharesInPool - sharesInPool);
                raisableCash += sharesToSell * company.getMarketPrice();
                log.info ("Cash after selling {} treasury shares: {}", sharesToSell, raisableCash);
                if (raisableCash >= cashToRaise) return false; // escaped
            }

            // President cash
            Player player = company.getPresident();
            raisableCash += player.getCash();
            log.info ("Cash after adding president cash: {}", raisableCash);
            if (raisableCash >= cashToRaise) return false; // escaped

            // Sell player shares, no dumps allowed
            PortfolioModel playerPortfolio = player.getPortfolioModel();
            int poolAllowsShares = PlayerShareUtils.poolAllowsShareNumbers(company);
            for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
                int ownedShares = playerPortfolio.getShareNumber(comp);
                if (ownedShares == 0) continue;

                /* May not sell more than the Pool can accept */
                int maxSharesToSell = Math.min(ownedShares, poolAllowsShares);
                if (maxSharesToSell == 0) continue;

                /* May not sell if a dump would occur */
                if (company.getPresident() == player) {
                    Player potential = company.findPlayerToDump();
                    if (potential != null) {
                        // May not sell more shares than this other player has
                        maxSharesToSell = Math.min(maxSharesToSell,
                                potential.getPortfolioModel().getShareNumber(comp));
                        if (maxSharesToSell == 0) continue;
                        raisableCash += maxSharesToSell * comp.getMarketPrice();
                        log.info("Cash after selling {} {} shares: {}",
                                maxSharesToSell, comp.getId(), raisableCash);
                    }
                }
            }
        }
        return raisableCash < cashToRaise;
    }


}
