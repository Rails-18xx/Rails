/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PhaseManager.java,v 1.6 2007/10/05 22:02:28 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import rails.game.state.State;
import rails.util.XmlUtils;


public class PhaseManager implements PhaseManagerI, ConfigurableComponentI
{

	protected static PhaseManagerI instance = null;
	protected static ArrayList<Phase> phaseList;
	protected static HashMap<String, Phase> phaseMap;

	protected static int numberOfPhases = 0;
	protected static State currentPhase = new State ("CurrentPhase", Phase.class);

	public PhaseManager()
	{

		instance = this;
	}

	public static PhaseManagerI getInstance()
	{
		return instance;
	}

	public void configureFromXML(Element el) throws ConfigurationException
	{

		/*
		 * Phase class name is now fixed but can be made configurable, if
		 * needed.
		 */
		NodeList phases = el.getElementsByTagName("Phase");
		numberOfPhases = phases.getLength();
		phaseList = new ArrayList<Phase>();
		phaseMap = new HashMap<String, Phase>();
		Phase phase;
		Element pe;
		String name;

		for (int i = 0; i < phases.getLength(); i++)
		{
			pe = (Element) phases.item(i);
			NamedNodeMap phaseAttr = pe.getAttributes();
			name = XmlUtils.extractStringAttribute(phaseAttr, "name", ""
					+ (i + 1));
			phase = new Phase(i, name);
			phaseList.add(phase);
			phaseMap.put(name, phase);
			phase.configureFromXML(pe);
		}
		PhaseI initialPhase = (PhaseI) phaseList.get(0);
		setPhase (initialPhase);

	}

	public PhaseI getCurrentPhase()
	{
		return (PhaseI) currentPhase.getObject();
	}

	public int getCurrentPhaseIndex()
	{
		return getCurrentPhase().getIndex();
	}

	public void setPhase(String name)
	{
		setPhase (phaseMap.get(name));
	}
	
	protected void setPhase (PhaseI phase) {
		if (phase != null)
		{
			currentPhase.set (phase);
			GameManager.initialiseNewPhase(phase);
		}
	}
	
	public static PhaseI getPhaseNyName (String name) {
	    return phaseMap.get(name);
	}

}
