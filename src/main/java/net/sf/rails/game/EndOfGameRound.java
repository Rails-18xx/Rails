package net.sf.rails.game;

import net.sf.rails.common.GuiDef;
import net.sf.rails.common.GuiHints;
import net.sf.rails.common.LocalText;

// --- DELETE ---
// import javax.swing.*;
// import java.awt.BorderLayout;
// import java.awt.Font;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EndOfGameRound: Placeholder for game end state.
 */
public final class EndOfGameRound extends Round {

    private static final Logger log = LoggerFactory.getLogger(EndOfGameRound.class);
    // --- DELETE ---
    // private boolean reportShown = false;

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
        
        // Logic to show FinalRankingDialog has been completely removed.
        // This method now ensures no erroneous buttons appear, but triggers no UI.
        return true;
    }
    
    // Class FinalRankingDialog removed entirely.

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