package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import net.sf.rails.util.GameLoader;
import rails.game.specific._1880.StartCompany_1880;

import com.google.common.base.Objects;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.util.RailsObjects;

/**
 *
 * Rails 2.0: Updated equals and toString methods
 */
public class BuyCertificate extends PossibleAction {

    // Server-side settings

    /* Some obsolete properties, which are only retained for backwards compatibility
     * (i.e. to remain able to load older saved files).
     * The certificate was in fact only used to find the below replacement
     * attributes. It was NOT actually used to select the bought certificate!
     */
    protected transient PublicCertificate certificate = null;
    protected String certUniqueId = null;

    /* Replacement for the above.*/
    protected transient PublicCompany company;
    protected String companyName;
    protected int sharePerCert; // Share % per buyable certificate.

    // FIXME: We have to recreate the portfolio name
    protected transient PortfolioModel from;
    protected String fromName; // Old: portfolio name. New: portfolio unique name.
    protected int price;
    protected int maximumNumber;

    // In exceptional cases, a president's share may end up in the pool and be buyable.
    // This can occur after a bankruptcy in 1835 and 18Scan
    protected boolean president = false;

    // Client-side settings
    protected int numberBought = 0;

    public static final long serialVersionUID = 1L;

    public BuyCertificate(PublicCompany company, int sharePerCert,
            PortfolioOwner from,
            int price, int maximumNumber) {
        super(company.getRoot()); // not defined by an activity yet
        this.company = company;
        this.sharePerCert = sharePerCert;
        if ( from == null ) {
            from = root.getBank().getIpo();
        }
        this.from = from.getPortfolioModel();
        this.fromName = this.from.getUniqueName();
        this.price = price;
        this.maximumNumber = maximumNumber;

        companyName = company.getId();
    }

    /** Buy a certificate from some owner at a given price */
    public BuyCertificate(PublicCompany company, int sharePerCert,
            PortfolioOwner from,
            int price) {
        this(company, sharePerCert, from, price, 1);
    }

    public PortfolioModel getFromPortfolio() {
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

    /* Only to enable fixing an action in ListAndFixSavedFiles */
    public void setPrice(int price) {
        this.price = price;
    }

    public PublicCompany getCompany() {
        return company;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getSharePerCertificate() {
        return sharePerCert;
    }

    /* Only to enable fixing an action in ListAndFixSavedFiles */
    public void setSharePerCert(int sharePerCert) {
        this.sharePerCert = sharePerCert;
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

    public boolean isPresident() {
        return president;
    }

    public void setPresident (boolean newValue) {
        this.president = newValue;
    }

    /* Only to enable fixing an action in ListAndFixSavedFiles */
    public void setFromByName(String from) {
        this.fromName = from;
        this.from = root.getPortfolioManager().getPortfolioByUniqueName(from);
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        BuyCertificate action = (BuyCertificate)pa;
        boolean options =
                // TODO: This is commented out as the certificate is not required anymore
                // Objects.equal(this.certificate, action.certificate)
                Objects.equal(this.company, action.company)
                // In StartCompany_1880 the sharePerCert can differ
                && (Objects.equal(this.sharePerCert, action.sharePerCert) || this instanceof StartCompany_1880)
                && Objects.equal(this.from, action.from)
                // In StartCompany the price can differ
                && (Objects.equal(this.price, action.price) || this instanceof StartCompany)
                && Objects.equal(this.maximumNumber, action.maximumNumber)
        ;

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
                && Objects.equal(this.numberBought, action.numberBought)
        ;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("certificate", certificate)
                    .addToString("company", company)
                    .addToString("sharePerCert", sharePerCert)
                    .addToString("from", from)
                    .addToString("price", price)
                    .addToString("maximumNumber", maximumNumber)
                    .addToString("president", president)
                    .addToStringOnlyActed("numberBought", numberBought)
                    .toString()
        ;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (in instanceof GameLoader.RailsObjectInputStream) {
            // Custom reading for backwards compatibility
            ObjectInputStream.GetField fields = in.readFields();

            certUniqueId = (String) fields.get("certUniqueId", null);
            companyName = (String) fields.get("companyName", null);
            fromName = (String) fields.get("fromName", fromName);
            price = fields.get("price", price);
            maximumNumber = fields.get("maximumNumber", maximumNumber);
            sharePerCert = fields.get("sharePerCert", -1);
            president = fields.get("president", false);

            numberBought = fields.get("numberBought", numberBought);

            /* Check for aliases (old company names) */
            CompanyManager companyManager = root.getCompanyManager();
            companyName = companyManager.checkAlias(companyName);

            if (certUniqueId != null) {
                // Old style
                certUniqueId = companyManager.checkAliasInCertId(certUniqueId);
                certificate = getRoot().getCertificateManager().getCertificate(certUniqueId);
                from = root.getPortfolioManager().getPortfolioByName(fromName);
                company = certificate.getCompany();
                companyName = company.getId();
                sharePerCert = certificate.getShare();
            } else if (companyName != null) {
                // New style (since Rails.1.3.1)
                company = root.getCompanyManager().getPublicCompany(companyName);
                from = root.getPortfolioManager().getPortfolioByUniqueName(fromName);
                // We don't need the certificate anymore.
            }
        } else {
            in.defaultReadObject();
        }
    }

    public void applyRailsRoot(RailsRoot root) {
        super.applyRailsRoot(root);

        /* Check for aliases (old company names) */
        CompanyManager companyManager = root.getCompanyManager();
        companyName = companyManager.checkAlias(companyName);

        if (certUniqueId != null) {
            // Old style
            certUniqueId = companyManager.checkAliasInCertId(certUniqueId);
            certificate = getRoot().getCertificateManager().getCertificate(certUniqueId);
            from = root.getPortfolioManager().getPortfolioByName(fromName);
            company = certificate.getCompany();
            companyName = company.getId();
            sharePerCert = certificate.getShare();
        } else if (companyName != null) {
            // New style (since Rails.1.3.1)
            company = root.getCompanyManager().getPublicCompany(companyName);
            from = root.getPortfolioManager().getPortfolioByUniqueName(fromName);
            // We don't need the certificate anymore.
        }
    }
}
