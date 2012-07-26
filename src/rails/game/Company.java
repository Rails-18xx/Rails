package rails.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import rails.common.parser.ConfigurableComponent;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.special.SpecialProperty;
import rails.game.state.BooleanState;
import rails.game.state.AbstractItem;
import rails.game.state.Item;
import rails.game.state.Owner;
import rails.game.state.PortfolioSet;
import rails.util.Util;

public abstract class Company extends AbstractItem implements Owner, ConfigurableComponent,
Cloneable, Comparable<Company> {

    /** The name of the XML tag used to configure a company. */
    public static final String COMPANY_ELEMENT_ID = "Company";

    /** The name of the XML attribute for the company's name. */
    public static final String COMPANY_NAME_TAG = "name";

    /** The name of the XML attribute for the company's type. */
    public static final String COMPANY_TYPE_TAG = "type";
    
    protected String longName;
    protected String alias = null; // To allow reloading files with old names after name changes
    protected CompanyType type;
    protected int companyNumber; // For internal use

    protected String infoText = "";
    protected String parentInfoText = "";

    /**
     * The value per certificate at the end of the rails.game. Default 0 (for
     * privates).
     */
    protected int value = 0;
    /**
     * Twice the amount each certificate counts against the limit (this way we
     * take care for certs that count for 0.5)
     */
    protected int certLimitCount = 2;

    /** Closed state */
    protected final BooleanState closedObject = BooleanState.create(this, "closed", false);

    // Moved here from PrivayeCOmpany on behalf of 1835
    protected final PortfolioSet<SpecialProperty> specialProperties = 
            PortfolioSet.create(this, "specialProperties", SpecialProperty.class);

    protected static Logger log =
        LoggerFactory.getLogger(Company.class.getPackage().getName());

    protected Company(Item parent, String id) {
        super(parent, id);
    }
    
    public void initType(CompanyType type) {
        this.type = type;
    }

    /** Only to be called from subclasses */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Special properties
        Tag spsTag = tag.getChild("SpecialProperties");
        if (spsTag != null) {

            List<Tag> spTags = spsTag.getChildren("SpecialProperty");
            String className;
            for (Tag spTag : spTags) {
                className = spTag.getAttributeAsString("class");
                if (!Util.hasValue(className))
                    throw new ConfigurationException(
                    "Missing class in private special property");
                SpecialProperty sp = null;
                try {
                    sp = (SpecialProperty) Class.forName(className).newInstance();
                } catch (Exception e) {
                    log.error ("Cannot instantiate "+className, e);
                    System.exit(-1);
                }
                sp.configureFromXML(spTag);
                specialProperties.moveInto(sp);
                parentInfoText += "<br>" + sp.getInfo();
            }
        }
    }

    /**
     * @return Set of all special properties we have.
     */
    public ImmutableSet<SpecialProperty> getSpecialProperties() {
        return specialProperties.items();
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null && !specialProperties.isEmpty();
    }

    public boolean hasPortfolio() {
        return true;
    }

    
    /**
     * 
     * @return This company's number
     */
    public int getNumber() {
        return companyNumber;
    }

    /**
     * @return whether this company is closed
     */
    public boolean isClosed() {
        return closedObject.value();
    }

    /**
     * Close this company.
     */
    public void setClosed() {
        closedObject.set(true);
    }

    /**
     * @return Type of company (Public/Private)
     */
    public CompanyType getType() {
        return type;
    }

    /**
     * @return String for type of company (Public/Private)
     */
    public String getTypeName() {
        return type.getId();
    }

    public String getLongName() {
        return longName;
    }

    public String getAlias() {
        return alias;
    }

    public String getInfoText(){
        return infoText;
    }

    /**
     * @return
     */
    public int getCertLimitCount() {
        return certLimitCount;
    }

    /**
     * @return This company's number
     */
    public int getCompanyNumber() {
        return companyNumber;
    }

    /**
     * @return Value of this company
     */
    public int getValue() {
        return value;
    }

    /**
     * @param i
     */
    public void setCertLimitCount(int i) {
        certLimitCount = i;
    }

    /**
     * @param i
     */
    public void setValue(int i) {
        value = i;
    }

    // TODO: Check if this is still required, moved to subclasses
/*    public Portfolio  getHolder() {
        return portfolio;
    }
*/

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Stub method implemented to comply with HolderModel<Token>I interface. Always
     * returns false.
     *
     * Use addToken(MapHex hex) method instead.
     */
    public boolean addToken(Company company, int position) {
        return false;
    }

    @Override
    public String toString() {
        return getTypeName() + ": " + getCompanyNumber() + ". " + getId()
        + " $" + this.getValue();
    }

    public boolean equals(Company company) {
        if (this.companyNumber == company.getCompanyNumber()
                && this.getId().equals(company.getId())
                && this.type.equals(company.getType())) return true;

        return false;
    }

    public int compareTo(Company otherCompany){
        int result;
        // compare typeNames first
        result = this.getTypeName().compareTo(otherCompany.getTypeName());
        // if same typeName then name
        if (result == 0)
            result = this.getId().compareTo(otherCompany.getId());

        return result;
    }

    public static String joinNamesWithDelimiter (List<Company> companies, String delimiter) {
        StringBuilder b = new StringBuilder("");
        if (companies != null) {
            for (Company company : companies) {
                if (b.length() > 0) b.append(delimiter);
                b.append(company.getId());
            }
        }
        return b.toString();
    }
}
