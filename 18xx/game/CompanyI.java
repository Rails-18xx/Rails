/*
 * Created on 05-Mar-2005
 *
 * IG Adams
 */
package game;

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

	void init (String name, CompanyTypeI type);

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
	 * @return
	 */
	public String getTypeName();
	/**
	 * @return
	 */
	int getValue();


	/**
	 * @param i
	 */
	void setValue(int i);

	/**
	 * @return
	 */
	int getCompanyNumber();
	
	/**
	 * @return
	 */
	boolean isClosed();
	/**
	 * @param b
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


}
