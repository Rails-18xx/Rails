/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PhaseI.java,v 1.3 2007/10/05 22:02:27 evos Exp $ */
package rails.game;

import java.util.Map;

public interface PhaseI extends ConfigurableComponentI
{
	public boolean isTileColourAllowed(String tileColour);

	public Map<String, Integer> getTileColours();

	public int getIndex();

	public String getName();

	public boolean doPrivatesClose();

	public boolean isPrivateSellingAllowed();

	public int getNumberOfOperatingRounds();

    public int getOffBoardRevenueStep();
}
