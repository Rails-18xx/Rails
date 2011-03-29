/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompanyI.java,v 1.12 2010/05/01 16:08:13 stefanfrey Exp $ */
package rails.game;

import java.util.List;

import rails.game.move.MoveableHolder;

public interface PrivateCompanyI extends CompanyI, Certificate, MoveableHolder, Closeable {

    public static final String TYPE_TAG = "Private";
    public static final String REVENUE = "revenue";
   
    //used by getUpperPrice and getLowerPrice to signal no limit
    public static final int NO_PRICE_LIMIT = -1;    
    
    /**
     * @return
     */
    public int getPrivateNumber();

    /**
     * @return
     */
    public int getBasePrice();

    /**
     * @return
     */
    // sfy 1889: changed to IntegerArray
    public int[] getRevenue();
    public int getRevenueByPhase(PhaseI phase);

    public List<MapHex> getBlockedHexes();

    public void setHolder(Portfolio portfolio);

    // sfy 1889: check if closeable
    public boolean isCloseable();
    public List<String> getPreventClosingConditions();
    
    // Methods related to closure when special properties are exercised.
    public boolean closesIfAllExercised();
    public boolean closesIfAnyExercised();
    public boolean closesAtEndOfTurn();
    public boolean closesManually();
    public void checkClosingIfExercised(boolean endOfOR);
    

    /**
     * @return Returns the upperPrice that the company can be sold in for.
     */
    public int getUpperPrice();
    public int getUpperPrice(boolean saleToPlayer);
    
    /**
     * @return Returns the lowerPrice that the company can be sold in for.
     */    
    public int getLowerPrice();
    public int getLowerPrice(boolean saleToPlayer);
    
    /**
     * @return Returns whether or not the company can be bought by a company
     */    
    public boolean tradeableToCompany();   
    
    /**
     * @return Returns whether or not the company can be bought by a player (from another player)
     */
    public boolean tradeableToPlayer();
    
}
