/*
 * Created on 05-Mar-2005
 *
 * IG Adams
 */
package game;

import java.util.List;

/**
 * @author iadams
 *
 * Interface for CompanyManager objects. A company manager is a factory
 * which vends Company objects.
 */
public interface CompanyManagerI {

    /** This is the name by which the CompanyManager should be registered with the ComponentManager. */
    static final String COMPONENT_NAME = "CompanyManager";

    /**
     * Returns the Company identified by the supplied name.
     * @param name the name of the company sought
     * @return the Company with the supplied name
     */
    CompanyI getCompany(String name);

    /**
     * Gives a list of Strings, the names of all registered Companies.
     * @return a List of the all the Company names
     */
    List getAllNames();

    /**
     * Gives a list of all the registered Companies.
     * @return a list of all the registered Companies
     */
    List getAllCompanies();
}
