/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Company.java,v 1.18 2010/05/24 11:20:42 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolder;
import rails.game.special.SpecialPropertyI;
import rails.game.state.BooleanState;
import rails.util.Tag;
import rails.util.Util;

public abstract class Company implements CompanyI, ConfigurableComponentI,
Cloneable, Comparable<Company> {

    protected String name;
    protected String longName;
    protected String alias = null; // To allow reloading files with old names after name changes
    protected CompanyTypeI type;
    protected int companyNumber; // For internal use

    /* Note: portfolio is used in two ways:
     * In private companies, it is primarily the portfolio that holds this private.
     * In public companies, it is the portfolio of this company.
     * This contradictory use needs to be disentangled. */
    protected Portfolio portfolio = null;

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
    protected BooleanState closedObject;

    // Moved here from PrivayeCOmpany on behalf of 1835
    protected List<SpecialPropertyI> specialProperties = null;

    protected static Logger log =
        Logger.getLogger(Company.class.getPackage().getName());

    public Company() {
    }

    public void init(String name, CompanyTypeI type) {
        this.name = name;
        this.type = type;
        closedObject = new BooleanState(name + "_Closed", false);
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
                SpecialPropertyI sp = null;
                try {
                    sp = (SpecialPropertyI) Class.forName(className).newInstance();
                } catch (Exception e) {
                    log.fatal ("Cannot instantiate "+className, e);
                    System.exit(-1);
                }
                sp.setCompany(this);
                if (specialProperties == null) specialProperties = new ArrayList<SpecialPropertyI>(2);
                specialProperties.add(sp);
                sp.configureFromXML(spTag);
                parentInfoText += "<br>" + sp.getInfo();
            }
        }
    }

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getSpecialProperties() {
        return specialProperties;
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null && !specialProperties.isEmpty();
    }

    /**
     * Get the Portfolio of this company, containing all privates and
     * certificates owned..
     *
     * @return The Portfolio of this company.
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * @return This company's number
     */
    public int getNumber() {
        return companyNumber;
    }

    /**
     * @return whether this company is closed
     */
    public boolean isClosed() {
        return closedObject.booleanValue();
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
    public CompanyTypeI getType() {
        return type;
    }

    /**
     * @return String for type of company (Public/Private)
     */
    public String getTypeName() {
        return type.getName();
    }

    /**
     * @return Name of company
     */
    public String getName() {
        return name;
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

    public MoveableHolder getHolder() {
        return portfolio;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Stub method implemented to comply with TokenHolderI interface. Always
     * returns false.
     *
     * Use addToken(MapHex hex) method instead.
     */
    public boolean addToken(CompanyI company, int position) {
        return false;
    }

    @Override
    public String toString() {
        return getTypeName() + ": " + getCompanyNumber() + ". " + getName()
        + " $" + this.getValue();
    }

    public boolean equals(CompanyI company) {
        if (this.companyNumber == company.getCompanyNumber()
                && this.name.equals(company.getName())
                && this.type.equals(company.getType())) return true;

        return false;
    }

    public int compareTo(Company otherCompany){
        int result;
        // compare typeNames first
        result = this.getTypeName().compareTo(otherCompany.getTypeName());
        // if same typeName then name
        if (result == 0)
            result = this.getName().compareTo(otherCompany.getName());

        return result;
    }

    public static String joinNamesWithDelimiter (List<CompanyI> companies, String delimiter) {
        StringBuilder b = new StringBuilder("");
        if (companies != null) {
            for (CompanyI company : companies) {
                if (b.length() > 0) b.append(delimiter);
                b.append(company.getName());
            }
        }
        return b.toString();
    }
}
