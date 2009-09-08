/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Attic/PhaseManagerI.java,v 1.7 2009/09/08 21:48:59 evos Exp $ */
package rails.game;

public interface PhaseManagerI
{
    public void init (GameManagerI gameManager);

	public int getCurrentPhaseIndex();

	public PhaseI getCurrentPhase();

	public void setPhase(String name);

    public PhaseI getPhaseNyName (String name);

    public boolean hasReachedPhase (String phaseName);
}
