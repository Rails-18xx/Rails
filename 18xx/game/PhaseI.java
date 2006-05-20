package game;

import java.util.Map;

public interface PhaseI extends ConfigurableComponentI
{
	public boolean isTileColourAllowed(String tileColour);

	public Map getTileColours();

	public int getIndex();

	public String getName();

	public boolean doPrivatesClose();

	public boolean isPrivateSellingAllowed();

	public int getNumberOfOperatingRounds();

}
