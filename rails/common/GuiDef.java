package rails.common;

public class GuiDef {

    /** Identifiers and default names for configurable UI classes */
    public enum ClassName {

        GAME_UI_MANAGER ("rails.ui.swing.GameUIManager"),
        OR_UI_MANAGER ("rails.ui.swing.ORUIManager"),
        STATUS_WINDOW ("rails.ui.swing.StatusWindow"),
        GAME_STATUS ("rails.ui.swing.GameStatus"),
        OR_WINDOW ("rails.ui.swing.ORWindow");

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
        NO_MAP_MODE,
        REVENUE_SUGGEST,
        ROUTE_HIGHLIGHT
    }

    /**
     * Definitions for UI window types, used by the server
     * to pass visibility hints to the UI.
     */
    public enum Panel {

        START_ROUND,
        STATUS,
        MAP,
        STOCK_MARKET
    }
    
 }
