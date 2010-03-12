package rails.game;

import rails.common.GuiDef;
import rails.common.GuiHints;
import rails.util.LocalText;

/**
 * EndOfGameRound is a dummy implementation of the Round class
 * It generates no additional actions.
 * It also sets guiHints (default: shows map, stock market and activates status) 
 *
 *  */

public class EndOfGameRound extends Round {

    public EndOfGameRound(GameManagerI gameManager) {
        super(gameManager);
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

    public void setGuiHints(GuiHints guiHints) {
        this.guiHints = guiHints; 
    }
    
    @Override
    public String getHelp() {
        return LocalText.getText("EndOfGameHelpText");
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
