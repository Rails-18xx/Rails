/*
 * Created on 05-Mar-2005
 *
 *IG Adams
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
        NodeList children = el.getElementsByTagName(CompanyI.COMPANY_ELEMENT_ID);
        for (int i = 0; i<children.getLength(); i++){
            Element compElement = (Element) children.item(i);
            NamedNodeMap nnp = compElement.getAttributes();

            //Extract the attributes of the Component
            String name = XmlUtils.extractStringAttribute(nnp, CompanyI.COMPANY_NAME_TAG);
            if (name == null) {  throw new ConfigurationException("Unnamed company found."); }
            String type = XmlUtils.extractStringAttribute(nnp, CompanyI.COMPANY_TYPE_TAG);
            if (type == null) {  throw new ConfigurationException("Company " + name + " has no type defined."); }
			String fgColour = XmlUtils.extractStringAttribute(nnp, CompanyI.COMPANY_FG_COLOUR_TAG);
			String bgColour = XmlUtils.extractStringAttribute(nnp, CompanyI.COMPANY_BG_COLOUR_TAG);
            
            if (mCompanies.get(name)!=null){
                throw new ConfigurationException("Company " + name + " configured twice");
            }

            CompanyI company = new Company(name, type, fgColour, bgColour);
            mCompanies.put(name, company);
        }
    }

    /**
     * @see game.CompanyManagerI#getCompany(java.lang.String)
     */
    public CompanyI getCompany(String name) {
        return (CompanyI)mCompanies.get(name);
    }

    /**
     * @see game.CompanyManagerI#getAllNames()
     */
    public List getAllNames() {
        return new ArrayList(mCompanies.keySet());
    }

    /**
     * @see game.CompanyManagerI#getAllCompanies()
     */
    public List getAllCompanies() {
        return new ArrayList(mCompanies.entrySet());
    }

    private Map mCompanies = new HashMap();
}
