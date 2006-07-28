package game;

import org.w3c.dom.Element;

/**
 * To be implemented by any Company object.
 */
public interface CompanyI {

    /** The name of the XML tag used to configure a company. */
    public static final String COMPANY_ELEMENT_ID = "Company";

    /** The name of the XML attribute for the company's name. */
    public static final String COMPANY_NAME_TAG = "name";

    /** The name of the XML attribute for the company's type. */
    public static final String COMPANY_TYPE_TAG = "type";

	void init (String name, CompanyTypeI type);
	
	void configureFromXML (Element element) throws ConfigurationException;

    /**
     * Returns the name of the Company
     * @return the name of the Company
     */
    String getName();
    
    /**
     * Returns the CompanyType of the Company
     * @return the type of the Company
     */
    CompanyTypeI getType();
    

	/**
     * Returns the type name of the Company
	 * @return type name
	 */
	public String getTypeName();
	
	/**
	 * @return Company Value
	 */
	int getValue();


	/**
	 * @param Company Value
	 */
	void setValue(int i);

	/**
	 * @return Company number
	 */
	int getCompanyNumber();
	
	/**
	 * @return true if Company is closed.
	 */
	boolean isClosed();
	/**
	 * Set company to closed
	 */
	void setClosed();

	/**
	 * @return 
	 */
	public int getCertLimitCount();
	/**
	 * @param i
	 */
	public void setCertLimitCount(int i);
	
	public abstract Object clone() throws CloneNotSupportedException;

	
	// Overriding some standard methods with something more useful.
	public String toString();
	public boolean equals(CompanyI company);
}
