/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Attic/PhaseManagerI.java,v 1.3 2007/10/05 22:02:28 evos Exp $ */
package rails.game;

public interface PhaseManagerI
{

	public int getCurrentPhaseIndex();

	public PhaseI getCurrentPhase();

	public void setPhase(String name);
	
}
