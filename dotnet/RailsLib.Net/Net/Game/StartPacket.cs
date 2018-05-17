using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * A Start Packet comprises a number of Start Items. The typical start packet
 * must be completely sold out before normal share buying can start (but there
 * are exceptions to this rule).
 */

namespace GameLib.Net.Game
{
    public class StartPacket : RailsAbstractItem
    {
        private static Logger<StartPacket> log = new Logger<StartPacket>();

        /** The name of the class that implements the Start Round for this packet. */
        private string roundClassName;
        /** The start items in this packet. */
        protected List<StartItem> items = new List<StartItem>();
        /** The minimum initial bidding increment above the share price */
        private int minimumInitialIncrement = 5;
        /** The minimum increment between subsequent bids */
        private int minimumIncrement = 5;
        /** The modulus of all bids (i.e. of which value the bid must be a multiple) */
        private int modulus = 5;

        /** Default name */
        public const string DEFAULT_ID = "Initial";

        protected StartPacket(IRailsItem parent, string id, string roundClassName) :
                base(parent, !string.IsNullOrEmpty(id) ? id : DEFAULT_ID)
        {
            this.roundClassName = roundClassName;
            //packets.put(name, this);
        }

        /**
         * @param id The start packet name.
         * @param roundClassName The StartRound class name.
         */
        public static StartPacket Create(IRailsItem parent, string id, string roundClassName)
        {
            return new StartPacket(parent, id, roundClassName);
        }

        /**
         * Configure the start packet from the contents of a &lt;StartPacket&gt; XML
         * element.
         *
         * @param element The &lt;StartPacket&gt; Element object.
         * @throws ConfigurationException if anything goes wrong.
         */
        public void ConfigureFromXML(Tag tag)
        {
            Tag biddingTag = tag.GetChild("Bidding");
            if (biddingTag != null)
            {
                minimumInitialIncrement =
                        biddingTag.GetAttributeAsInteger("initial",
                                minimumInitialIncrement);
                minimumIncrement =
                        biddingTag.GetAttributeAsInteger("minimum",
                                minimumIncrement);
                modulus = biddingTag.GetAttributeAsInteger("increment", modulus);
            }
            List<Tag> itemTags = tag.GetChildren("Item");

            int index = 0;
            foreach (Tag itemTag in itemTags)
            {
                // Extract the attributes of the Start Packet Item (certificate)
                string itemName = itemTag.GetAttributeAsString("name");
                if (itemName == null)
                {
                    throw new ConfigurationException("No item name");
                }
                string itemType = itemTag.GetAttributeAsString("type");
                if (itemType == null)
                {
                    throw new ConfigurationException("No item type");
                }
                bool president =
                        !string.IsNullOrEmpty(itemTag.GetAttributeAsString("president", ""));

                int basePrice = itemTag.GetAttributeAsInteger("basePrice", 0);
                bool reduceable = itemTag.GetAttributeAsBoolean("reduceable", false);
                StartItem item = StartItem.Create(this, itemName, itemType, basePrice, reduceable, index++, president);
                items.Add(item);

                // Optional attributes
                int row = itemTag.GetAttributeAsInteger("row", 0);
                if (row > 0) item.Row = row;
                int column = itemTag.GetAttributeAsInteger("column", 0);
                if (column > 0) item.Column = column;

                // Check if there is another certificate
                List<Tag> subItemTags = itemTag.GetChildren("SubItem");
                if (subItemTags != null)
                {
                    foreach (Tag subItemTag in subItemTags)
                    {
                        string itemName2 = subItemTag.GetAttributeAsString("name");
                        if (itemName2 == null)
                        {
                            throw new ConfigurationException("No item name");
                        }
                        string itemType2 = subItemTag.GetAttributeAsString("type");
                        if (itemType2 == null)
                        {
                            throw new ConfigurationException("No item type");
                        }
                        bool president2 =
                                !string.IsNullOrEmpty(subItemTag.GetAttributeAsString("president", ""));

                        item.SetSecondary(itemName2, itemType2, president2);
                    }
                }
                log.Debug("ItemTag = " + itemTag);
            }
        }

        /**
         * This method must be called after all XML parsing has completed. It will
         * set the relationships between all start packets and the start items that
         * each one contains.
         */
        public void Init(GameManager gameManager)
        {
            foreach (StartItem item in items)
            {
                item.Init(gameManager);
            }
        }

        /**
         * Get the start packet with as given name.
         *
         * @param name The start packet name.
         * @return The start packet (or null if it does not exist).
         */
        //public static StartPacket getStartPacket(string name) {
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
        public List<StartItem> Items
        {
            get
            {
                return items;
            }
        }

        public StartItem GetItem(int index)
        {
            return items[index];
        }

        public void AddItem(StartItem startItem)
        {
            items.Add(startItem);
        }

        /**
         * Get the first start item. This one often gets a special treatment (price
         * reduction).
         *
         * @return first item
         */
        public StartItem GetFirstItem()
        {
            if (items.Count > 0)
            {
                return items[0];
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
        public StartItem GetFirstUnsoldItem()
        {
            foreach (StartItem item in items)
            {
                if (!item.IsSold) return item;
            }
            return null;
        }

        /**
         * Get all not yet sold start items.
         *
         * @return A List of all unsold items.
         */
        public List<StartItem> GetUnsoldItems()
        {
            List<StartItem> unsoldItems = new List<StartItem>();
            foreach (StartItem item in items)
            {
                if (!(item.IsSold))
                {
                    unsoldItems.Add(item);
                }
            }
            return unsoldItems;
        }

        /**
         * Check if all items have been sold.
         *
         * @return True if all items have been sold.
         */
        public bool AreAllSold()
        {
            foreach (StartItem item in items)
            {
                if (!item.IsSold) return false;
            }
            return true;
        }

        /**
         * Get the name of the StartRound class that will sell out this packet.
         *
         * @return StartRound subclass name.
         */
        public string RoundClassName
        {
            get
            {
                return roundClassName;
            }
        }

        public int NumberOfItems
        {
            get
            {
                return items.Count;
            }
        }

        public int MinimumIncrement
        {
            get
            {
                return minimumIncrement;
            }
        }

        public int MinimumInitialIncrement
        {
            get
            {
                return minimumInitialIncrement;
            }
        }

        public int Modulus
        {
            get
            {
                return modulus;
            }
        }
    }
}
