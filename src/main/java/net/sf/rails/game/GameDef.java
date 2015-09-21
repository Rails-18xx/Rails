package net.sf.rails.game;

import net.sf.rails.game.financial.StockRound;

/**
 * This class will hold parameter identifiers for use in the game engine only.
 * See rails.common.Defs for parameters used in the GUI/engine communication.
 * @author VosE
 *
 */
public class GameDef {

    public enum Parm {

        NO_SALE_IN_FIRST_SR (false),
        NO_SALE_IF_NOT_OPERATED (false),
        STOCK_ROUND_SEQUENCE(StockRound.SELL_BUY_SELL),
        PLAYER_SHARE_LIMIT (60),
        POOL_SHARE_LIMIT(50),
        TREASURY_SHARE_LIMIT(50),
        FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS(false),
        SKIP_FIRST_STOCK_ROUND(false),
        NO_SALE_OF_JUST_BOUGHT_CERT(false),
        REMOVE_TRAIN_BEFORE_SR(false),
        EMERGENCY_MUST_BUY_CHEAPEST_TRAIN (true),
        EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN (false),
        EMERGENCY_MAY_BUY_FROM_COMPANY (true);

        private Object defaultValue;

        Parm (boolean defaultValue) { this.defaultValue = defaultValue; }
        Parm (int defaultValue) { this.defaultValue = defaultValue; }

        public Object defaultValue() { return defaultValue; }
        public boolean defaultValueAsBoolean() { return (Boolean) defaultValue; }
        public int defaultValueAsInt() { return (Integer) defaultValue; }
    }

    /**
     * OR step values
     * @author Erik
     */
    public enum OrStep {

        /* In-sequence steps */
        INITIAL,
        LAY_TRACK,
        LAY_TOKEN,
        CALC_REVENUE,
        PAYOUT,
        BUY_TRAIN,
        TRADE_SHARES,
        REPAY_LOANS,
        FINAL,

        /* Out-of-sequence steps*/
        DISCARD_TRAINS;

    }
    
    public static Object getGameParameter(RailsItem item, GameDef.Parm key) {
        return item.getRoot().getGameManager().getGameParameter(key);
    }

    public static int getGameParameterAsInt(RailsItem item, GameDef.Parm key) {
        if (key.defaultValue() instanceof Integer) {
            return (Integer) getGameParameter(item, key);
        } else {
            return -1;
        }
    }

    public static boolean getGameParameterAsBoolean(RailsItem item, GameDef.Parm key) {
        if (key.defaultValue() instanceof Boolean) {
            return (Boolean) getGameParameter(item, key);
        } else {
            return false;
        }
    }



}
