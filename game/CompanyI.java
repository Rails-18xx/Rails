/*
 * Created on 05-Mar-2005
 *
 * IG Adams
 */
package game;

import java.util.ArrayList;

/**
 * @author iadams
 *
 * To be implemented by any Company object.
 */
public interface CompanyI {

    /** The name of the XML tag used to configure a company. */
    public static final String COMPANY_ELEMENT_ID = "Company";

    /** The name of the XML attribute for the company's name. */
    public static final String COMPANY_NAME_TAG = "name";

    /** The name of the XML attribute for the company's type. */
    public static final String COMPANY_TYPE_TAG = "type";

	/** The name of the XML attribute for the company's type. */
	public static final String COMPANY_FG_COLOUR_TAG = "fgcolour";

	/** The name of the XML attribute for the company's type. */
	public static final String COMPANY_BG_COLOUR_TAG = "bgcolour";

    /**
     * Returns the name of the Company
     * @return the name of the Company
     */
    String getName();
    
    /**
     * Returns the type of the Company
     * @return the type of the Company
     */
    String getType();
    
    String getFgColour();
    
    String getBgColour();
    
	void start (StockSpaceI startPrice);

	boolean canBuyStock();
	
	boolean hasFloated();


	/**
	 * @return
	 */
	ArrayList getPortfolio();

	/**
	 * @return
	 */
	StockSpaceI getParPrice();

	/**
	 * @return
	 */
	ArrayList getTrainsOwned();

	/**
	 * @return
	 */
	int getTreasury();

	/**
	 * @param list
	 */
	void setTrainsOwned(ArrayList list);

	/**
	 * @param i
	 */
	void setTreasury(int i);

	/**
	 * @return
	 */
	StockSpaceI getCurrentPrice();
	
	/**
	 * @param price
	 */
	void setCurrentPrice(StockSpaceI price);


	/**
	 * @return
	 */
	int getNumber();
	
	/**
	 * @return
	 */
	boolean isClosed();
	/**
	 * @param b
	 */
	void setClosed(boolean b);

	/**
	 * @param string
	 */
	void setBgColour(String string);

	/**
	 * @param string
	 */
	void setFgColour(String string);

	/**
	 * @param b
	 */
	void setFloated(boolean b);


}
