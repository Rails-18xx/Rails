package rails.game;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import rails.util.XmlUtils;


public class PhaseManager implements PhaseManagerI, ConfigurableComponentI
{

	protected static PhaseManagerI instance = null;
	protected static ArrayList phaseList;
	protected static HashMap phaseMap;

	protected static int numberOfPhases = 0;
	protected static int currentIndex = 0;

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
		GameManager.setCurrentPhase((PhaseI) phaseList.get(0));

	}

	public PhaseI getCurrentPhase()
	{
		return (PhaseI) phaseList.get(currentIndex);
	}

	public int getCurrentPhaseIndex()
	{
		return currentIndex;
	}

	public void setNextPhase()
	{
		if (currentIndex < numberOfPhases - 1)
			++currentIndex;
	}

	public void setPhase(String name)
	{
		PhaseI nextPhase = (PhaseI) phaseMap.get(name);
		if (nextPhase != null)
		{
			currentIndex = nextPhase.getIndex();
			GameManager.setCurrentPhase(nextPhase);
		}
	}
	
	public static PhaseI getPhaseNyName (String name) {
	    return (PhaseI) phaseMap.get(name);
	}

}
