/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartItem.java,v 1.1 2005/05/12 22:22:28 evos Exp $
 * 
 * Created on 04-May-2005
 * Change Log:
 */
package game;

import java.util.*;

/**
 * Each object of this class represents a "start packet item", 
 * which consist of one or two (in practice never more than two)
 * certificates. The whole start packet must be bought before the 
 * open stock rounds can start.
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
    
    public StartItem (String name, String type, int basePrice, boolean president) {
        this.name = name;
        this.type = type;        
        this.basePrice = basePrice;
        this.president = president;
    }
    
    public void setSecondary (String name2, String type2, boolean president2) {
        this.name2 = name2;
        this.type2 = type2;
        this.president2 = president2;
    }
    
    /**
     * Initialisation to be done after all XML parsing has completed.
     */
    public void init() {

        if (ipo == null) ipo = Bank.getIpo();
        if (unavailable == null) unavailable = Bank.getUnavailable();
        if (compMgr == null) compMgr = Game.getCompanyManager();
        
        CompanyI company = compMgr.getCompany (type, name);
        if (company instanceof PrivateCompanyI) {
            primary = (Certificate) company;
//System.out.println("private "+company.getName()+" price "+basePrice);
//System.out.println(" - in "+primary.getPortfolio().getName());
        } else {
            primary = ipo.findCertificate((PublicCompanyI) company, president);
            // Move the certificate to the "unavailable" pool.
            PublicCertificateI pubcert = (PublicCertificateI) primary;
            if (!pubcert.getPortfolio().getName().equals("Unavailable"))
                unavailable.buyCertificate(pubcert, pubcert.getPortfolio(), 0);
//System.out.println("public "+company.getName()+" price "+basePrice);
        }
        
        // Check if there is another certificate
        if (name2 != null) {
            
           CompanyI company2 = compMgr.getCompany(type2, name2);
           if (company2 instanceof PrivateCompanyI) {
               secondary = (Certificate) company2;
//System.out.println("extra private "+company2.getName());
           } else {
               secondary = ipo.findCertificate((PublicCompanyI)company2, president2);
               // Move the certificate to the "unavailable" pool.
               PublicCertificateI pubcert2 = (PublicCertificateI) secondary;
               if (!pubcert2.getPortfolio().getName().equals("Unavailable"))
                   unavailable.buyCertificate(pubcert2, pubcert2.getPortfolio(), 0);
//System.out.println("extra public "+company2.getName()+" "+pubcert2.getShare()+"%");
           }
        }

    }
    
    protected void setRow (int row) {
        this.row = row;
    }
    
    protected void setColumn (int column) {
        this.column = column;
    }
    
    public int getRow () {
        return row;
    }
    
    public int getColumn () {
        return column;
    }
    
    public Certificate getPrimary () {
        return primary;
    }
    
    public boolean hasSecondary () {
        return secondary != null;
    }
        
    public Certificate getSecondary () {
        return secondary;
    }
    
    public int getBasePrice() {
        return basePrice;
    }
    
    public String getName () {
        return name;
    }
    
    public void setBid (int amount, Player bidder) {
        bid = amount;
        bids++;
        this.bidder = bidder;
        if (bidder != null) bidders.put (bidder.getName(), new Bid(bidder.getName(), amount));
    }
    
    public int getBid () {
        return bid;
    }
    
    public int getBids () {
        return bids;
    }
    
    public Player getBidder () {
        return bidder;
    }
    
    public boolean hasBid() {
        return bidder != null;
    }
    
    /**
     * Get the minimum allowed next bid.
     * TODO 5 should be sonfigurable.
     * @return Minimum bid
     */
    public int getMinimumBid () {
        if (bid > 0) {
            return bid + 5;
        } else {
            return basePrice + 5;
        }
    }
    
    public Bid[] getBidders() {
        return (Bid[]) bidders.entrySet().toArray(new Bid[0]);
    }
    
    public boolean hasBid (String playerName) {
        return bidders.containsKey(playerName);
    }
    
    public Bid getBidForPlayer (String playerName) {
        return (Bid) bidders.get(playerName); 
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
    
    public class Bid {
        
        String bidderName;
        int amount;
        
        protected Bid (String playerName, int amount) {
            bidderName = playerName;
            this.amount = amount;
        }
        
        public String getBidderName () {
            return bidderName;
        }
        
        public int getAmount () {
            return amount;
        }
    }

}
