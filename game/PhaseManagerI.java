/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PhaseManagerI.java,v 1.1 2005/10/16 15:02:10 evos Exp $
 * 
 * Created on 16-Oct-2005
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public interface PhaseManagerI {
    
	public int getCurrentPhaseIndex ();
	
	public PhaseI getCurrentPhase ();
	
	public void setNextPhase ();
	
	public void setPhase (String name);
}
