/*
 * Created on 30-Apr-2005
 */
package game;

/**
 * A common interface to thr various "Round".
 * A Round is defined as any process in an 18xx game where different players have "turns". 
 * @author Erik Vos
 */
public interface Round {
	
	/**
	 * Get the player tha thas the next turn.
	 * @return Player object.
	 */
	public Player getCurrentPlayer ();

}
