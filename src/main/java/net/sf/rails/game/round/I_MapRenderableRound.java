package net.sf.rails.game.round;

import net.sf.rails.game.round.RoundFacade;

/**
 * A marker interface for any Round that must be rendered on the
 * main 'MAP' panel (i.e., the ORPanel) instead of the
 * default SRPanel.
 *
 * This allows rounds like PrussianFormationRound to be rendered on the map
 * without having to inherit from StockRound, decoupling the UI
 * from the class hierarchy.
 */
public interface I_MapRenderableRound extends RoundFacade {
    // This interface is intentionally blank.
}