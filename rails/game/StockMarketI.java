/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockMarketI.java,v 1.9 2009/10/09 20:20:34 evos Exp $ */
package rails.game;

import java.util.List;

public interface StockMarketI extends ConfigurableComponentI {

    /**
     * This is the name by which the CompanyManager should be registered with
     * the ComponentManager.
     */
    static final String COMPONENT_NAME = "StockMarket";

    public StockSpaceI[][] getStockChart();

    public StockSpaceI getStockSpace(int row, int col);

    public StockSpaceI getStockSpace(String name);

    public void start(PublicCompanyI company, StockSpaceI price);

    public void payOut(PublicCompanyI company);

    public void withhold(PublicCompanyI company);

    public void sell(PublicCompanyI company, int numberOfShares);

    public void soldOut(PublicCompanyI company);

    public void moveUp(PublicCompanyI company);
    
    public void close (PublicCompanyI company);

    public int getNumberOfColumns();

    public int getNumberOfRows();

    public List<StockSpaceI> getStartSpaces();

    public int[] getStartPrices();

    public StockSpaceI getStartSpace(int price);

    public boolean isGameOver();

    public void processMove(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to);

}
