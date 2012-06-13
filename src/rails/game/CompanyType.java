package rails.game;

import java.util.ArrayList;
import java.util.List;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.state.AbstractItem;
import rails.game.state.Item;

/**
 * Objects of this class represent a particular type of company, of which
 * typically multiple instances exist in a rails.game. Examples: "Private",
 * "Minor", "Major", "Mountain" etc. <p> This class contains common properties
 * of the companies of one type, and aids in configuring the companies by
 * reducing the need to repeatedly specify common properties with different
 * companies.
 */
public class CompanyType extends AbstractItem {
    
    /*--- Class attributes ---*/

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

    /*--- Instance attributes ---*/
    protected String className;
    protected int capitalisation = PublicCompany.CAPITALISE_FULL;

    protected List<Company> companies = new ArrayList<Company>();

    /**
     * The constructor.
     *
     */
    private CompanyType(String className) {
        this.className = className;
    }

    /**
    * @param name Company type name ("Private", "Public", "Minor" etc.).
    * @param className Name of the class that will instantiate this type of
    * company.
    */
    public static CompanyType create(Item parent, String id, String className) {
        CompanyType type = new CompanyType(className);
        type.init(parent, id);
        return type;
    }

    /**
     * @see rails.common.parser.ConfigurableComponent#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        //No longer needed.
    }

    public void finishConfiguration (GameManager gameManager) {

    }

    public Company createCompany(String name, Tag typeTag, Tag tag)
    throws ConfigurationException {
        Company newCompany = null;
        try {
            newCompany = (Company) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(LocalText.getText(
                    "ClassCannotBeInstantiated", className), e);
        }
        newCompany.init(this, name);
        newCompany.configureFromXML(typeTag);
        newCompany.configureFromXML(tag);
        companies.add(newCompany);
        return newCompany;
    }

    /*--- Getters and setters ---*/
    /**
     * Get the name of the class that will implement this type of company.
     *
     * @return The full class name.
     */
    public String getClassName() {
        return className;
    }

    public List<Company> getCompanies() {
		return companies;
	}

	public void setCapitalisation(int mode) {
        this.capitalisation = mode;
    }

    public void setCapitalisation(String mode) {
        if (mode.equalsIgnoreCase("full")) {
            this.capitalisation = PublicCompany.CAPITALISE_FULL;
        } else if (mode.equalsIgnoreCase("incremental")) {
            this.capitalisation = PublicCompany.CAPITALISE_INCREMENTAL;
        }
    }

    public int getCapitalisation() {
        return capitalisation;
    }

}
