package rails.game.special;


import org.w3c.dom.*;

import rails.game.*;
import rails.util.Util;
import rails.util.XmlUtils;


public class SpecialTokenLay extends SpecialORProperty
{
    /** TODO This is now largely a copy of SpecialTileLay -- need be worked on? */
	String locationCode = null;
	MapHex location = null;
	boolean extra = false;
	boolean free = false;

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
		free = XmlUtils.extractBooleanAttribute(nnp,
				"free",
				free);
		closingValue = XmlUtils.extractIntegerAttribute(nnp,
				"closingValue",
				closingValue);
	}

	public boolean isExtra()
	{
		return extra;
	}

	public boolean isFree()
	{
		return free;
	}

	public MapHex getLocation()
	{
		return location;
	}
	
	public String toString() {
	    return "SpecialTokenLay comp="+privateCompany.getName()+" hex="+locationCode+" extra="+extra+" cost="+free;
	}
}
