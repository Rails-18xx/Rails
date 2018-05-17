using System;


namespace GameLib.Net.Common
{
    public class GuiDef
    {
        /** Identifiers and default names for configurable UI classes */
        public class ClassName
        {

            public static ClassName GAME_UI_MANAGER = new ClassName("rails.ui.swing.GameUIManager");
            public static ClassName OR_UI_MANAGER = new ClassName("rails.ui.swing.ORUIManager");
            public static ClassName STATUS_WINDOW = new ClassName("rails.ui.swing.StatusWindow");
            public static ClassName GAME_STATUS = new ClassName("rails.ui.swing.GameStatus");
            public static ClassName OR_WINDOW = new ClassName("rails.ui.swing.ORWindow");
            public static ClassName START_ROUND_WINDOW = new ClassName("rails.ui.swing.StartRoundWindow");

            private string defaultClassName;

            ClassName(string defaultClassName)
            {
                this.defaultClassName = defaultClassName;
            }

            public string GetDefaultClassName()
            {
                return defaultClassName;
            }
        }

        public static String GetDefaultClassName(ClassName key)
        {
            return key.GetDefaultClassName();
        }

        /** Definitions for key/value pairs in the communication
         * between GameManager and GameUIManager.
         */

        public enum Parm
        {

            HAS_ANY_PAR_PRICE,
            CAN_ANY_COMPANY_HOLD_OWN_SHARES,
            CAN_ANY_COMPANY_BUY_PRIVATES,
            DO_BONUS_TOKENS_EXIST,
            HAS_ANY_COMPANY_LOANS,
            HAS_ANY_RIGHTS,
            NO_MAP_MODE,
            REVENUE_SUGGEST,
            ROUTE_HIGHLIGHT,
            PLAYER_ORDER_VARIES
        }

        /**
         * Definitions for UI window types, used by the server
         * to pass visibility hints to the UI.
         */
        public enum Panel
        {

            START_ROUND,
            STATUS,
            MAP,
            STOCK_MARKET
        }
    }
}
