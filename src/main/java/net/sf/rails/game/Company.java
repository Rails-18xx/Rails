package net.sf.rails.game;

import java.util.Comparator;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.game.special.SpecialProperty;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;


public interface Company extends RailsOwner, Configurable, Cloneable {

    /** The name of the XML tag used to configure a company. */
    public static final String COMPANY_ELEMENT_ID = "Company";

    /** The name of the XML attribute for the company's name. */
    public static final String COMPANY_NAME_TAG = "name";

    /** The name of the XML attribute for the company's type. */
    public static final String COMPANY_TYPE_TAG = "type";

    /**
     * A Comparator for Companies
     */
    public static final Comparator<Company> COMPANY_COMPARATOR = new Comparator<Company>() {
        @Override
        public int compare(Company c0, Company c1) {
            return ComparisonChain.start()
                    .compare(c0.getType().getId(), c1.getType().getId())
                    .compare(c0.getId(), c1.getId())
                    .result();
        }
    };

//    protected String longName;
//    protected String alias = null; // To allow reloading files with old names after name changes
//    protected CompanyType type;
//    protected int companyNumber; // For internal use
//
//    protected String infoText = "";
//    protected String parentInfoText = "";
//
//    /**
//     * The value per certificate at the end of the rails.game. Default 0 (for
//     * privates).
//     */
//    protected int value = 0;
//    /**
//     * Twice the amount each certificate counts against the limit (this way we
//     * take care for certs that count for 0.5)
//     */
//    protected int certLimitCount = 2;
//
//    /** Closed state */
//    protected final BooleanState closedObject = new BooleanState(this, "closed", false);
//
    /** Only to be called from subclasses */
//    public void configureFromXML(Tag tag) throws ConfigurationException {
//
//        // Special properties
//        Tag spsTag = tag.getChild("SpecialProperties");
//        if (spsTag != null) {
//
//            List<Tag> spTags = spsTag.getChildren("SpecialProperty");
//            String className;
//            for (Tag spTag : spTags) {
//                className = spTag.getAttributeAsString("class");
//                if (!Util.hasValue(className))
//                    throw new ConfigurationException(
//                    "Missing class in private special property");
//                String uniqueId = SpecialProperty.createUniqueId();
//                SpecialProperty sp = Configure.create(SpecialProperty.class, className, this, uniqueId);
//                sp.setOriginalCompany(this);
//                sp.configureFromXML(spTag);
//                sp.moveTo(this);
//                parentInfoText += "<br>" + sp.getInfo();
//            }
//        }
//    }

    public void initType(CompanyType type);

    /**
     * @return Type of company (Public/Private)
     */
    public CompanyType getType();

    /**
     * @return whether this company is closed
     */
    public boolean isClosed();

    /**
     * Close this company.
     */
    public void setClosed();

    public String getLongName();

    public String getAlias();

    public String getInfoText();

    // Since 1835 required for both private and public companies
    /**
     * @return Set of all special properties we have.
     */
    public abstract ImmutableSet<SpecialProperty> getSpecialProperties();

}
