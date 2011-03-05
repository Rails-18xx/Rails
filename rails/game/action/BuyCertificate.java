/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyCertificate.java,v 1.12 2009/11/04 20:33:22 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class BuyCertificate extends PossibleAction {

    // Server-side settings

    /* Some obsolete properties, which are only retained for backwards compatibility
     * (i.e. to remain able to load older saved files).
     * The certificate was in fact only used to find the below replacement
     * attributes. It was NOT actually used to select the bought certificate!
     */
    transient protected PublicCertificateI certificate = null;
    protected String certUniqueId = null;

    /* Replacement for the above.*/
    transient protected PublicCompanyI company;
    protected String companyName;
    protected int sharePerCert; // Share % per buyable certificate.

    transient protected Portfolio from;
    protected String fromName; // Old: portfolio name. New: portfolio unique name.
    protected int price;
    protected int maximumNumber;

    // Client-side settings
    protected int numberBought = 0;

    public static final long serialVersionUID = 1L;

    public BuyCertificate(PublicCompanyI company, int sharePerCert,
            Portfolio from,
            int price, int maximumNumber) {
        this.company = company;
        this.sharePerCert = sharePerCert;
        this.from = from;
        this.fromName = from.getUniqueName();
        this.price = price;
        this.maximumNumber = maximumNumber;

        companyName = company.getName();
    }

    /** Buy a certificate from some portfolio at a given price */
    public BuyCertificate(PublicCompanyI company, int sharePerCert,
            Portfolio from,
            int price) {
        this(company, sharePerCert, from, price, 1);
    }

    /** Required for deserialization */
    public BuyCertificate() {}

    public Portfolio getFromPortfolio() {
        return from;
    }

    /**
     * @return Returns the maximumNumber.
     */
    public int getMaximumNumber() {
        return maximumNumber;
    }

    /**
     * @return Returns the price.
     */
    public int getPrice() {
        return price;
    }

    public PublicCompanyI getCompany() {
        return company;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getSharePerCertificate() {
        return sharePerCert;
    }

    public int getSharesPerCertificate() {
        return sharePerCert / company.getShareUnit();
    }

    public int getNumberBought() {
        return numberBought;
    }

    public void setNumberBought(int numberBought) {
        this.numberBought = numberBought;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof BuyCertificate)) return false;
        BuyCertificate a = (BuyCertificate) action;
        return a.certificate == certificate && a.from == from
               && a.price == price && a.maximumNumber == maximumNumber;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof BuyCertificate)) return false;
        BuyCertificate a = (BuyCertificate) action;
        return a.certificate == certificate && a.from == from
               && a.price == price && a.numberBought == numberBought;
    }

    @Override
    public String toString() {
        StringBuffer text = new StringBuffer();
        text.append("BuyCertificate: ");
        if (acted) text.append("Bought "+numberBought +" of ");
        if (maximumNumber > 1) text.append ("max."+maximumNumber+" of ");
        text.append(sharePerCert).append("% ").append(companyName)
            .append(" from ").append(from.getName())
            .append(" price=").append(Bank.format((sharePerCert/company.getShareUnit()) * price));
        return text.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        //in.defaultReadObject();
        // Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();

        certUniqueId = (String) fields.get("certUniqueId", null);
        companyName = (String) fields.get("companyName", null);
        fromName = (String) fields.get("fromName", fromName);
        price = fields.get("price", price);
        maximumNumber = fields.get("maximumNumber", maximumNumber);
        sharePerCert = fields.get("sharePerCert", -1);

        numberBought = fields.get("numberBought", numberBought);

        GameManagerI gameManager = GameManager.getInstance();

        /* Check for aliases (old company names) */
        CompanyManagerI companyManager = gameManager.getCompanyManager();
        companyName = companyManager.checkAlias (companyName);

        if (certUniqueId != null) {
            // Old style
            certUniqueId = companyManager.checkAliasInCertId(certUniqueId);
            certificate = PublicCertificate.getByUniqueId(certUniqueId);
            from = gameManager.getPortfolioByName(fromName);
            company = certificate.getCompany();
            companyName = company.getName();
            sharePerCert = certificate.getShare();
        } else if (companyName != null) {
            // New style (since Rails.1.3.1)
            company = gameManager.getCompanyManager().getPublicCompany(companyName);
            from = gameManager.getPortfolioByUniqueName(fromName);
            // We don't need the certificate anymore.
        }


    }
}
