/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartItem.java,v 1.5 2005/05/25 19:08:17 evos Exp $
 * 
 * Created on 04-May-2005
 * Change Log:
 */
package game;

import java.util.*;

/**
 * Each object of this class represents a "start packet item", 
 * which consist of one or two certificates. 
 * The whole start packet must be bought before the stock rounds can start.
 * <p>During XML parsing, only the certificate name and other attributes are saved.
 * The certificate objects are linked to in the later initialisation step. 
 * @author Erik Vos
 */
public class StartItem {
    
    // Fixed properties
    protected String name = null;
    protected Certificate primary = null;
    protected Certificate secondary = null;
    protected int basePrice;
    protected int row = 0;
    protected int column = 0;
    
    // Bids
    protected int bid = 0;
    protected int bids = 0;
    protected Player bidder = null;
    protected Map bidders = new HashMap();
    protected boolean sold;
    
    // For initialisation purposes only
    protected String type = null;
    protected boolean president = false;
    protected String name2 = null;
    protected String type2 = null;
    protected boolean president2 = false;
    
    // Static properties
    protected static Portfolio ipo;
    protected static Portfolio unavailable;
    protected static CompanyManagerI compMgr;
    
    /**
     * The constructor, taking the properties of the "primary" 
     * (often teh only) certificate.
     * The parameters are only stored, real initialisation 
     * is done by the init() method.  
     * @param name The Company name of the primary certificate.
     * This name will also become the name of the start item itself.
     * @param type The CompanyType name of the primary certificate.
     * @param basePrice The start item base selling price,
     * i.e. the price for which the item can be bought or where bidding starts. 
     * @param president True if the primary certificate is the president's share. 
     */
    public StartItem (String name, String type, int basePrice, boolean president) {
        this.name = name;
        this.type = type;        
        this.basePrice = basePrice;
        this.president = president;
    }
    
    /**
     * Add a secondary certificate, that "comes with" the primary certificate.
     * @param name2 The Company name of the secondary certificate.
     * @param type2 The CompanyType name of the secondary certificate.
     * @param president2 True if the secondary certificate is the president's share.
     */
    public void setSecondary (String name2, String type2, boolean president2) {
        this.name2 = name2;
        this.type2 = type2;
        this.president2 = president2;
    }
    
    /**
     * Initialisation, to be called after all XML parsing has completed,
     * and after IPO initialisation.
     */
    public void init() {

        if (ipo == null) ipo = Bank.getIpo();
        if (unavailable == null) unavailable = Bank.getUnavailable();
        if (compMgr == null) compMgr = Game.getCompanyManager();
        
        CompanyI company = compMgr.getCompany (type, name);
        if (company instanceof PrivateCompanyI) {
            primary = (Certificate) company;
        } else {
            primary = ipo.findCertificate((PublicCompanyI) company, president);
            // Move the certificate to the "unavailable" pool.
            PublicCertificateI pubcert = (PublicCertificateI) primary;
//Log.write("*** Company"+name+" certificate "+primary+"/"+pubcert);
            if (pubcert.getPortfolio() == null ||
            		!pubcert.getPortfolio().getName().equals("Unavailable"))
                unavailable.buyCertificate(pubcert, pubcert.getPortfolio(), 0);
        }
        
        // Check if there is another certificate
        if (name2 != null) {
            
           CompanyI company2 = compMgr.getCompany(type2, name2);
           if (company2 instanceof PrivateCompanyI) {
               secondary = (Certificate) company2;
           } else {
               secondary = ipo.findCertificate((PublicCompanyI)company2, president2);
               // Move the certificate to the "unavailable" pool.
               PublicCertificateI pubcert2 = (PublicCertificateI) secondary;
               if (!pubcert2.getPortfolio().getName().equals("Unavailable"))
                   unavailable.buyCertificate(pubcert2, pubcert2.getPortfolio(), 0);
           }
        }
    }
    
    /**
     * Set the start packet row.
     * <p>Applies to games like 1835 where start items are
     * organised and become available in rows. 
     * @param row
     */
    protected void setRow (int row) {
        this.row = row;
    }
    
    /**
     * Set the start packet row.
     * <p>Applies to games like 1837 where start items are
     * organised and become available in columns. 
     * @param row
     */
    protected void setColumn (int column) {
        this.column = column;
    }
    
    /**
     * Get the row number.
     * @see setRow()
     * @return The row number. Default 0.
     */
    public int getRow () {
        return row;
    }
    
    /**
     * Get the column number.
     * @see setColumn()
     * @return The column number. Default 0.
     */
    public int getColumn () {
        return column;
    }
    
    /**
     * Get the primary (or only) certificate.
     * @return The primary certificate object.
     */
    public Certificate getPrimary () {
        return primary;
    }
    
    /**
     * Check if there is a secondary certificate. 
     * @return True if there is a secondary certificate.
     */
    public boolean hasSecondary () {
        return secondary != null;
    }
        
    /**
     * Get the secondary certificate.
     * @return The secondary certificate object, or null if it does not exist.
     */
    public Certificate getSecondary () {
        return secondary;
    }
    
    /**
     * Get the start item base price.
     * @return The base price.
     */
    public int getBasePrice() {
        return basePrice;
    }
    
    /**
     * Get the start item name (which is the company name of the 
     * primary certificate).
     * @return The start item name.
     */
    public String getName () {
        return name;
    }
    
    /**
     * Register a bid.<p>
     * This method does <b>not</b> check off the amount of money
     * that a player has available for bidding.
     * @param amount The bid amount.
     * @param bidder The bidding player.
     */
    public void setBid (int amount, Player bidder) {
        bid = amount;
        bids++;
        this.bidder = bidder;
        if (bidder != null) bidders.put (bidder.getName(), new Bid(bidder.getName(), amount));
    }
    
    /**
     * Remove a player from the list of bidders.
     * @param player The player to be removed.
     * @return The remaining number of bidders.
     */
    public int removeBid (Player player) {
    	if (player != null && bidders.containsKey(player.getName())) {
    		bidders.remove(player.getName());
    	}
    	return bidders.size();
    }
    
    /**
     * Get the currently highest bid amount.
     * @return The bid amount (0 if there have been no bids yet).
     */
    public int getBid () {
        return bid;
    }
    
    /**
     * Get the highest bid done so far by a particular player.
     * @param player The name of the player.
     * @return The bid amount for this player (default 0).
     */
    public int getBid (Player player) {
    	String playerName = player.getName();
    	if (bidders.containsKey(playerName)) {
    		return ((Bid)bidders.get(playerName)).getAmount();
    	} else {
    		return 0;
    	}
    }
    
    /**
     * Return the total number of players that has done bids so far on this item.
     * @return The number of bidders.
     */
    public int getBidders () {
        return bidders.size();
    }
    
    /**
     * Get the highest bidder so far. 
     * @return The player object that did the highest bid.
     */
    public Player getBidder () {
        return bidder;
    }
    
    /**
     * Check if any bids have bene done so far.
     * @return True if there is any bid.
     */
    public boolean hasBid() {
        return bidder != null;
    }
    
    /**
     * Get the minimum allowed next bid.
     * TODO 5 should be configurable.
     * @return Minimum bid
     */
    public int getMinimumBid () {
        if (bid > 0) {
            return bid + 5;
        } else {
            return basePrice + 5;
        }
    }
    
     /**
     * Check if a player has done any bids on this start item.
     * @param playerName The name of the player.
     * @return True if this player has done any bids.
     */
    public boolean hasBid (String playerName) {
        return bidders.containsKey(playerName);
    }
    
    /**
     * Get the last Bid done by a given player. 
     * @param playerName The name of the player.
     * @return His latest Bid object.
     */
    public Bid getBidForPlayer (String playerName) {
        return (Bid) bidders.get(playerName); 
    }
    
    /**
     * Check if the start item has been sold.
     * @return True if this item has been sold.
     */
    public boolean isSold() {
        return sold;
    }
    /**
     * Set the start item sold status.
     * @param sold The new sold status (usually true).
     */
    public void setSold(boolean sold) {
        this.sold = sold;
    }
    /**
     * This method indicates if there is a company for which a par price
     * must be set when this start item is bought. The UI can use this
     * to ask for the price immediately, so bypassing the extra "price asking"
     * intermediate step.
     * @return A public company for which a price must be set.
     */
    public PublicCompanyI needsPriceSetting () {
        if (primary instanceof PublicCertificateI
                && ((PublicCertificateI)primary).isPresidentShare()) {
            return ((PublicCertificateI)primary).getCompany();
        } else if (secondary != null && secondary instanceof PublicCertificateI
                && ((PublicCertificateI)secondary).isPresidentShare()) {
            return ((PublicCertificateI)secondary).getCompany();
        } else {
            return null;
        }
    }
    
    /**
     * Class Bid holds the details of a particular bid on this item:
     * the bidder and the amount.  
     * @author Erik Vos
     */
    public class Bid {
        
        String bidderName;
        int amount;
        
        /**
         * Create a new Bid.
         * @param playerName The bidding player.
         * @param amount The bid amount.
         */
        protected Bid (String playerName, int amount) {
            bidderName = playerName;
            this.amount = amount;
        }
        
        /**
         * Get the bidding player. 
         * @return Player name.
         */
        public String getBidderName () {
            return bidderName;
        }
        
        /**
         * Get the bid amount.
         * @return The bid amount.
         */
        public int getAmount () {
            return amount;
        }
    }

}
