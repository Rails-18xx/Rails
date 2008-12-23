/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/RoundI.java,v 1.9 2008/12/23 19:55:29 evos Exp $ */
package rails.game;

import rails.game.action.PossibleAction;

/**
 * A common interface to the various "Rounds". A Round is defined as any process
 * in an 18xx rails.game where different players have "turns".
 */
public interface RoundI {

    /**
     * Get the player that has the next turn.
     * 
     * @return Player object.
     */
    public Player getCurrentPlayer();

    public String getHelp();

    public boolean process(PossibleAction action);

    public boolean setPossibleActions();

}
