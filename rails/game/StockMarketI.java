/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockMarketI.java,v 1.4 2007/12/21 21:18:12 evos Exp $ */
package rails.game;

import java.util.List;

public interface StockMarketI
{

	/**
	 * This is the name by which the CompanyManager should be registered with
	 * the ComponentManager.
	 */
	static final String COMPONENT_NAME = "StockMarket";

	public void init();

	public StockSpace[][] getStockChart();

	public StockSpace getStockSpace(int row, int col);

	public StockSpace getStockSpace(String name);

	public void start (PublicCompanyI company, StockSpaceI price);
	
	public void payOut(PublicCompanyI company);

	public void withhold(PublicCompanyI company);

	public void sell(PublicCompanyI company, int numberOfShares);

	public void soldOut(PublicCompanyI company);

	public int getNumberOfColumns();

	public int getNumberOfRows();

	public List getStartSpaces();

	public int[] getStartPrices();

    public StockSpaceI getStartSpace(int price);

	public boolean isGameOver();

	public void processMove(PublicCompanyI company, StockSpaceI from,
			StockSpaceI to);

}
