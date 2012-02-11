/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ActionButton.java,v 1.5 2008/06/04 19:00:38 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;

import rails.common.parser.Config;
import rails.game.action.ActionTaker;
import rails.game.action.PossibleAction;

/**
 * A subclass of JButton that allows linking "PossibleAction" objects to it.
 * 
 * @see ClickField
 */
public class ActionButton extends JButton implements ActionTaker {

    private static final long serialVersionUID = 1L;

    private static final Set<String> KEYS_TEXT_DISPLAY = new HashSet<String>
    (Arrays.asList( new String[] {
            "text and icon",
            "only text",
            "",
            null
    }));
    private static final Set<String> KEYS_ICON_DISPLAY = new HashSet<String>
    (Arrays.asList( new String[] {
            "text and icon",
            "only icon"
    }));

    private static Set<ActionButton> actionButtons = new HashSet<ActionButton>();

    private List<PossibleAction> actions = new ArrayList<PossibleAction>(1);

    /**
     * null value means that the action button is not set up by an appropriate
     * RailsIcon (eg., by calling setText directly).
     */
    private RailsIcon railsIcon = null;
    
    public ActionButton(RailsIcon railsIcon) {
        super();
        setRailsIcon(railsIcon);
        this.setMargin(new Insets(2,2,2,2));
        actionButtons.add(this);
    }

    public void addPossibleAction(PossibleAction o) {
        actions.add(o);
    }

    public List<PossibleAction> getPossibleActions() {
        return actions;
    }

    public void clearPossibleActions() {
        actions.clear();
    }

    public void setPossibleAction(PossibleAction action) {
        clearPossibleActions();
        addPossibleAction(action);
    }

    public void setRailsIcon(RailsIcon railsIcon) {
        if (railsIcon == null) railsIcon = RailsIcon.NULL;
        this.railsIcon = railsIcon;
        showRailsIcon();
    }
    
    /**
     * Display according to configuration.
     * If no text/icon is attached, then icon/text is displayed as fallback
     * (irrespective of configuration).
     * Text becomes the tool tip text in case of icon-only display.
     */
    private void showRailsIcon() {
        if (railsIcon != null) {
            if (isTextEnabled() || railsIcon.largeIcon == null) {
                super.setText(railsIcon.description);
            } else {
                super.setText(null);
            }
            if (isIconEnabled() || railsIcon.description == null) {
                if (isIconSizeSmall()) {
                    super.setIcon(railsIcon.smallIcon);
                } else {
                    super.setIcon(railsIcon.largeIcon);
                }
                if (!isTextEnabled()) {
                    super.setToolTipText(railsIcon.description);
                }
            } else {
                super.setIcon(null);
            }
        }
    }
    
    private boolean isTextEnabled() {
        return KEYS_TEXT_DISPLAY.contains(Config.get("actionButton.iconText",""));
    }

    private boolean isIconEnabled() {
        return KEYS_ICON_DISPLAY.contains(Config.get("actionButton.iconText",""));
    }
    
    private boolean isIconSizeSmall() {
        return "small".equals(Config.get("actionButton.iconSize"));
    }

    /**
     * Should only be used if an arbitrary text is to displayed without icon.
     * In any other case, setRailsIcon should be used.
     */
    @Override
    public void setText(String text) {
        super.setText(text);
        setIcon(null);
        railsIcon = null;
    }
    
    /**
     * To be called upon change of button display type
     */
    public static void resetRailsIcons() {
        for (ActionButton ab : actionButtons) {
            ab.showRailsIcon();
        }
    }
    
}
