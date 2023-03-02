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
        TREASURY_SHARE_LIMIT(50), // No longer directly used, now only a default (EV 02/2023).
        FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS(false),
        SKIP_FIRST_STOCK_ROUND(false),
        NO_SALE_OF_JUST_BOUGHT_CERT(false),
        NO_SALE_OF_JUST_STARTED_COMPANY(false),
        NO_CERTIFICATE_SPLIT_ON_SELLING(false),
        REMOVE_TRAIN_BEFORE_SR(false),
        EMERGENCY_MUST_BUY_CHEAPEST_TRAIN (true),
        EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN (false),
        EMERGENCY_MAY_BUY_FROM_COMPANY (true),
        EMERGENCY_MAY_ADD_PRES_CASH_FROM_COMPANY (true),
        EMERGENCY_MUST_SELL_TREASURY_SHARES(false),
        EMERGENCY_MUST_TAKE_LOANS(false),
        EMERGENCY_COMPANY_BANKRUPTCY(false),
        DUAL_TRAIN_BECOMES_UNDECIDED_IN_POOL (false),
        MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE (false),
        REMOVE_PERMANENT (false),
        BANKRUPTCY_STYLE (Bankruptcy.Style.DEFAULT);

        private Object defaultValue;

        Parm (boolean defaultValue) { this.defaultValue = defaultValue; }
        Parm (int defaultValue) { this.defaultValue = defaultValue; }
        Parm (Object defaultValue) {this.defaultValue = defaultValue; }

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
        CHECK_DESTINATIONS,
        LAY_TOKEN,
        CALC_REVENUE,
        PAYOUT,
        BUY_TRAIN,
        TRADE_SHARES,
        REPAY_LOANS,
        BUY_BONDS,
        FINAL,

        /* Out-of-sequence steps*/
        DISCARD_TRAINS,
        EXCHANGE_TOKENS,
    }
    
    public static Object getParm(RailsItem item, GameDef.Parm key) {
        return item.getRoot().getGameManager().getGameParameter(key);
    }

    public static int getParmAsInt(RailsItem item, GameDef.Parm key) {
        if (key.defaultValue() instanceof Integer) {
            return (Integer) getParm(item, key);
        } else {
            return -1;
        }
    }

    public static boolean getParmAsBoolean(RailsItem item, GameDef.Parm key) {
        if (key.defaultValue() instanceof Boolean) {
            return (Boolean) getParm(item, key);
        } else {
            return false;
        }
    }
}
