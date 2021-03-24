package net.sf.rails.game;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Start Packet comprises a number of Start Items. The typical start packet
 * must be completely sold out before normal share buying can start (but there
 * are exceptions to this rule).
 */
public class StartPacket extends RailsAbstractItem {

    protected static final Logger log = LoggerFactory.getLogger(StartPacket.class);

    /** The name of the class that implements the Start Round for this packet. */
    private String roundClassName;
    /** The start items in this packet. */
    protected List<StartItem> items = new ArrayList<StartItem>();
    /** The minimum initial bidding increment above the share price */
    protected int minimumInitialIncrement = 5;
    /** The minimum increment between subsequent bids */
    protected int minimumIncrement = 5;
    /** The modulus of all bids (i.e. of which value the bid must be a multiple) */
    protected int modulus = 5;
    /** Is multiple-column display enabled?
     *  If so, row and col attributes become mandatory for all start items
     *  (if the <MultipleColumn> tag precedes all start items).*/
    protected boolean multipleColumns = false;
    /** The number of columns. Will be derived from the column attributes. */
    protected int numberOfColumns = 1;
    /** The number of rows. Will be derived from the row attributes, if multipleColumns is true. */
    protected int numberOfRows;

    /** Default name */
    public static final String DEFAULT_ID = "Initial";

    protected StartPacket(RailsItem parent, String id, String roundClassName) {
        super(parent, Util.hasValue(id) ? id : DEFAULT_ID);
        this.roundClassName = roundClassName;
        //packets.put(name, this);
    }

    /**
     * @param id The start packet name.
     * @param roundClassName The StartRound class name.
     */
    public static StartPacket create(RailsItem parent, String id, String roundClassName) {
        return new StartPacket(parent, id, roundClassName);
    }

    /**
     * Configure the start packet from the contents of a &lt;StartPacket&gt; XML
     * element.
     *
     * @param tag The &lt;StartPacket&gt; Element object.
     * @throws ConfigurationException if anything goes wrong.
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Multiple column display?
        Tag columnsTag = tag.getChild("MultipleColumns");
        multipleColumns = columnsTag != null;

        // Bidding parameters, if applicable
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
            StartItem item = StartItem.create(this, itemName, itemType,
                    basePrice, reduceable, index++, president);
            items.add(item);

            // Optional attributes
            int row = itemTag.getAttributeAsInteger("row", 0);
            item.setRow(row);
            int column = itemTag.getAttributeAsInteger("column", 0);
            if (multipleColumns) {
                if (!(row > 0 && column > 0)) {
                    throw new ConfigurationException(
                            "With multiple columns, both row and column attributes are required");
                }
                item.setColumn(column);
                numberOfRows = Math.max (numberOfRows, row);
                numberOfColumns = Math.max (numberOfColumns, column);
            }

            // Displayed name
            String displayName = itemTag.getAttributeAsString("displayName", null);
            if (displayName != null) item.setDisplayName(displayName);

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
            log.debug("ItemTag = {}", itemTag);
        }
    }

    /**
     * This method must be called after all XML parsing has completed. It will
     * set the relationships between all start packets and the start items that
     * each one contains.
     */
    protected void init(GameManager gameManager) {
        for (StartItem item : items) {
            item.init(gameManager);
        }
    }

    /**
     * Get the start packet with as given name.
     *
     * @param name The start packet name.
     * @return The start packet (or null if it does not exist).
     */
    //public static StartPacket getStartPacket(String name) {
    //    return packets.get(name);
    //}

    /**
     * Get the start packet with the default name.
     *
     * @return The default start packet (or null if it does not exist).
     */
    //public static StartPacket getStartPacket() {
    //    return getStartPacket(DEFAULT_NAME);
    //}

    /**
     * Get the items of this start packet.
     *
     * @return The List of start items.
     */
    public List<StartItem> getItems() {
        return items;
    }

    public StartItem getItem(int index) {
        return items.get(index);
    }

    public void addItem(StartItem startItem) {
        items.add(startItem);
    }

    /**
     * Get the first start item. This one often gets a special treatment (price
     * reduction).
     *
     * @return first item
     */
    public StartItem getFirstItem() {
        if (!items.isEmpty()) {
            return items.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get the first start item that has not yet been sold. In many cases this
     * is the only item that can be bought immediately.
     *
     * @return first unsold item
     */
    public StartItem getFirstUnsoldItem() {
        for (StartItem item : items) {
            if (!item.isSold()) return item;
        }
        return null;
    }

    /**
     * Get all not yet sold start items.
     *
     * @return A List of all unsold items.
     */
    public List<StartItem> getUnsoldItems() {
        List<StartItem> unsoldItems = new ArrayList<StartItem>();
        for (StartItem item : items) {
            if (!(item.isSold())) {
                unsoldItems.add(item);
            }
        }
        return unsoldItems;
    }

    /**
     * Check if all items have been sold.
     *
     * @return True if all items have been sold.
     */
    public boolean areAllSold() {
        for (StartItem item : items) {
            if (!item.isSold()) return false;
        }
        return true;
    }

    /**
     * Get the name of the StartRound class that will sell out this packet.
     *
     * @return StartRound subclass name.
     */
    public String getRoundClassName() {
        return roundClassName;
    }

    public int getNumberOfItems() {
        return items.size();
    }

    public int getMinimumIncrement() {
        return minimumIncrement;
    }

    public int getMinimumInitialIncrement() {
        return minimumInitialIncrement;
    }

    public int getModulus() {
        return modulus;
    }

    public boolean isMultipleColumns() {
        return multipleColumns;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }
}
