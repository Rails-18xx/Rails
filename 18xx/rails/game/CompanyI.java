/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/CompanyI.java,v 1.8 2010/02/28 21:38:05 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.move.MoveableHolder;
import rails.game.special.SpecialPropertyI;

/**
 * To be implemented by any Company object.
 */
public interface CompanyI extends ConfigurableComponentI, MoveableHolder {

    /** The name of the XML tag used to configure a company. */
    public static final String COMPANY_ELEMENT_ID = "Company";

    /** The name of the XML attribute for the company's name. */
    public static final String COMPANY_NAME_TAG = "name";

    /** The name of the XML attribute for the company's type. */
    public static final String COMPANY_TYPE_TAG = "type";

    void init(String name, CompanyTypeI type);

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getSpecialProperties();

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties();

    /**
     * Get the Portfolio of this company, containing all privates and
     * certificates owned..
     *
     * @return The Portfolio of this company.
     */
    public Portfolio getPortfolio();

    /**
     * Returns the name of the Company
     *
     * @return the name of the Company
     */
    public String getName();

    public String getLongName();

    public String getInfoText();

    public String getAlias();

    /**
     * Returns the CompanyType of the Company
     *
     * @return the type of the Company
     */
    CompanyTypeI getType();

    /**
     * Returns the type name of the Company
     *
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
