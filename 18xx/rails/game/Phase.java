/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Phase.java,v 1.5 2007/10/07 20:14:54 evos Exp $ */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import rails.util.Tag;

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
    
	public void configureFromXML(Tag tag) throws ConfigurationException
	{
	    
	    NamedNodeMap attributes;
	    String colourList;
	    String[] colourArray = new String[0];
	    tileColours = new HashMap<String, Integer>();
	    
	    // Allowed tile colours
        Tag tilesTag = tag.getChild("Tiles");
        if (tilesTag != null) {
            colourList = tilesTag.getAttributeAsString("colour", previousTileColours);
        } else {
            colourList = previousTileColours;
        }
        if (colourList != null) colourArray = colourList.split(",");
        for (int i=0; i<colourArray.length; i++) {
            tileColours.put(colourArray[i], null);
        }

        // Private-related properties
        Tag privatesTag = tag.getChild("Privates");
        if (privatesTag != null) {
            privateSellingAllowed = previousPrivateSellingAllowed = 
            	privatesTag.getAttributeAsBoolean(
            			"sellingAllowed", previousPrivateSellingAllowed);
            privatesClose = privatesTag.getAttributeAsBoolean("close", false);
        } else {
            privateSellingAllowed = previousPrivateSellingAllowed;
        }

        // Operating rounds
        Tag orTag = tag.getChild("OperatingRounds");
        if (orTag != null) {
            numberOfOperatingRounds = previousNumberOfOperatingRounds =
            	orTag.getAttributeAsInteger(
            			"number", previousNumberOfOperatingRounds);
        } else {
            numberOfOperatingRounds = previousNumberOfOperatingRounds;
        }
        
        // Off-board revenue steps
        Tag offBoardTag = tag.getChild("OffBoardRevenue");
        if (offBoardTag != null) {
            offBoardRevenueStep = previousOffBoardRevenueStep =
            	offBoardTag.getAttributeAsInteger("step", previousOffBoardRevenueStep);
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
