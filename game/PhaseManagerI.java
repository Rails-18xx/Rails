package game;

public interface PhaseManagerI
{

	public int getCurrentPhaseIndex();

	public PhaseI getCurrentPhase();

	public void setNextPhase();

	public void setPhase(String name);
	
}
