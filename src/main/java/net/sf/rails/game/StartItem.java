package net.sf.rails.game;


import java.util.List;
import java.util.Map;

import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.CountingMoneyModel;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Each object of this class represents a "start packet item", which consist of
 * one or two certificates. The whole start packet must be bought before the
 * stock rounds can start. <p> During XML parsing, only the certificate name and
 * other attributes are saved. The certificate objects are linked to in the
 * later initialisation step.
 */

public class StartItem extends RailsAbstractItem {

    // Fixed properties
    protected Certificate primary = null;
    protected Certificate secondary = null;
    protected final CountingMoneyModel basePrice = CountingMoneyModel.create(this, "basePrice", false);
    protected boolean reduceable;
    protected int row = 0;
    protected int column = 0;
    protected int index;

    // Bids
    protected final GenericState<Player> lastBidder = new GenericState<>(this, "lastBidder");
    protected final Map<Player, CountingMoneyModel> bids = Maps.newHashMap();
    protected final CountingMoneyModel minimumBid = CountingMoneyModel.create(this, "minimumBid", false);
    protected final Map<Player, BooleanState> active = Maps.newHashMap();

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

    protected static final String[] statusName =
            new String[]{"Unavailable", "Biddable", "Buyable", "Selectable",
                    "Auctioned", "NeedingSharePrice", "Sold"};

    // For initialisation purposes only
    protected String type;
    protected boolean president;
    protected String name2 = null;
    protected String type2 = null;
    protected boolean president2 = false;

    public enum NoBidsReaction {
        REDUCE_AND_REBID,
        RUN_OPERATING_ROUND
    }

    ;

    protected NoBidsReaction noBidsReaction = NoBidsReaction.RUN_OPERATING_ROUND;

    private static final Logger log = LoggerFactory.getLogger(StartItem.class);

    /**
     * The constructor, taking the properties of the "primary" (often teh only)
     * certificate. The parameters are only stored, real initialisation is done
     * by the init() method.
     */
    protected StartItem(RailsItem parent, String id, String type, int index, boolean president) {
        super(parent, id);
        this.type = type;
        this.index = index;
        this.president = president;

        minimumBid.setSuppressZero(true);

    }

    /**
     * @param name      The Company name of the primary certificate. This name will
     *                  also become the name of the start item itself.
     * @param type      The CompanyType name of the primary certificate.
     * @param president True if the primary certificate is the president's
     *                  share.
     * @return a fully intialized StartItem
     */
    public static StartItem create(RailsItem parent, String name, String type, int price, boolean reduceable, int index, boolean president) {
        StartItem item = new StartItem(parent, name, type, index, president);
        item.initBasePrice(price);
        item.setReducePrice(reduceable);
        return item;
    }

    protected void initBasePrice(int basePrice) {
        this.basePrice.set(basePrice);
    }

    protected void setReducePrice(boolean reduceable) {
        this.reduceable = reduceable;
    }

    /**
     * Add a secondary certificate, that "comes with" the primary certificate.
     *
     * @param name2      The Company name of the secondary certificate.
     * @param type2      The CompanyType name of the secondary certificate.
     * @param president2 True if the secondary certificate is the president's
     *                   share.
     */
    public void setSecondary(String name2, String type2, boolean president2) {
        this.name2 = name2;
        this.type2 = type2;
        this.president2 = president2;
    }

    /**
     * Add a secondary certificate, that "comes with" the primary certificate
     * after initialization.
     *
     * @param secondary The secondary certificate.
     */
    public void setSecondary(Certificate secondary) {
        this.secondary = secondary;
    }

    /**
     * Initialisation, to be called after all XML parsing has completed, and
     * after IPO initialisation.
     */
    public void init(GameManager gameManager) {

        List<Player> players = getRoot().getPlayerManager().getPlayers();
        for (Player p : players) {
            // TODO: Check if this is correct or that it should be initialized with zero
            CountingMoneyModel bid = CountingMoneyModel.create(this, "bidBy_" + p.getId(), false);
            bid.setSuppressZero(true);
            bids.put(p, bid);
            active.put(p, new BooleanState(this, "active_" + p.getId()));
        }
        // TODO Leave this for now, but it should be done
        // in the game-specific StartRound class
        minimumBid.set(basePrice.value() + 5);

        BankPortfolio ipo = getRoot().getBank().getIpo();
        BankPortfolio unavailable = getRoot().getBank().getUnavailable();

        CompanyManager compMgr = getRoot().getCompanyManager();

        Company company = compMgr.getCompany(type, getId());
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
     * @param column
     */
    protected void setColumn(int column) {
        this.column = column;
    }

    /**
     * Get the row number.
     *
     * @return The row number. Default 0.
     */
    public int getRow() {
        return row;
    }

    /**
     * Get the column number.
     *
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

    public boolean getReduceable() {
        return reduceable;
    }

    public void reduceBasePriceBy(int amount) {
        basePrice.change(-amount);
    }


    /**
     * Register a bid. <p> This method does <b>not</b> check off the amount of
     * money that a player has available for bidding.
     *
     * @param amount The bid amount.
     * @param bidder The bidding player.
     */
    public void setBid(int amount, Player bidder) {
        CountingMoneyModel bid = bids.get(bidder);
        bid.set(amount);
        bid.setSuppressZero(false);
        active.get(bidder).set(true);
        lastBidder.set(bidder);
        minimumBid.set(amount + 5);
    }

    /**
     * Get the currently highest bid amount.
     *
     * @return The bid amount (0 if there have been no bids yet).
     */
    public int getBid() {
        if (lastBidder.value() == null) {
            return 0;
        } else {
            return bids.get(lastBidder.value()).value();
        }
    }

    /**
     * Get the highest bid done so far by a particular player.
     *
     * @param player The name of the player.
     * @return The bid amount for this player (default 0).
     */
    public int getBid(Player player) {
        return bids.get(player).value();
    }

    /**
     * Return the total number of players that has done bids so far on this
     * item.
     *
     * @return The number of bidders.
     */
    public int getBidders() {
        int bidders = 0;
        for (Player bidder : active.keySet()) {
            if (active.get(bidder).value() == true) {
                bidders++;
            }
        }
        return bidders;
    }

    /**
     * Get the highest bidder so far.
     *
     * @return The player object that did the highest bid.
     */
    public Player getBidder() {
        return lastBidder.value();
    }

    public void setPass(Player player) {
        active.get(player).set(false);
        CountingMoneyModel bid = bids.get(player);
        bid.set(0);
        bid.setSuppressZero(true);
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
     * @param player The player.
     * @return True if this player is active for this startItem
     */
    public boolean isActive(Player player) {
        return active.get(player).value();
    }


    /**
     * Set all players to active on this start item.  Used when
     * players who did not place a bid are still allowed to
     * participate in an auction (e.g. 1862)
     */
    public void setAllActive() {
        for (Player p : active.keySet()) {
            active.get(p).set(true);
        }
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
     */
    public void setSold(Player player, int buyPrice) {
        status.set(SOLD);

        lastBidder.set(player);

        // For display purposes, set all lower bids to zero
        for (Player p : bids.keySet()) {
            CountingMoneyModel bid = bids.get(p);
            // Unblock any bid money
            if (bid.value() > 0) {
                p.unblockCash(bid.value());
                if (p != player) {
                    bid.set(0);
                    bid.setSuppressZero(true);
                }
                active.get(p).set(false);
                ;
            }
        }
        // for winning bidder set bid to buyprice
        bids.get(player).set(buyPrice);
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

    public IntegerState getStatusModel() {
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

    public Model getBidForPlayerModel(Player player) {
        return bids.get(player);
    }

    public Model getMinimumBidModel() {
        return minimumBid;
    }

    public String getType() {
        return type;
    }

    public void setNoBidsReaction(NoBidsReaction action) {
        this.noBidsReaction = action;
    }

    public NoBidsReaction getNoBidsReaction() {
        return noBidsReaction;
    }

    public String getText() {
        return toString();
    }

}

