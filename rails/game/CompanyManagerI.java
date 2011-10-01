package rails.game;

import java.util.List;

import rails.common.parser.ConfigurableComponentI;

/**
 * Interface for CompanyManager objects. A company manager is a factory which
 * vends Company objects.
 */
public interface CompanyManagerI extends ConfigurableComponentI {

    /**
     * This is the name by which the CompanyManager should be registered with
     * the ComponentManager.
     */
    static final String COMPONENT_NAME = "CompanyManager";

    /**
     * Returns the Private Company identified by the supplied name.
     *
     * @param name the name of the company sought
     * @return the Private Company with the supplied name
     */
    PrivateCompany getPrivateCompany(String name);

    /**
     * Returns the Public Company identified by the supplied name.
     *
     * @param name the name of the company sought
     * @return the Public Company with the supplied name
     */
    PublicCompany getPublicCompany(String name);

    /**
     * Gives a list of all the registered Private Companies.
     *
     * @return a list of all the registered Private Companies
     */
    List<PrivateCompany> getAllPrivateCompanies();

    /**
     * Gives a list of all the registered Private Companies.
     *
     * @return a list of all the registered Private Companies
     */
    List<PublicCompany> getAllPublicCompanies();

    /**
     * Find a company by type and name
     *
     * @param type The name of the CompanyType
     * @param name The name of the Company
     * @return The company object, or null if not found.
     */
    public Company getCompany(String type, String name);

    public String checkAlias (String alias);
    public String checkAliasInCertId (String certId);

    public List<CompanyTypeI> getCompanyTypes();

    public void closeAllPrivates();

    public List<PrivateCompany> getPrivatesOwnedByPlayers();

    public StartPacket getStartPacket (int index);
    public StartPacket getStartPacket (String name);

}
