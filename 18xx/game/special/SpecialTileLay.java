/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialTileLay.java,v 1.1 2005/11/24 22:42:40 evos Exp $
 * 
 * Created on 24-Nov-2005
 * Change Log:
 */
package game.special;

import game.*;

import org.w3c.dom.*;
import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class SpecialTileLay extends SpecialORProperty {
    
    String locationCode = null;
    MapHex location = null;
    boolean extra = false;
    boolean costApplies = true;
    
    public void configureFromXML (Element element) throws ConfigurationException {
        
        Element stlEl = (Element) element.getElementsByTagName("SpecialTileLay").item(0);
        if (stlEl == null) throw new ConfigurationException ("SpecialTileLay element missing");
        
        NamedNodeMap nnp = stlEl.getAttributes();
        locationCode = XmlUtils.extractStringAttribute(nnp, "location");
        if (!XmlUtils.hasValue(locationCode))
            throw new ConfigurationException ("SpecialTileLay: location missing");
        location = MapManager.getInstance().getHex (locationCode);
        if (location == null)
            throw new ConfigurationException ("Location "+locationCode+" does not exist");
         
        extra = XmlUtils.extractBooleanAttribute(nnp, "extra", extra);
        costApplies = XmlUtils.extractBooleanAttribute(nnp, "costApplies", costApplies);
        closingValue = XmlUtils.extractIntegerAttribute(nnp, "closingValue", closingValue);
    }

}
