package rails.common;

import java.util.EnumMap;

public class Defs {

    /* Identifiers and default names for configurable UI classes */
    public enum ClassName {
        OR_UI_MANAGER,
        STATUS_WINDOW,
        GAME_STATUS
    }
    
    private static EnumMap<Defs.ClassName, String> defaultClasses =
        new EnumMap<Defs.ClassName, String> (Defs.ClassName.class);
    
    static {
        defaultClasses.put (ClassName.OR_UI_MANAGER, 
                "rails.ui.swing.ORUIManager");
        defaultClasses.put (ClassName.STATUS_WINDOW, 
                "rails.ui.swing.StatusWindow");
        defaultClasses.put (ClassName.GAME_STATUS, 
                "rails.ui.swing.GameStatus");
    }
    
    public static String getDefaultClassName (ClassName key) {
        return defaultClasses.get(key);
    }
    
    /* Definitions for key/value pairs in the communication
     * between GameManager and GameUIManager.
     */
    
    public enum Parm {
        
        HAS_ANY_PAR_PRICE,
        CAN_ANY_COMPANY_HOLD_OWN_SHARES,
        CAN_ANY_COMPANY_BUY_PRIVATES,
        DO_BONUS_TOKENS_EXIST
    }
}
