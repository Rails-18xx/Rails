using GameLib.Net.Game.Financial;
using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

/**
 * This class will hold parameter identifiers for use in the game engine only.
 * See rails.common.Defs for parameters used in the GUI/engine communication.
 * @author VosE
 *
 */

namespace GameLib.Net.Game
{
    public class GameDef
    {
        public class Parm
        {

            public static Parm NO_SALE_IN_FIRST_SR = new Parm(false);
            public static Parm NO_SALE_IF_NOT_OPERATED = new Parm(false);
            public static Parm STOCK_ROUND_SEQUENCE = new Parm(StockRound.SELL_BUY_SELL);
            public static Parm PLAYER_SHARE_LIMIT = new Parm(60);
            public static Parm POOL_SHARE_LIMIT = new Parm(50);
            public static Parm TREASURY_SHARE_LIMIT = new Parm(50);
            public static Parm FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS = new Parm(false);
            public static Parm SKIP_FIRST_STOCK_ROUND = new Parm(false);
            public static Parm NO_SALE_OF_JUST_BOUGHT_CERT = new Parm(false);
            public static Parm REMOVE_TRAIN_BEFORE_SR = new Parm(false);
            public static Parm EMERGENCY_MUST_BUY_CHEAPEST_TRAIN = new Parm(true);
            public static Parm EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN = new Parm(false);
            public static Parm EMERGENCY_MAY_BUY_FROM_COMPANY = new Parm(true);

            public static List<Parm> Values = new List<Parm>()
            {
                NO_SALE_IN_FIRST_SR,
                NO_SALE_IF_NOT_OPERATED,
                STOCK_ROUND_SEQUENCE,
                PLAYER_SHARE_LIMIT,
                POOL_SHARE_LIMIT,
                TREASURY_SHARE_LIMIT,
                FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS,
                SKIP_FIRST_STOCK_ROUND,
                NO_SALE_OF_JUST_BOUGHT_CERT,
                REMOVE_TRAIN_BEFORE_SR,
                EMERGENCY_MUST_BUY_CHEAPEST_TRAIN,
                EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN,
                EMERGENCY_MAY_BUY_FROM_COMPANY
            };

            private object defaultValue;

            Parm(bool defaultValue) { this.defaultValue = defaultValue; }
            Parm(int defaultValue) { this.defaultValue = defaultValue; }

            public object DefaultValue() { return defaultValue; }
            public bool DefaultValueAsBoolean() { return (bool)defaultValue; }
            public int DefaultValueAsInt() { return (int)defaultValue; }
        }

        /**
         * OR step values
         * @author Erik
         */
        public enum OrStep
        {

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
            DISCARD_TRAINS
        }

        public static object GetGameParameter(IRailsItem item, GameDef.Parm key)
        {
            return item.GetRoot.GameManager.GetGameParameter(key);
        }

        public static int GetGameParameterAsInt(IRailsItem item, GameDef.Parm key)
        {
            if (key.DefaultValue() is int)
            {
                return (int)GetGameParameter(item, key);
            }
            else
            {
                return -1;
            }
        }

        public static bool GetGameParameterAsBoolean(IRailsItem item, GameDef.Parm key)
        {
            if (key.DefaultValue() is bool)
            {
                return (bool)GetGameParameter(item, key);
            }
            else
            {
                return false;
            }
        }
    }
}
