package net.sf.rails.ui.swing.gamespecific._1844;

import net.sf.rails.ui.swing.StatusWindow;

public class StatusWindow_1844 extends StatusWindow {

    private static final long serialVersionUID = 1L;

    public StatusWindow_1844() {
        super();
    }

    @Override
    protected boolean updateSpecialActionMenu() {
        // If we have already specical actions make sure the are shown
        Boolean enabled = super.updateSpecialActionMenu();
        // Check that we are in a StockRound

        // Add the actions to buy 1 of the remaining Private Companies
        // Either a Tunnel or a Mountain Railway

        return enabled;

    }

}

