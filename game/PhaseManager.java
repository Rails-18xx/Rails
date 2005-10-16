/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PhaseManager.java,v 1.1 2005/10/16 15:02:10 evos Exp $
 * 
 * Created on 16-Oct-2005
 * Change Log:
 */
package game;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class PhaseManager implements PhaseManagerI, ConfigurableComponentI {
    
    protected static PhaseManagerI instance = null;
    protected static ArrayList phaseList;
    protected static HashMap phaseMap;
    
    protected static int numberOfPhases = 0;
    protected static int currentIndex = 0;
    
    public PhaseManager () {
        
        instance = this;
    }
    
    public static PhaseManagerI getInstance() {
        return instance;
    }
    
	public void configureFromXML(Element el) throws ConfigurationException {
	    
	    /* Phase class name is now fixed but can be made configurable, if needed. */
		NodeList phases = el.getElementsByTagName("Phase");
		numberOfPhases = phases.getLength();
		phaseList = new ArrayList();
		phaseMap = new HashMap();
		Phase phase;
		Element pe;
		String name;

		for (int i = 0; i < phases.getLength(); i++) {
		    pe = (Element) phases.item(i);
		    NamedNodeMap phaseAttr = pe.getAttributes();
		    name = XmlUtils.extractStringAttribute (phaseAttr, "name", ""+(i+1)); 
		    phase = new Phase (i, name);
		    phaseList.add(phase);
		    phaseMap.put(name, phase);
		    phase.configureFromXML(pe);
		}

	}
	
	public PhaseI getCurrentPhase () {
	    return (PhaseI) phaseList.get(currentIndex);
	}
	
	public int getCurrentPhaseIndex () {
	    return currentIndex;
	}
	
	public void setNextPhase () {
	    if (currentIndex < numberOfPhases - 1) ++currentIndex;
	}
	
	public void setPhase (String name) {
	    PhaseI nextPhase = (PhaseI) phaseMap.get(name);
	    if (nextPhase != null) {
	        currentIndex = nextPhase.getIndex();
	    }
	}


}
