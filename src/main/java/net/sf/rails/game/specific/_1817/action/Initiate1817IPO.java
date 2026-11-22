package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

/**
 * Action generated when a player initiates an IPO in 1817.
 * Storing the company ID as a String to ensure serialization compatibility.
 */
public class Initiate1817IPO extends PossibleAction {

    private static final long serialVersionUID = 1L;
    
    private final String companyName;
    private String hexId;
    private int bid;

    public Initiate1817IPO(RailsRoot root, String companyName) {
        super(root);
        this.companyName = companyName;
    }

    /**
     * Helper to resolve the company object from the stored ID string.
     */
    public PublicCompany getCompany() {
        return getRoot().getCompanyManager().getPublicCompany(companyName);
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getHexId() {
        return hexId;
    }

    public void setHexId(String hexId) {
        this.hexId = hexId;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    @Override
    public String toString() {
        return "Initiate IPO for " + companyName + " at " + (hexId != null ? hexId : "??") + " with bid $" + bid;
    }
}