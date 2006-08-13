package game.special;

import game.*;

import org.w3c.dom.*;

import util.Util;
import util.XmlUtils;

public class SpecialTokenLay extends SpecialORProperty
{
    /** TODO This is now largely a copy of SpecialTileLay -- need be worked on? */
	String locationCode = null;
	MapHex location = null;
	boolean extra = false;
	boolean costApplies = true;

	public void configureFromXML(Element element) throws ConfigurationException
	{

		NodeList nl = element.getElementsByTagName("SpecialTokenLay");
		if (nl == null || nl.getLength() == 0)
		{
			throw new ConfigurationException("<SpecialTokenLay> tag missing");
		}
		Element stlEl = (Element) nl.item(0);

		NamedNodeMap nnp = stlEl.getAttributes();
		locationCode = XmlUtils.extractStringAttribute(nnp, "location");
		if (!Util.hasValue(locationCode))
			throw new ConfigurationException("SpecialTokenLay: location missing");
		location = MapManager.getInstance().getHex(locationCode);
		if (location == null)
			throw new ConfigurationException("Location " + locationCode
					+ " does not exist");

		extra = XmlUtils.extractBooleanAttribute(nnp, "extra", extra);
		costApplies = XmlUtils.extractBooleanAttribute(nnp,
				"costApplies",
				costApplies);
		closingValue = XmlUtils.extractIntegerAttribute(nnp,
				"closingValue",
				closingValue);
	}

	public boolean isExtra()
	{
		return extra;
	}

	public boolean costApplies()
	{
		return costApplies;
	}

	public MapHex getLocation()
	{
		return location;
	}
}
