package net.sf.rails.game.specific._18Chesapeake;

import java.util.List;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartPacket;
import net.sf.rails.util.Util;

public class StartPacket_18Chesapeake extends StartPacket {

	public StartPacket_18Chesapeake(RailsItem parent, String id, String roundClassName) {
		super(parent, id, roundClassName);
		// TODO Auto-generated constructor stub
	}
	
	   /**
     * @param id The start packet name.
     * @param roundClassName The StartRound class name.
     */
    public static StartPacket_18Chesapeake create(RailsItem parent, String id, String roundClassName) {
        return new StartPacket_18Chesapeake(parent, id, roundClassName);
    }
	
	  /**
     * Configure the start packet from the contents of a &lt;StartPacket&gt; XML
     * element.
     *
     * @param element The &lt;StartPacket&gt; Element object.
     * @throws ConfigurationException if anything goes wrong.
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        Tag biddingTag = tag.getChild("Bidding");
        if (biddingTag != null) {
            minimumInitialIncrement =
                    biddingTag.getAttributeAsInteger("initial",
                            minimumInitialIncrement);
            minimumIncrement =
                    biddingTag.getAttributeAsInteger("minimum",
                            minimumIncrement);
            modulus = biddingTag.getAttributeAsInteger("increment", modulus);
        }
        List<Tag> itemTags = tag.getChildren("Item");
        
        int index = 0;
        for (Tag itemTag : itemTags) {

            // Extract the attributes of the Start Packet Item (certificate)
            String itemName = itemTag.getAttributeAsString("name");
            if (itemName == null)
                throw new ConfigurationException("No item name");
            String itemType = itemTag.getAttributeAsString("type");
            if (itemType == null)
                throw new ConfigurationException("No item type");
            boolean president =
                    Util.hasValue(itemTag.getAttributeAsString("president", ""));
           
            int basePrice = itemTag.getAttributeAsInteger("basePrice", 0);
            boolean reduceable = itemTag.getAttributeAsBoolean("reduceable", false); 
            StartItem_18Chesapeake item = StartItem_18Chesapeake.create(this, itemName, itemType, basePrice, reduceable, index++, president);
            items.add(item);

              // Check if there is another certificate
            List<Tag> subItemTags = itemTag.getChildren("SubItem");
            if (subItemTags != null) {
                for (Tag subItemTag : subItemTags) {

                    String itemName2 = subItemTag.getAttributeAsString("name");
                    if (itemName2 == null)
                        throw new ConfigurationException("No item name");
                    String itemType2 = subItemTag.getAttributeAsString("type");
                    if (itemType2 == null)
                        throw new ConfigurationException("No item type");
                    boolean president2 =
                            Util.hasValue(subItemTag.getAttributeAsString(
                                    "president", ""));

                    item.setSecondary(itemName2, itemType2, president2);
                }

            }
            log.debug("ItemTag = " + itemTag);
        }
    }
    
    
}
