/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockMarketI.java,v 1.2 2007/10/05 22:02:27 evos Exp $ */
package rails.game;

import java.util.List;
import org.w3c.dom.Element;

public interface StockMarketI
{

	/**
	 * This is the name by which the CompanyManager should be registered with
	 * the ComponentManager.
	 */
	static final String COMPONENT_NAME = "StockMarket";

	/**
	 * Instructs the component to configure itself from the provided XML
	 * element.
	 * 
	 * @param el
	 *            the XML element containing the configuration
	 * @throws ConfigurationException
	 */
	void configureFromXML(Element el) throws ConfigurationException;

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
