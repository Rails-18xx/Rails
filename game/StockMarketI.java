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
	
	StockSpace[][] getStockChart();
	StockSpace getStockSpace (int row, int col);
	
	void payOut (CompanyI company);
	void withhold (CompanyI company);
	void sell (CompanyI company, int numberOfShares);
	void soldOut (CompanyI company);
	
	int getNumberOfColumns();
	int getNumberOfRows();

	List getStartSpaces();
	boolean isGameOver();

}