package net.sf.rails.common;

public class GuiDef {

    /** Identifiers and default names for configurable UI classes */
    public enum ClassName {

        GAME_UI_MANAGER ("net.sf.rails.ui.swing.GameUIManager"),
        OR_UI_MANAGER ("net.sf.rails.ui.swing.ORUIManager"),
        STATUS_WINDOW ("net.sf.rails.ui.swing.StatusWindow"),
        GAME_STATUS ("net.sf.rails.ui.swing.GameStatus"),
        OR_WINDOW ("net.sf.rails.ui.swing.ORWindow"),
        START_ROUND_WINDOW ("net.sf.rails.ui.swing.StartRoundWindow");

        private String defaultClassName;

        ClassName (String defaultClassName) {
            this.defaultClassName = defaultClassName;
        }

        public String getDefaultClassName () {
            return defaultClassName;
        }
    }

    public static String getDefaultClassName (ClassName key) {
        return key.getDefaultClassName();
    }

    /** Definitions for key/value pairs in the communication
     * between GameManager and GameUIManager.
     */

    public enum Parm {

        HAS_ANY_PAR_PRICE,
        CAN_ANY_COMPANY_HOLD_OWN_SHARES,
        CAN_ANY_COMPANY_BUY_PRIVATES,
        DO_BONUS_TOKENS_EXIST,
        HAS_ANY_COMPANY_LOANS,
        HAS_ANY_RIGHTS,
        NO_MAP_MODE,
        REVENUE_SUGGEST,
        ROUTE_HIGHLIGHT, 
        PLAYER_ORDER_VARIES,
        HAS_SPECIAL_COMPANY_INCOME,  // E.g. coal mines in 1837
        HAS_GROWING_NUMBER_OF_SHARES,   // For at least one company
        HAS_BONDS,
    }

    /**
     * Definitions for UI window types, used by the server
     * to pass visibility hints to the UI.
     */
    public enum Panel {
        START_ROUND,     // Also used for Parliament Round in 1862
        STATUS,
        MAP,
        STOCK_MARKET
    }

}
