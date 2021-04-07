package net.sf.rails.game;

import java.util.Comparator;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.game.special.SpecialProperty;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;


public interface Company extends RailsOwner, Configurable, Cloneable {

    /** The name of the XML tag used to configure a company. */
    String COMPANY_ELEMENT_ID = "Company";

    /** The name of the XML attribute for the company's name. */
    String COMPANY_NAME_TAG = "name";

    /** The name of the XML attribute for the company's type. */
    String COMPANY_TYPE_TAG = "type";

    /**
     * A Comparator for Companies
     */
    Comparator<Company> COMPANY_COMPARATOR = new Comparator<Company>() {
        @Override
        public int compare(Company c0, Company c1) {
            return ComparisonChain.start()
                    .compare(c0.getType().getId(), c1.getType().getId())
                    .compare(c0.getId(), c1.getId())
                    .result();
        }
    };

    void initType(CompanyType type);

    /**
     * @return Type of company (Public/Private)
     */
    CompanyType getType();

    /**
     * @return whether this company is closed
     */
    boolean isClosed();

    /**
     * Close this company.
     */
    void setClosed();

    default String getLongName() {return "";}

    default String getAlias() {return "";}

    default String getInfoText() {return "";}

    // Since 1835 required for both private and public companies
    /**
     * @return Set of all special properties we have.
     */
    ImmutableSet<SpecialProperty> getSpecialProperties();

}
