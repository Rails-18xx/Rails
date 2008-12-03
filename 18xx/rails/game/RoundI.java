/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/RoundI.java,v 1.8 2008/12/03 20:15:15 evos Exp $ */
package rails.game;

import rails.game.action.PossibleAction;

/**
 * A common interface to the various "Rounds". A Round is defined as any process
 * in an 18xx rails.game where different players have "turns".
 */
public interface RoundI {

    public void setGameManager (GameManager gameManager);
    
    public GameManager getGameManager ();
    
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
