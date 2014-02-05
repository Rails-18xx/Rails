package rails.game;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.model.CountingMoneyModel;
import rails.game.state.IntegerState;
import rails.game.state.Model;

/**
 * Each object of this class represents a "start packet item", which consist of
 * one or two certificates. The whole start packet must be bought before the
 * stock rounds can start. <p> During XML parsing, only the certificate name and
 * other attributes are saved. The certificate objects are linked to in the
 * later initialisation step.
 */
public class StartItem extends RailsAbstractItem {

    // Fixed properties
    protected String name = null;
    protected Certificate primary = null;
    protected Certificate secondary = null;
    protected final CountingMoneyModel basePrice = CountingMoneyModel.create(this, "basePrice", false);
    protected int row = 0;
    protected int column = 0;
    protected int index;

    // Bids
    protected final IntegerState lastBidderIndex = IntegerState.create(this, "lastBidder", -1);
    protected List<Player> players;
    protected int numberOfPlayers;
    protected CountingMoneyModel[] bids;
    protected final CountingMoneyModel minimumBid = CountingMoneyModel.create(this, "minimumBid", false);

    // Status info for the UI ==> MOVED TO BuyOrBidStartItem
    // TODO REDUNDANT??
    /**
     * Status of the start item (buyable? biddable?) regardless whether the
     * current player has the amount of (unblocked) cash to buy it or to bid on
     * it.
     */
    protected final IntegerState status = IntegerState.create(this, "status");

    public static final int UNAVAILABLE = 0;
    public static final int BIDDABLE = 1;
    public static final int BUYABLE = 2;
    public static final int SELECTABLE = 3;
    public static final int AUCTIONED = 4;
    public static final int NEEDS_SHARE_PRICE = 5; // TODO No longer used (may
    // not be true after
    // bidding), needs code
    // cleanup
    public static final int SOLD = 6;

    public static final String[] statusName =
            new String[] { "Unavailable", "Biddable", "Buyable", "Selectable",
                    "Auctioned", "NeedingSharePrice", "Sold" };

    // For initialisation purposes only
    protected String type = null;
    protected boolean president = false;
    protected String name2 = null;
    protected String type2 = null;
    protected boolean president2 = false;

    // Static properties
    //protected static Portfolio ipo;
    //protected static Portfolio unavailable;
    //protected static CompanyManager compMgr;
    //protected static int nextIndex = 0;

    protected static Map<String, StartItem> startItemMap;

    protected static Logger log =
            LoggerFactory.getLogger(StartItem.class);

    /**
     * The constructor, taking the properties of the "primary" (often teh only)
     * certificate. The parameters are only stored, real initialisation is done
     * by the init() method.
     * 
     * FIXME: Double usage for name and id
     *
     */
    private StartItem(RailsItem parent, String name, String type, int index, boolean president) {
        super(parent, name);
        this.name = name;
        this.type = type;
        this.index = index;
        this.president = president;

        if (startItemMap == null)
            startItemMap = new HashMap<String, StartItem>();
        startItemMap.put(name, this);

        minimumBid.setSuppressZero(true);

    }

    /** 
     * @param name The Company name of the primary certificate. This name will
     * also become the name of the start item itself.
     * @param type The CompanyType name of the primary certificate.
     * @param president True if the primary certificate is the president's
     * share.
     * @return a fully intialized StartItem 
     */
    public static StartItem create(RailsItem parent, String name, String type, int price, int index, boolean president){
        StartItem item = new StartItem(parent, name, type, index, president);
        item.initBasePrice(price);
        return item;
    }
    
    private void initBasePrice(int basePrice) {
        this.basePrice.set(basePrice);
    }
    
    /**
     * Add a secondary certificate, that "comes with" the primary certificate.
     *
     * @param name2 The Company name of the secondary certificate.
     * @param type2 The CompanyType name of the secondary certificate.
     * @param president2 True if the secondary certificate is the president's
     * share.
     */
    public void setSecondary(String name2, String type2, boolean president2) {
        this.name2 = name2;
        this.type2 = type2;
        this.president2 = president2;
    }

    /**
     * Initialisation, to be called after all XML parsing has completed, and
     * after IPO initialisation.
     */
    public void init(GameManager gameManager) {

        this.players = getRoot().getPlayerManager().getPlayers();
        numberOfPlayers = players.size();
        bids = new CountingMoneyModel[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            // TODO: Check if this is correct or that it should be initialized with zero
            bids[i] = CountingMoneyModel.create(this, "bidBy_" + players.get(i).getId(), false);
            bids[i].setSuppressZero(true);

        }
        // TODO Leave this for now, but it should be done
        // in the game-specific StartRound class
        minimumBid.set(basePrice.value() + 5);

        BankPortfolio ipo = getRoot().getBank().getIpo();
        BankPortfolio unavailable = getRoot().getBank().getUnavailable();

        CompanyManager compMgr = getRoot().getCompanyManager();

        Company company = compMgr.getCompany(type, name);
        if (company instanceof PrivateCompany) {
            primary = (Certificate) company;
        } else {
            primary = ipo.getPortfolioModel().findCertificate((PublicCompany) company, president);
            // Move the certificate to the "unavailable" pool.
            PublicCertificate pubcert = (PublicCertificate) primary;
            if (pubcert.getOwner() == null
                || pubcert.getOwner() != unavailable.getParent()) {
                pubcert.moveTo(unavailable);
            }
        }

        // Check if there is another certificate
        if (name2 != null) {

            Company company2 = compMgr.getCompany(type2, name2);
            if (company2 instanceof PrivateCompany) {
                secondary = (Certificate) company2;
            } else {
                secondary =
                        ipo.getPortfolioModel().findCertificate((PublicCompany) company2,
                                president2);
                // Move the certificate to the "unavailable" pool.
                // FIXME: This is still an issue to resolve  ???
                PublicCertificate pubcert2 = (PublicCertificate) secondary;
                if (pubcert2.getOwner() != unavailable) {
                    pubcert2.moveTo(unavailable);
                }
            }
        }

    }

    public int getIndex() {
        return index;
    }

    /**
     * Set the start packet row. <p> Applies to games like 1835 where start
     * items are organised and become available in rows.
     *
     * @param row
     */
    protected void setRow(int row) {
        this.row = row;
    }

    /**
     * Set the start packet row. <p> Applies to games like 1837 where start
     * items are organised and become available in columns.
     *
     * @param row
     */
    protected void setColumn(int column) {
        this.column = column;
    }

    /**
     * Get the row number.
     *
     * @see setRow()
     * @return The row number. Default 0.
     */
    public int getRow() {
        return row;
    }

    /**
     * Get the column number.
     *
     * @see setColumn()
     * @return The column number. Default 0.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get the primary (or only) certificate.
     *
     * @return The primary certificate object.
     */
    public Certificate getPrimary() {
        return primary;
    }

    /**
     * Check if there is a secondary certificate.
     *
     * @return True if there is a secondary certificate.
     */
    public boolean hasSecondary() {
        return secondary != null;
    }

    /**
     * Get the secondary certificate.
     *
     * @return The secondary certificate object, or null if it does not exist.
     */
    public Certificate getSecondary() {
        return secondary;
    }

    /**
     * Get the start item base price.
     *
     * @return The base price.
     */
    public int getBasePrice() {
        return basePrice.value();
    }

    public void reduceBasePriceBy(int amount) {
        basePrice.change(-amount);
    }

    /**
     * Get the start item name (which is the company name of the primary
     * certificate).
     *
     * @return The start item name.
     */
    public String getName() {
        return name;
    }

    /**
     * Register a bid. <p> This method does <b>not</b> check off the amount of
     * money that a player has available for bidding.
     *
     * @param amount The bid amount.
     * @param bidder The bidding player.
     * special amounts are 0 for 18EU as buy price, -1 as standard pass, -2 and below as pass in 18EU 
     */
    public void setBid(int amount, Player bidder) {
        int index = bidder.getIndex();
        bids[index].set(amount);

        if (amount > 0) {
            lastBidderIndex.set(index);
            minimumBid.set(amount + 5);
        } else if (amount == 0) {
            // Used in 18EU to force making the "bid"
            // (in fact: buy price) visible
            // FIXME: is this still required?
            // bids[index].update();
        } else if (amount == -1) {
            // Passed (standard type)
            bids[index].set(0);
            // FIXME: is this still required?
            // bids[index].update();
        }
    }

    /**
     * Get the currently highest bid amount.
     *
     * @return The bid amount (0 if there have been no bids yet).
     */
    public int getBid() {
        int index = lastBidderIndex.value();
        if (index < 0) {
            return 0;
        } else {
            return bids[index].value();
        }
    }

    /**
     * Get the highest bid done so far by a particular player.
     *
     * @param player The name of the player.
     * @return The bid amount for this player (default 0).
     */
    public int getBid(Player player) {
        int index = player.getIndex();
        return bids[index].value();
    }

    /**
     * Return the total number of players that has done bids so far on this
     * item.
     *
     * @return The number of bidders.
     */
    public int getBidders() {
        int bidders = 0;
        for (int i = 0; i < numberOfPlayers; i++) {
            if (bids[i].value() > 0) bidders++;
        }
        return bidders;
    }

    /**
     * Get the highest bidder so far.
     *
     * @return The player object that did the highest bid.
     */
    public Player getBidder() {
        int index = lastBidderIndex.value();
        if (index < 0) {
            return null;
        } else {
            return players.get(lastBidderIndex.value());
        }
    }

    /**
     * Get the minimum allowed next bid. TODO 5 should be configurable.
     *
     * @return Minimum bid
     */
    public int getMinimumBid() {
        return minimumBid.value();
    }

    public void setMinimumBid(int value) {
        minimumBid.set(value);
    }

    /**
     * Check if a player has done any bids on this start item.
     *
     * @param playerName The name of the player.
     * @return True if this player has done any bids.
     */
    public boolean hasBid(Player player) {
        int index = player.getIndex();
        return bids[index].value() > 0;
    }

    /**
     * Check if the start item has been sold.
     *
     * @return True if this item has been sold.
     */
    public boolean isSold() {
        return status.value() == SOLD;
    }

    /**
     * Set the start item sold status.
     *
     * @param sold The new sold status (usually true).
     */
    public void setSold(Player player, int buyPrice) {
        status.set(SOLD);

        int index = player.getIndex();
        lastBidderIndex.set(index);

        // For display purposes, set all lower bids to zero
        for (int i = 0; i < numberOfPlayers; i++) {
            // Unblock any bid money
            if (bids[i].value() > 0) {
                players.get(i).unblockCash(bids[i].value());
                if (index != i) bids[i].set(0);
            }
        }
        bids[index].set(buyPrice);
        minimumBid.set(0);
    }

    /**
     * This method indicates if there is a company for which a par price must be
     * set when this start item is bought. The UI can use this to ask for the
     * price immediately, so bypassing the extra "price asking" intermediate
     * step.
     *
     * @return A public company for which a price must be set.
     */
    public PublicCompany needsPriceSetting() {
        PublicCompany company;

        if ((company = checkNeedForPriceSetting(primary)) != null) {
            return company;
        } else if (secondary != null
                   && ((company = checkNeedForPriceSetting(secondary)) != null)) {
            return company;
        }

        return null;
    }

    /**
     * If a start item component a President's certificate that needs price
     * setting, return the name of thecompany for which the price must be set.
     *
     * @param certificate
     * @return Name of public company, or null
     */
    protected PublicCompany checkNeedForPriceSetting(Certificate certificate) {

        if (!(certificate instanceof PublicCertificate)) return null;

        PublicCertificate publicCert = (PublicCertificate) certificate;

        if (!publicCert.isPresidentShare()) return null;

        PublicCompany company = publicCert.getCompany();

        if (!company.hasStockPrice()) return null;

        if (company.getIPOPrice() != 0) return null;

        return company;

    }

    public int getStatus() {
        return status.value();
    }

    public IntegerState getStatusModel () {
        return status;
    }

    public String getStatusName() {
        return statusName[status.value()];
    }

    public void setStatus(int status) {
        this.status.set(status);
    }

    public Model getBasePriceModel() {
        return basePrice;
    }

    public Model getBidForPlayerModel(int index) {
        return bids[index];
    }

    public Model getMinimumBidModel() {
        return minimumBid;
    }

    public static StartItem getByName(String name) {
        return startItemMap.get(name);
    }

    public String getType() {
        return type;
    }

    // FIXME: This is not a good idea as long as hashCode has not been changed
//    public boolean equals(StartItem item) {
//        log.debug("Item " + item.getType() + "/" + item.getName()
//                  + " is compared with " + type + "/" + name);
//        return this.name.equals(item.getName())
//               && this.type.equals(item.getType());
//    }

//    @Override
//    public String toString() {
//        return ("StartItem "+name+" status="+statusName[status.value()]);
//    }

    public String getText () {
        return toString();
    }

}
