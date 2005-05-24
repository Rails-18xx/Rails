/*
 * Created on 12-Mar-2005
 */
package game;

import java.util.List;

import org.w3c.dom.Element;

/**
 * @author Erik
 */
public interface StockMarketI {

	/** This is the name by which the CompanyManager should be registered with the ComponentManager. */
	static final String COMPONENT_NAME = "StockMarket";

	/**
	 * Instructs the component to configure itself from the provided XML element.
	 * 
	 * @param el the XML element containing the configuration
	 * @throws ConfigurationException
	 */
	void configureFromXML(Element el) throws ConfigurationException;

    public void init ();

	public StockSpace[][] getStockChart();
	public StockSpace getStockSpace (int row, int col);
	public StockSpace getStockSpace (String name);
	
	public void payOut (PublicCompanyI company);
	public void withhold (PublicCompanyI company);
	public void sell (PublicCompanyI company, int numberOfShares);
	public void soldOut (PublicCompanyI company);
	
	public int getNumberOfColumns();
	public int getNumberOfRows();

	public List getStartSpaces();
	public StockSpaceI getStartSpace (int price);
	public boolean isGameOver();

}