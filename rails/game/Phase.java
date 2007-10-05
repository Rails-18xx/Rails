/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Phase.java,v 1.4 2007/10/05 22:02:28 evos Exp $ */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import rails.util.XmlUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class Phase implements PhaseI {
    
    protected int index;
    protected String name;
    protected HashMap<String, Integer> tileColours;
    protected boolean privateSellingAllowed = false;
    protected boolean privatesClose = false;
    protected int numberOfOperatingRounds = 1;
    protected int offBoardRevenueStep = 1;
    
    protected static boolean previousPrivateSellingAllowed = false;
    protected static int previousNumberOfOperatingRounds = 1;
    protected static String previousTileColours = "";
    protected static int previousOffBoardRevenueStep = 1;
    
    public Phase (int index, String name) {
        this.index = index;
        this.name = name;
    }
    
	public void configureFromXML(Element el) throws ConfigurationException {
	    
	    NamedNodeMap attributes;
	    String colourList;
	    String[] colourArray = new String[0];
	    tileColours = new HashMap<String, Integer>();
	    
	    // Allowed tile colours
        NodeList nl = el.getElementsByTagName("Tiles");
        if (nl != null && nl.getLength() > 0) {
            attributes = nl.item(0).getAttributes();
            colourList = XmlUtils.extractStringAttribute(attributes, "colour", previousTileColours);
        } else {
            colourList = previousTileColours;
        }
        if (colourList != null) colourArray = colourList.split(",");
        for (int i=0; i<colourArray.length; i++) {
            tileColours.put(colourArray[i], null);
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
        } else {
            privateSellingAllowed = previousPrivateSellingAllowed;
        }

        // Operating rounds
        nl = el.getElementsByTagName("OperatingRounds");
        if (nl != null && nl.getLength() > 0) {
            attributes = nl.item(0).getAttributes();
            numberOfOperatingRounds = previousNumberOfOperatingRounds =
                XmlUtils.extractIntegerAttribute
            		(attributes, "number", previousNumberOfOperatingRounds);
        } else {
            numberOfOperatingRounds = previousNumberOfOperatingRounds;
        }
        
        // Off-board revenue steps
        nl = el.getElementsByTagName("OffBoardRevenue");
        if (nl != null && nl.getLength() > 0) {
            attributes = nl.item(0).getAttributes();
            offBoardRevenueStep = previousOffBoardRevenueStep =
                XmlUtils.extractIntegerAttribute
            		(attributes, "step", previousOffBoardRevenueStep);
        } else {
            offBoardRevenueStep = previousOffBoardRevenueStep;
        }
	}
	
	public boolean isTileColourAllowed (String tileColour) {
	    return tileColours.containsKey(tileColour);
	}
	
	public Map<String, Integer> getTileColours () {
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
    
    /**
     * @return Returns the offBoardRevenueStep.
     */
    public int getOffBoardRevenueStep() {
        return offBoardRevenueStep;
    }
    
    public String toString() {
    	return name;
    }
}
