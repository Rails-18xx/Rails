/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Attic/PhaseManagerI.java,v 1.5 2008/06/04 19:00:31 evos Exp $ */
package rails.game;

public interface PhaseManagerI {

    public int getCurrentPhaseIndex();

    public PhaseI getCurrentPhase();

    public void setPhase(String name);

    public PhaseI getPhaseNyName(String name);

    public boolean hasReachedPhase(String phaseName);
}
