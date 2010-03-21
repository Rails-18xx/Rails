/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PhaseI.java,v 1.9 2010/03/21 17:43:50 evos Exp $ */
package rails.game;

import java.util.Map;

public interface PhaseI extends ConfigurableComponentI {
    public boolean isTileColourAllowed(String tileColour);

    /** Called when a phase gets activated */
    public void activate();

    public Map<String, Integer> getTileColours();

    public int getIndex();

    public String getName();

    public boolean doPrivatesClose();
    public void addObjectToClose(Closeable object);
    public String getInfo();

    public boolean isPrivateSellingAllowed();
    
    public int getPrivatesRevenueStep(); // sfy 1889

    public boolean isTrainTradingAllowed();

    public boolean canBuyMoreTrainsPerTurn();

    public boolean canBuyMoreTrainsPerTypePerTurn();

    public boolean isLoanTakingAllowed();
    
    public int getNumberOfOperatingRounds();

    public int getOffBoardRevenueStep();

    public String getParameterAsString (String key);
    public int getParameterAsInteger (String key);
}
