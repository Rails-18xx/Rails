/**
 * 
 */
package net.sf.rails.game.specific._1844;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.CompanyType;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

/**
 * @author martin
 *
 */
public class CompanyManager_1844 extends CompanyManager {

    
    /** A List with all public companies */
    private List<HoldingCompany> lHoldingCompanies =
            new ArrayList<HoldingCompany>();

    /** A map with all public (i.e. non-private) companies by name */
    private Map<String, HoldingCompany> mHoldingCompanies =
            new HashMap<String, HoldingCompany>();

    private int numberOfHoldingCompanies = 0;

    /**
     * @param parent
     * @param id
     */
    public CompanyManager_1844(RailsRoot parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        // TODO Auto-generated method stub
        super.configureFromXML(tag);
        
    }

    @Override
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {
        for (HoldingCompany comp : lHoldingCompanies) {
            comp.finishConfiguration(root);
        }  
        super.finishConfiguration(root);
        
        
    }

    public HoldingCompany getHoldingCompany(String name) {
        return mHoldingCompanies.get(checkAlias(name));
    }
    public List<HoldingCompany> getAllHoldingCompanies() {
        return lHoldingCompanies;
    }

    @Override
    public Company getCompany(String type, String name) {
        // TODO Auto-generated method stub
        return super.getCompany(type, name);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.CompanyManager#mapCompanyType(java.util.Map, net.sf.rails.common.parser.Tag, java.lang.String, java.lang.String, net.sf.rails.game.CompanyType)
     */
    @Override
    protected void mapCompanyType(Map<String, Tag> typeTags, Tag companyTag,
            String name, String type, CompanyType cType)
            throws ConfigurationException {
        // TODO Auto-generated method stub
        try {

            //NEW//Company company = cType.createCompany(name, companyTag);
            Tag typeTag = typeTags.get(type);
            Company company = cType.createCompany(name, typeTag, companyTag);

            /* Private or public */
            if (company instanceof PrivateCompany) {
                mPrivateCompanies.put(name, (PrivateCompany) company);
                lPrivateCompanies.add((PrivateCompany) company);

            } else if (company instanceof PublicCompany) {
                ((PublicCompany)company).setIndex (numberOfPublicCompanies++);
                mPublicCompanies.put(name, (PublicCompany) company);
                lPublicCompanies.add((PublicCompany) company);
 
            } else if (company instanceof HoldingCompany) {
                ((HoldingCompany)company).setIndex (numberOfHoldingCompanies++);
                mHoldingCompanies.put(name, (HoldingCompany) company);
                lHoldingCompanies.add((HoldingCompany) company);
            }
            /* By type and name */
            if (!mCompaniesByTypeAndName.containsKey(type))
                mCompaniesByTypeAndName.put(type,
                        new HashMap<String, Company>());
            (mCompaniesByTypeAndName.get(type)).put(
                    name, company);

            String alias = company.getAlias();
            if (alias != null) createAlias (alias, name);

        } catch (Exception e) {
            throw new ConfigurationException(LocalText.getText(
                    "ClassCannotBeInstantiated", cType.getClassName()), e);
        }
    }

    public int getNumberOfHoldingCompanies() {
        return numberOfHoldingCompanies;
    }

    public void setNumberOfHoldingCompanies(int numberOfHoldingCompanies) {
        this.numberOfHoldingCompanies = numberOfHoldingCompanies;
    }
    
}
