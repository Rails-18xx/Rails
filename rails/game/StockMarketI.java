/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockMarketI.java,v 1.10 2010/03/10 17:26:49 stefanfrey Exp $ */
package rails.game;

import java.util.List;

import rails.common.parser.ConfigurableComponentI;

public interface StockMarketI extends ConfigurableComponentI {

    /**
     * This is the name by which the CompanyManager should be registered with
     * the ComponentManager.
     */
    static final String COMPONENT_NAME = "StockMarket";

    public StockSpaceI[][] getStockChart();

    public StockSpaceI getStockSpace(int row, int col);

    public StockSpaceI getStockSpace(String name);

    public void start(PublicCompany company, StockSpaceI price);

    public void payOut(PublicCompany company);

    public void withhold(PublicCompany company);

    public void sell(PublicCompany company, int numberOfShares);

    public void soldOut(PublicCompany company);

    public void moveUp(PublicCompany company);
    
    public void close (PublicCompany company);

    public int getNumberOfColumns();

    public int getNumberOfRows();

    public List<StockSpaceI> getStartSpaces();

    public int[] getStartPrices();

    public StockSpaceI getStartSpace(int price);

    public void processMove(PublicCompany company, StockSpaceI from,
            StockSpaceI to);

    public void processMoveToStackPosition(PublicCompany company, StockSpaceI from,
            StockSpaceI to, int toStackPosition);
}
