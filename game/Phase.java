/*
 * Created on Sep 7, 2005
 * Author: Erik Vos
 */
package game;

import java.util.HashMap;
import java.util.List;
import util.XmlUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * @author Erik Vos
 */
public class Phase implements PhaseI {
    
    int index;
    String name;
    HashMap tileColours;
    
    public Phase (int index, String name) {
        this.index = index;
        this.name = name;
    }
    
	public void configureFromXML(Element el) throws ConfigurationException {
	    
	    NamedNodeMap tileAttr;
	    String colourList;
	    String[] colourArray = new String[0];
	    tileColours = new HashMap();
	    
        NodeList nl = el.getElementsByTagName("Tiles");
        if (nl != null && nl.getLength() > 0) {
            tileAttr = nl.item(0).getAttributes();
            colourList = XmlUtils.extractStringAttribute(tileAttr, "colour");
            if (colourList != null) colourArray = colourList.split(",");
            for (int i=0; i<colourArray.length; i++) {
                tileColours.put(colourArray[i], null);
            }
        }
	}
	
	public boolean isTileColourAllowed (String tileColour) {
	    return tileColours.containsKey(tileColour);
	}
	
	public int getIndex () {
	    return index;
	}
	
	public String getName () {
	    return name;
	}

	/* (non-Javadoc)
	 * @see game.PhaseI#getAvailableTrainTypes()
	 */
    /* NOT USED, IS NOW HANDLED BY TRAINMANAGER */
	public List getAvailableTrainTypes() {
		// TODO Auto-generated method stub
		return null;
	}

}
