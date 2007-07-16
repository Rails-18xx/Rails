package rails.game;

import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.special.SpecialPropertyI;

/**
 * A common interface to the various "Rounds". A Round is defined as any process
 * in an 18xx rails.game where different players have "turns".
 */
public interface RoundI
{

	/**
	 * Get the player that has the next turn.
	 * 
	 * @return Player object.
	 */
	public Player getCurrentPlayer();
	
	public String getHelp();
	
	public List<SpecialPropertyI> getSpecialProperties();
	
    public boolean process (PossibleAction action);
    
    public boolean setPossibleActions();
    
}
