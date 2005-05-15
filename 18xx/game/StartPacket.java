/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartPacket.java,v 1.2 2005/05/15 20:47:14 evos Exp $
 * 
 * Created on 06-May-2005
 * Change Log:
 */
package game;

import java.util.*;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class StartPacket {

    private static Map packets = new HashMap();

    /**/
    String name; // For identification if there is more than one.
    String roundClassName;
    List items = new ArrayList();
    
    StartPacket (String name, String roundClassName) {
        this.name = name;
        this.roundClassName = roundClassName;
        packets.put (name, this);
    }
    
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
           if (itemName == null) throw new ConfigurationException ("No item name");
           String itemType = XmlUtils.extractStringAttribute(nnp, "type");
           if (itemType == null) throw new ConfigurationException ("No item type");
           boolean president = XmlUtils.hasValue (XmlUtils.extractStringAttribute(nnp, "president", ""));

           int basePrice = XmlUtils.extractIntegerAttribute(nnp, "basePrice", 0);
           StartItem item = (new StartItem(itemName, itemType, basePrice, president));
           items.add (item);
           
           // Optional attributes
           int row = XmlUtils.extractIntegerAttribute(nnp, "row", 0);
           if (row > 0) item.setRow(row);
           int column = XmlUtils.extractIntegerAttribute(nnp, "column", 0);
           if (column > 0) item.setColumn (column);
           
           // Check if there is another certificate
           NodeList children2 = itemEl.getElementsByTagName("SubItem");
           if (children2.getLength() > 0) {
	           NamedNodeMap nnp2 = ((Element) children2.item(0)).getAttributes();
	           String itemName2 = XmlUtils.extractStringAttribute(nnp2, "name");
	           if (itemName2 == null) throw new ConfigurationException ("No item name");
	           String itemType2 = XmlUtils.extractStringAttribute(nnp2, "type");
	           if (itemType2 == null) throw new ConfigurationException ("No item type");
	           boolean president2 = XmlUtils.hasValue (XmlUtils.extractStringAttribute(nnp2, "president", ""));

	           item.setSecondary(itemName2, itemType2, president2);

           }
        }
     }
    
    protected static void init() {
        Iterator it = packets.values().iterator();
        StartPacket sp;
        Iterator it2;
        while (it.hasNext()) {
            sp = (StartPacket)it.next();
            it2 = sp.items.iterator();
            while (it2.hasNext()) {
                ((StartItem)it2.next()).init();
            }
        }
    }
    
   
    public static StartPacket getStartPacket (String name) {
        return (StartPacket) packets.get(name);
    }
    
    public List getItems() {
        return items;
    }
    
    public StartItem getFirstItem () {
        if (!items.isEmpty()) {
            return (StartItem) items.get(0);
        } else {
            return null;
        }
    }
    
    public StartItem getFirstUnsoldItem () {
        StartItem result = null;
        Iterator it = items.iterator();
        while (it.hasNext()) {
            if (!(result = (StartItem)it.next()).isSold()) break;
        }
        return result;
    }
    
    public List getUnsoldItems () {
        List unsoldItems = new ArrayList();
        Iterator it = items.iterator();
        StartItem u;
        while (it.hasNext()) {
            if (!(u = (StartItem)it.next()).isSold()) {
                unsoldItems.add(u);
            }
        }
        return unsoldItems;
    }
    
    public boolean areAllSold () {
        Iterator it = items.iterator();
        while (it.hasNext()) {
            if (!((StartItem)it.next()).isSold()) return false;
        }
        return true;
    }
    
    public String getRoundClassName () {
        return roundClassName;
    }
    
 }
