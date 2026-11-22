package rails.game.action;

import java.awt.Color;
import net.sf.rails.game.state.Owner;
import java.awt.event.KeyEvent;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Player;

public interface GuiTargetedAction {

    Owner getActor();
    String getGroupLabel();
    String getButtonLabel();
    Color getButtonColor();


    // --- COMPATIBILITY LAYER (Fixes GameStatus errors) ---
    
    /** Legacy alias for getActor() */
    default Object getTarget() {
        return getActor();
    }

/** * Identifies the human player responsible for this action.
     * Used for the "Middle Header" in the UI.
     */
    default String getPlayerName() {
        Owner actor = getActor();
        
        // precise logic: If the actor is a Company, get its President.
        if (actor instanceof PublicCompany) {
            Player p = ((PublicCompany) actor).getPresident();
            if (p != null) {
                return p.getName();
            }
        }
        
        // Fallback: If the actor is already a Player (unlikely for Discard, but possible)
        if (actor instanceof Player) {
            return ((Player) actor).getName();
        }

        return ""; // Return empty string to keep the label blank but safe
    }


    /** Legacy alias for getButtonColor() */
    default Color getHighlightColor() {
        return getButtonColor();
    }
    
    /** Legacy alias for getGroupLabel() */
    default String getInfoText() {
        return getGroupLabel();
    }

    // Optional addition to GuiTargetedAction.java
default String getToolTip() {
    return null; // ORPanel will ignore null, or fall back to toString()
}

// --- VISUAL SIGNATURE PROTOCOL ---
    
    /** * The fill color for the UI element (Button or Card).
     * Defaults to the existing getButtonColor() for compatibility.
     */
    default Color getHighlightBackgroundColor() {
        return getButtonColor();
    }

    /** * The stroke color for the UI element's border.
     * Defaults to a darker version of the background for contrast.
     */
    default Color getHighlightBorderColor() {
        Color bg = getHighlightBackgroundColor();
        return (bg != null) ? bg.darker() : Color.GRAY;
    }

    /** * The text color. Defaults to Black.
     */
    default Color getHighlightTextColor() {
        return Color.BLACK;
    }

/**
     * Hotkey Management.
     * @return The KeyEvent virtual key code (e.g. KeyEvent.VK_ENTER), or 0 if none.
     */
    default int getHotkey() {
        return 0;
    }

}