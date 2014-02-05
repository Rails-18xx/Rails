package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.*;
import rails.game.model.PortfolioModel;
import rails.game.model.PortfolioOwner;

public class BuyCertificate extends PossibleAction {

    // Server-side settings

    /* Some obsolete properties, which are only retained for backwards compatibility
     * (i.e. to remain able to load older saved files).
     * The certificate was in fact only used to find the below replacement
     * attributes. It was NOT actually used to select the bought certificate!
     */
    transient protected PublicCertificate certificate = null;
    protected String certUniqueId = null;

    /* Replacement for the above.*/
    transient protected PublicCompany company;
    protected String companyName;
    protected int sharePerCert; // Share % per buyable certificate.

    // FIXME: We have to recreate the portfolio name
    transient protected PortfolioModel from;
    protected String fromName; // Old: portfolio name. New: portfolio unique name.
    protected int price;
    protected int maximumNumber;

    // Client-side settings
    protected int numberBought = 0;

    public static final long serialVersionUID = 1L;

    public BuyCertificate(PublicCompany company, int sharePerCert,
            PortfolioOwner from,
            int price, int maximumNumber) {
        this.company = company;
        this.sharePerCert = sharePerCert;
        this.from = from.getPortfolioModel();
        // FIXME: From used to be a Portfolio(model) with unique name to identify
        // this.fromName = from.getUniqueName();
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

    /** Required for deserialization */
    public BuyCertificate() {}

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

    public PublicCompany getCompany() {
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
        if (!(action.getClass() == BuyCertificate.class)) return false;
        BuyCertificate a = (BuyCertificate) action;
        return a.company == company && a.from == from
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
            .append(" from ").append(from.getUniqueName())
            .append(" price=").append(Currency.format(company, (sharePerCert/company.getShareUnit()) * price));
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

        RailsRoot root = RailsRoot.getInstance();

        /* Check for aliases (old company names) */
        CompanyManager companyManager = root.getCompanyManager();
        companyName = companyManager.checkAlias (companyName);

        if (certUniqueId != null) {
            // Old style
            certUniqueId = companyManager.checkAliasInCertId(certUniqueId);
            certificate = PublicCertificate.getByUniqueId(certUniqueId);
            // TODO: This function needs a compatible replacement 
            from = getGameManager().getPortfolioByName(fromName);
            company = certificate.getCompany();
            companyName = company.getId();
            sharePerCert = certificate.getShare();
        } else if (companyName != null) {
            // New style (since Rails.1.3.1)
            company = root.getCompanyManager().getPublicCompany(companyName);
            // TODO: This function needs a compatible replacement 
            from = getGameManager().getPortfolioByUniqueName(fromName);
            // We don't need the certificate anymore.
        }


    }
}
