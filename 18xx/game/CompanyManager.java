/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CompanyManager.java,v 1.3 2005/03/20 23:46:57 wakko666 Exp $
 * 
 * Created on 05-Mar-2005 IG Adams
 * Changes:
 * 19mar2005 Erik Vos: added CompanyType and split public/private companies.
 */
package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import util.XmlUtils;

/**
 * @author iadams
 *
 * First inmplementation of CompanyManager.
 */
public class CompanyManager implements CompanyManagerI, ConfigurableComponentI {

	/** A map with all company types, by type name */
	 private Map mCompanyTypes = new HashMap();
    
	/** A List with all companies */
	private List lCompanies = new ArrayList();
	/** A List with all private companies */
	private List lPrivateCompanies = new ArrayList();
	/** A List with all public companies */
	private List lPublicCompanies = new ArrayList();
	/** A map with all private companies by name */
	private Map mPrivateCompanies = new HashMap();
	/** A map with all public (i.e. non-private) companies by name */
	private Map mPublicCompanies = new HashMap();
	/** A map of all type names to lists of companies of that type */
	private Map mCompaniesByType = new HashMap();
	
	 /* NOTES:
	  * 1. we don't have a map over all companies, because some games
	  * have duplicate names, e.g. B&O in 1830.
	  * 2. we have both a map and a list of private/public companies 
	  * to preserve configuration sequence while allowing direct access.
	  *  */
    /**
     * No-args constructor. 
     */
    public CompanyManager(){
        //Nothing to do here, everything happens when configured.
    }

    /**
     * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Element el) throws ConfigurationException {
    	
		/* Read and configure the company types */
		NodeList types = el.getElementsByTagName(CompanyTypeI.ELEMENT_ID);
		for (int i = 0; i<types.getLength(); i++){
			Element compElement = (Element) types.item(i);
			NamedNodeMap nnp = compElement.getAttributes();

			//Extract the attributes of the Component
			String name = XmlUtils.extractStringAttribute(nnp, CompanyTypeI.NAME_TAG);
			if (name == null) {  throw new ConfigurationException("Unnamed company type found."); }
			String className = XmlUtils.extractStringAttribute(nnp, CompanyTypeI.CLASS_TAG);
			if (className == null) {  throw new ConfigurationException("Company type " + name + " has no class defined."); }
           
			if (mCompanyTypes.get(name)!=null){
				throw new ConfigurationException("Company type " + name + " configured twice");
			}

			CompanyTypeI type = new CompanyType(name, className, el);
			mCompanyTypes.put(name, type);
		}
		
   	/* Read and configure the companies */
        NodeList children = el.getElementsByTagName(CompanyI.COMPANY_ELEMENT_ID);
        for (int i = 0; i<children.getLength(); i++){
            Element compElement = (Element) children.item(i);
            NamedNodeMap nnp = compElement.getAttributes();

            //Extract the attributes of the Component
            String name = XmlUtils.extractStringAttribute(nnp, CompanyI.COMPANY_NAME_TAG);
            if (name == null) {  throw new ConfigurationException("Unnamed company found."); }
            String type = XmlUtils.extractStringAttribute(nnp, CompanyI.COMPANY_TYPE_TAG);
            if (type == null) {  throw new ConfigurationException("Company " + name + " has no type defined."); }
           
            CompanyTypeI cType = (CompanyTypeI) mCompanyTypes.get(type);
			if (cType == null) {  throw new ConfigurationException("Company " + name + " has undefined type "+cType); }
			
			try{
				String className = cType.getClassName();
				Company company = (Company) Class.forName(className).newInstance();
				company.init (name, cType);
				company.configureFromXML(compElement);
				
				/* Add company to the various lists */
				lCompanies.add (company);
				/* Private or public */
				if (company instanceof PrivateCompanyI) {
					mPrivateCompanies.put (name, company);
					lPrivateCompanies.add (company);
				} else if (company instanceof PublicCompanyI) {
					mPublicCompanies.put (name, company);
					lPublicCompanies.add (company);
				}
				/* By type */
				if (!mCompaniesByType.containsKey(type)) mCompaniesByType.put (type, new ArrayList());
				((List)mCompaniesByType.get(type)).add(company);
				
			} catch (Exception e) {
				throw new ConfigurationException ("Class "+cType.getClassName()+" cannot be instantiated", e);
			}
        }
    }

    /**
     * @see game.CompanyManagerI#getCompany(java.lang.String)
     * 
     */
    public PrivateCompanyI getPrivateCompany(String name) {
        return (PrivateCompanyI) mPrivateCompanies.get(name);
    }

	public PublicCompanyI getPublicCompany(String name) {
		return (PublicCompanyI) mPublicCompanies.get(name);
	}

    /**
     * @see game.CompanyManagerI#getAllNames()
     */
    public List getAllPrivateNames() {
        return new ArrayList(mPrivateCompanies.keySet());
    }

	public List getAllPublicNames() {
		return new ArrayList(mPublicCompanies.keySet());
	}

    /**
     * @see game.CompanyManagerI#getAllCompanies()
     */
	public List getAllCompanies() {
		return (List) lCompanies;
	}

    public List getAllPrivateCompanies() {
        return (List) lPrivateCompanies;
    }

	public List getAllPublicCompanies() {
		return (List) lPublicCompanies;
	}
	
	public List getCompaniesByType (String type) {
		return (List) mCompaniesByType.get(type);
	}

}
