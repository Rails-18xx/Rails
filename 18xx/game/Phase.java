/*
 * Created on Sep 7, 2005
 * Author: Erik Vos
 */
package game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.XmlUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * @author Erik Vos
 */
public class Phase implements PhaseI {
    
    protected int index;
    protected String name;
    protected HashMap tileColours;
    protected boolean privateSellingAllowed = false;
    protected boolean privatesClose = false;
    protected int numberOfOperatingRounds = 1;
    
    protected static boolean previousPrivateSellingAllowed = false;
    protected static int previousNumberOfOperatingRounds = 1;
    
    public Phase (int index, String name) {
        this.index = index;
        this.name = name;
    }
    
	public void configureFromXML(Element el) throws ConfigurationException {
	    
	    NamedNodeMap attributes;
	    String colourList;
	    String[] colourArray = new String[0];
	    tileColours = new HashMap();
	    
	    // Allowed tile colours
        NodeList nl = el.getElementsByTagName("Tiles");
        if (nl != null && nl.getLength() > 0) {
            attributes = nl.item(0).getAttributes();
            colourList = XmlUtils.extractStringAttribute(attributes, "colour");
            if (colourList != null) colourArray = colourList.split(",");
            for (int i=0; i<colourArray.length; i++) {
                tileColours.put(colourArray[i], null);
            }
        }

        // Private-related properties
        nl = el.getElementsByTagName("Privates");
        if (nl != null && nl.getLength() > 0) {
            attributes = nl.item(0).getAttributes();
            privateSellingAllowed = previousPrivateSellingAllowed = 
                XmlUtils.extractBooleanAttribute
            		(attributes, "sellingAllowed", previousPrivateSellingAllowed);
            privatesClose = XmlUtils.extractBooleanAttribute
        		(attributes, "close", false);
        }

        // Operating rounds
        nl = el.getElementsByTagName("OperatingRounds");
        if (nl != null && nl.getLength() > 0) {
            attributes = nl.item(0).getAttributes();
            numberOfOperatingRounds = previousNumberOfOperatingRounds =
                XmlUtils.extractIntegerAttribute
            		(attributes, "number", previousNumberOfOperatingRounds);
        }
}
	
	public boolean isTileColourAllowed (String tileColour) {
	    return tileColours.containsKey(tileColour);
	}
	
	public Map getTileColours () {
	    return tileColours;
	}
	
	public int getIndex () {
	    return index;
	}
	
	public String getName () {
	    return name;
	}

    /**
     * @return Returns the privatesClose.
     */
    public boolean doPrivatesClose() {
        return privatesClose;
    }
    /**
     * @return Returns the privateSellingAllowed.
     */
    public boolean isPrivateSellingAllowed() {
        return privateSellingAllowed;
    }
    
    public int getNumberOfOperatingRounds () {
        return numberOfOperatingRounds;
    }
}
