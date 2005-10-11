/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CompanyTypeI.java,v 1.6 2005/10/11 17:35:29 wakko666 Exp $ 
 * 
 * Created 19mar2005 by Erik Vos
 * Changes:
 * 
 */
package game;

import org.w3c.dom.Element;

/**
 * The interface for StockSpaceType.
 * @author Erik Vos 
 * 
 */
public interface CompanyTypeI {

	/*--- Constants ---*/
	/** The name of the XML tag used to configure a company type. */
	public static final String ELEMENT_ID = "CompanyType";

	/** The name of the XML attribute for the company type's name. */
	public static final String NAME_TAG = "name";

	/** The name of the XML attribute for the company type's class name. */
	public static final String CLASS_TAG = "class";

	/** The name of the XML tag for the "NoCertLimit" property. */
	public static final String AUCTION_TAG = "Auction";
	
	/** The name of the XML tag for the "AllClose" tag. */
	public static final String ALL_CLOSE_TAG = "AllClose";

	public void configureFromXML(Element el) throws ConfigurationException;
	
	public CompanyI createCompany (String name, Element element) 
	throws ConfigurationException; 

	
	/**
	 * @return phase all privates close
	 */
	public int getAllClosePhase();

	/**
	 * @return auction type
	 */
	public String getAuctionType();

	/**
	 * @return name
	 */
	public String getName();

	/**
	 * @param i
	 */
	public void setAllClosePhase(int i);

	/**
	 * @param string
	 */
	public void setAuctionType(String string);

	/**
	 * @return class name
	 */
	public String getClassName();

	public void setCapitalisation (int mode);
	public void setCapitalisation (String mode);
	public int getCapitalisation ();

}