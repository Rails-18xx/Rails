package rails.game;

import java.util.List;

/**
 * Interface for CompanyManager objects. A company manager is a factory which
 * vends Company objects.
 */
public interface CompanyManagerI
{

	/**
	 * This is the name by which the CompanyManager should be registered with
	 * the ComponentManager.
	 */
	static final String COMPONENT_NAME = "CompanyManager";

	/**
	 * Returns the Private Company identified by the supplied name.
	 * 
	 * @param name
	 *            the name of the company sought
	 * @return the Private Company with the supplied name
	 */
	PrivateCompanyI getPrivateCompany(String name);

	/**
	 * Returns the Public Company identified by the supplied name.
	 * 
	 * @param name
	 *            the name of the company sought
	 * @return the Public Company with the supplied name
	 */
	PublicCompanyI getPublicCompany(String name);

	/**
	 * Gives a list of Strings, the names of all registered Private Companies.
	 * 
	 * @return a List of the all the Private Company names
	 */
	List getAllPrivateNames();

	/**
	 * Gives a list of Strings, the names of all registered Public Companies.
	 * 
	 * @return a List of the all the Public Company names
	 */
	List getAllPublicNames();

	/**
	 * Gives a list of all the registered Companies.
	 * 
	 * @return a list of all the registered Companies
	 */
	List getAllCompanies();

	/**
	 * Gives a list of all the registered Private Companies.
	 * 
	 * @return a list of all the registered Private Companies
	 */
	List getAllPrivateCompanies();

	/**
	 * Gives a list of all the registered Private Companies.
	 * 
	 * @return a list of all the registered Private Companies
	 */
	List getAllPublicCompanies();

	/**
	 * Gives a list of all the registered Companies of a given type.
	 * 
	 * @param The
	 *            company type name.
	 * @return a list of all the registered Companies of a goven type.
	 */
	List getCompaniesByType(String type);

	/**
	 * Find a company by type and name
	 * 
	 * @param type
	 *            The name of the CompanyType
	 * @param name
	 *            The name of the Company
	 * @return The company object, or null if not found.
	 */
	CompanyI getCompany(String type, String name);

	/** Post XML parsing initialisations */
	public void initCompanies() throws ConfigurationException;

	   public List getCompaniesWithExcessTrains ();
	   
	   public int getBaseTokenLayCostBySequence (int index);

		public void closeAllPrivates();
		
		public List getPrivatesOwnedByPlayers ();

}
