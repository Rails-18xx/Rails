/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CompanyTypeI.java,v 1.1 2005/03/20 23:52:23 wakko666 Exp $ 
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

	/**
	 * @return
	 */
	public int getAllClosePhase();

	/**
	 * @return
	 */
	public String getAuctionType();

	/**
	 * @return
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
	 * @return
	 */
	public String getClassName();

	public void releaseDomElement ();
	public Element getDomElement ();
}