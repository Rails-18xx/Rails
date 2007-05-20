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
	protected static ArrayList phaseList;
	protected static HashMap phaseMap;

	protected static int numberOfPhases = 0;
	//protected static int currentIndex = 0;
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
		phaseList = new ArrayList();
		phaseMap = new HashMap();
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
		return (PhaseI) currentPhase.getState();
	}

	public int getCurrentPhaseIndex()
	{
		return getCurrentPhase().getIndex();
	}

	//public void setNextPhase()
	//{
	//	if (currentIndex < numberOfPhases - 1)
	//		++currentIndex;
	//}

	public void setPhase(String name)
	{
		PhaseI phase = (PhaseI) phaseMap.get(name);
		setPhase (phase);
	}
	
	protected void setPhase (PhaseI phase) {
		if (phase != null)
		{
			currentPhase.set (phase);
			GameManager.initialiseNewPhase(phase);
		}
	}
	
	public static PhaseI getPhaseNyName (String name) {
	    return (PhaseI) phaseMap.get(name);
	}

}
