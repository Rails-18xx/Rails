/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/SellShares.java,v 1.7 2010/01/31 22:22:29 macfreek Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package net.sf.rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import net.sf.rails.game.*;
import net.sf.rails.util.Util;


/**
 * @author Erik Vos
 */
public class SellShares extends PossibleAction {

    // Server-side settings
    private String companyName;
    transient private PublicCompany company;
    private int shareUnit;
    private int shareUnits;
    private int share;
    private int price;
    private int number;
    /** Dump flag, indicates to which type of certificates the president's share must be exchanged.<br>
     * 0 = no dump, or dump that does not require any choice of exchange certificates;<br>
     * 1 = exchange against 1-share certificates (usually 10%);<br>
     * 2 = exchange against a 2-share certificate (as can occur in 1835);<br>
     * etc.
     */
    private int presidentExchange = 0;

    // For backwards compatibility only
    private int numberSold = 0;

    public static final long serialVersionUID = 1L;

    public SellShares(PublicCompany company, int shareUnits, int number,
            int price) {
        this (company, shareUnits, number, price, 0);
    }

    public SellShares(PublicCompany company, int shareUnits, int number,
            int price, int presidentExchange) {
        this.company = company;
        this.shareUnits = shareUnits;
        this.price = price;
        this.number = number;
        this.presidentExchange = presidentExchange;

        companyName = company.getId();
        shareUnit = company.getShareUnit();
        share = shareUnits * shareUnit;
    }

    /**
     * @return Returns the maximumNumber.
     */
    public int getNumber() {
        return number;
    }

    /**
     * @return Returns the price.
     */
    public int getPrice() {
        return price;
    }

    /**
     * @return Returns the companyName.
     */
    public String getCompanyName() {
        return companyName;
    }

    public PublicCompany getCompany() {
        return getCompanyManager().getPublicCompany(companyName);
    }

    public int getShareUnits() {
        return shareUnits;
    }

    public int getShareUnit() {
        return shareUnit;
    }

    public int getShare() {
        return share;
    }

    public int getPresidentExchange() {
        return presidentExchange;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof SellShares)) return false;
        SellShares a = (SellShares) action;
        return a.getCompanyName().equals(companyName)
        && a.getShareUnits() == shareUnits
        && a.getNumber() == number
        && a.getPrice() == price;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof SellShares)) return false;
        SellShares a = (SellShares) action;
        return a.companyName.equals(companyName)
        && a.shareUnits == shareUnits
        && a.number == number
        && a.price == price;
    }

    @Override
    public String toString() {
        return "SellShares: "
        + number + " of " + share + "% " + companyName
        + " at " + Currency.format(company, shareUnits * price) + " apiece"
        + (presidentExchange > 0 ? " (pres.exch. for "+presidentExchange*shareUnit+"% share(s))" : "");
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        //in.defaultReadObject();
        // Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();

        companyName = (String) fields.get("companyName", null);
        shareUnit = fields.get("shareUnit", shareUnit);
        shareUnits = fields.get("shareUnits", shareUnits);
        share = fields.get("share", share);
        price = fields.get("price", price);
        numberSold = fields.get("numberSold", 0); // For backwards compatibility
        number = fields.get("number", numberSold);
        presidentExchange = fields.get("presidentExchange", 0);

        CompanyManager companyManager = getCompanyManager();
        if (Util.hasValue(companyName))
            companyName = companyManager.checkAlias(companyName);
        company = companyManager.getPublicCompany(companyName);
    }
}
