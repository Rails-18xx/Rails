package net.sf.rails.game;

import net.sf.rails.common.GuiDef;
import net.sf.rails.common.GuiHints;
import net.sf.rails.common.LocalText;

/**
 * EndOfGameRound is a dummy implementation of the Round class
 * It generates no additional actions.
 * It also sets guiHints (default: shows map, stock market and activates status) 
 *
 *  */

public final class EndOfGameRound extends Round {

    /**
     * Constructed via Configure
     */
   public EndOfGameRound(GameManager parent, String id) {
        super(parent, id);
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);
    }
    
    @Override
    public boolean setPossibleActions() {
        possibleActions.clear();
        return true;
    }
    
    public GuiHints getGuiHints() {
        return guiHints;
    }
    
    @Override
    public String toString() {
        return "EndOfGameRound ";
    }

    @Override
    public String getRoundName() {
        return toString();
    }
    
}
