package rails.game;

import java.util.*;
import org.w3c.dom.*;

import rails.util.Util;
import rails.util.XmlUtils;

/**
 * A Start Packet comprises a number of Start Items. The typical start packet
 * must be completely sold out before normal share buying can start (but there
 * are exceptions to this rule).
 */
public class StartPacket
{

	/**
	 * A Map holding all start packets of a rails.game (yes, there can be more than
	 * one, e.g. 18US).
	 */
	private static Map packets = new HashMap();

	/**
	 * The start packet name. Usually the default name "Ïnitial" is used.
	 */
	private String name; // For identification if there is more than one.
	/** The name of the class that implements the Start Round for this packet. */
	private String roundClassName;
	/** The start round variant name */
	private String variant;
	/** The start items in this packet. */
	private List items = new ArrayList();

	/** Default name */
	public static final String DEFAULT_NAME = "Initial";

	/**
	 * Constructor. Only takes the packet and class named. Actual initialisation
	 * is done in <b>configureFromXML()</b>.
	 * 
	 * @param name
	 *            The start packet name.
	 * @param roundClassName
	 *            The StartRound class name.
	 */
	StartPacket(String name, String roundClassName)
	{
		this.name = Util.hasValue(name) ? name : DEFAULT_NAME;
		this.roundClassName = roundClassName;
		this.variant = GameManager.getVariant();
		packets.put(name, this);
	}

	/**
	 * Configure the start packet from the contents of a &lt;StartPacket&gt; XML
	 * element.
	 * 
	 * @param element
	 *            The &lt;StartPacket&gt; Element object.
	 * @throws ConfigurationException
	 *             if anything goes wrong.
	 */
	public void configureFromXML(Element element) throws ConfigurationException
	{
		NodeList children = element.getElementsByTagName("Item");
		Portfolio ipo = Bank.getIpo();
		Portfolio unavailable = Bank.getUnavailable();

		for (int i = 0; i < children.getLength(); i++)
		{
			Element itemEl = (Element) children.item(i);
			NamedNodeMap nnp = itemEl.getAttributes();
			// Extract the attributes of the Start Packet Item (certificate)
			String itemName = XmlUtils.extractStringAttribute(nnp, "name");
			if (itemName == null)
				throw new ConfigurationException("No item name");
			String itemType = XmlUtils.extractStringAttribute(nnp, "type");
			if (itemType == null)
				throw new ConfigurationException("No item type");
			boolean president = Util.hasValue(XmlUtils.extractStringAttribute(nnp,
					"president",
					""));

			int basePrice = XmlUtils.extractIntegerAttribute(nnp,
					"basePrice",
					0);
			StartItem item = (new StartItem(itemName,
					itemType,
					basePrice,
					president));
			items.add(item);

			// Optional attributes
			int row = XmlUtils.extractIntegerAttribute(nnp, "row", 0);
			if (row > 0)
				item.setRow(row);
			int column = XmlUtils.extractIntegerAttribute(nnp, "column", 0);
			if (column > 0)
				item.setColumn(column);

			// Check if there is another certificate
			NodeList children2 = itemEl.getElementsByTagName("SubItem");
			if (children2.getLength() > 0)
			{
				NamedNodeMap nnp2 = ((Element) children2.item(0)).getAttributes();
				String itemName2 = XmlUtils.extractStringAttribute(nnp2, "name");
				if (itemName2 == null)
					throw new ConfigurationException("No item name");
				String itemType2 = XmlUtils.extractStringAttribute(nnp2, "type");
				if (itemType2 == null)
					throw new ConfigurationException("No item type");
				boolean president2 = Util.hasValue(XmlUtils.extractStringAttribute(nnp2,
						"president",
						""));

				item.setSecondary(itemName2, itemType2, president2);

			}
		}
	}

	/**
	 * This method must be called after all XML parsing has completed. It will
	 * set the relationships between all start packets and the start items that
	 * each one contains.
	 */
	protected static void init()
	{
		Iterator it = packets.values().iterator();
		StartPacket sp;
		Iterator it2;
		while (it.hasNext())
		{
			sp = (StartPacket) it.next();
			it2 = sp.items.iterator();
			while (it2.hasNext())
			{
				((StartItem) it2.next()).init();
			}
		}
	}

	/**
	 * Get the start packet with as given name.
	 * 
	 * @param name
	 *            The start packet name.
	 * @return The start packet (or null if it does not exist).
	 */
	public static StartPacket getStartPacket(String name)
	{
		return (StartPacket) packets.get(name);
	}

	/**
	 * Get the start packet with the default name.
	 * 
	 * @return The default start packet (or null if it does not exist).
	 */
	public static StartPacket getStartPacket()
	{
		return getStartPacket(DEFAULT_NAME);
	}

	/**
	 * Get the items of this start packet.
	 * 
	 * @return The List of start items.
	 */
	public List getItems()
	{
		return items;
	}

	public StartItem getItem(int index)
	{
		return (StartItem) items.get(index);
	}

	/**
	 * Get the first start item. This one often gets a special treatment (price
	 * reduction).
	 * 
	 * @return first item
	 */
	public StartItem getFirstItem()
	{
		if (!items.isEmpty())
		{
			return (StartItem) items.get(0);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Get the first start item that has not yet been sold. In many cases this
	 * is the only item that can be bought immediately.
	 * 
	 * @return first unsold item
	 */
	public StartItem getFirstUnsoldItem()
	{
		StartItem result;
		Iterator it = items.iterator();
		while (it.hasNext())
		{
			if (!(result = (StartItem) it.next()).isSold())
				return result;
		}
		return null;
	}

	/**
	 * Get all not yet sold start items.
	 * 
	 * @return A List of all unsold items.
	 */
	public List getUnsoldItems()
	{
		List unsoldItems = new ArrayList();
		Iterator it = items.iterator();
		StartItem u;
		while (it.hasNext())
		{
			if (!(u = (StartItem) it.next()).isSold())
			{
				unsoldItems.add(u);
			}
		}
		return unsoldItems;
	}

	/**
	 * Check if all items have bene sold.
	 * 
	 * @return True if all items have been sold.
	 */
	public boolean areAllSold()
	{
		Iterator it = items.iterator();
		while (it.hasNext())
		{
			if (!((StartItem) it.next()).isSold())
				return false;
		}
		return true;
	}

	/**
	 * Get the name of the StartRound class that will sell out this packet.
	 * 
	 * @return StartRound subclass name.
	 */
	public String getRoundClassName()
	{
		return roundClassName;
	}

	public String getVariant()
	{
		return variant;
	}

}
