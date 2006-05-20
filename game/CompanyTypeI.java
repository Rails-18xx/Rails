package game;

import org.w3c.dom.Element;

/**
 * The interface for StockSpaceType.
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
	 * @return name
	 */
	public String getName();

	/**
	 * @return class name
	 */
	public String getClassName();

	public void setCapitalisation (int mode);
	public void setCapitalisation (String mode);
	public int getCapitalisation ();

}