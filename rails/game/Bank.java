package rails.game;

import java.util.List;

import org.apache.log4j.Logger;

import rails.game.model.CashModel;
import rails.game.model.ModelObject;
import rails.util.*;

public class Bank implements CashHolder, ConfigurableComponentI {

    /** Default limit of shares in the bank pool */
    private static final int DEFAULT_POOL_SHARE_LIMIT = 50;
    private static final int DEFAULT_BANK_AMOUNT = 12000;
    private static final String DEFAULT_MONEY_FORMAT = "$@";

    /** The Bank's amont of cash */
    private CashModel money;

    /** The IPO */
    private Portfolio ipo = null;
    /** The Bank Pool */
    private Portfolio pool = null;
    /** Collection of items that will (may) become available in the future */
    private Portfolio unavailable = null;
    /** Collection of items that have been discarded (but are kept to allow Undo) */
    private Portfolio scrapHeap = null;

    private static Bank instance = null;

    /** Is the bank broken (remains true once set) */
    private boolean broken = false;
    /** Is the bank just broken (returns true exactly once) */
    private boolean brokenReported = false;

    /**
     * The money format template. '@' is replaced by the numeric amount, the
     * rest is copied.
     */
    private String moneyFormat = null;

    private int poolShareLimit = DEFAULT_POOL_SHARE_LIMIT;

    protected static Logger log =
            Logger.getLogger(Bank.class.getPackage().getName());

    /**
     * @return an instance of the Bank object
     */
    public static Bank getInstance() {
        return instance;
    }

    public Bank() {

        instance = this;

        money = new CashModel(this);
        // Create the IPO and the Bank Pool.
        ipo = new Portfolio("IPO", this);
        pool = new Portfolio("Pool", this);
        unavailable = new Portfolio("Unavailable", this);
        scrapHeap = new Portfolio("ScrapHeap", this);

        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        }
    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Parse the Bank element

        /* First set the money format */
        if (moneyFormat == null) {
            /*
             * Only use the rails.game-specific format if it has not been
             * overridden in the configuration file (see static block above)
             */
            Tag moneyTag = tag.getChild("Money");
            if (moneyTag != null) {
                moneyFormat = moneyTag.getAttributeAsString("format");
            }
        }
        /* Make sure that we have a format */
        if (!Util.hasValue(moneyFormat)) moneyFormat = DEFAULT_MONEY_FORMAT;

        Tag bankTag = tag.getChild("Bank");
        if (bankTag != null) {
            money.setCash(bankTag.getAttributeAsInteger("amount",
                    DEFAULT_BANK_AMOUNT));
        }
        ReportBuffer.add(LocalText.getText("BankSizeIs",
                format(money.getCash())));

    }

    /**
     * @param percentage of a company allowed to be in the Bank pool.
     */
    public void setPoolShareLimit(int percentage) {
        poolShareLimit = percentage;
    }

    /**
     * Put all available certificates in the IPO
     */
    public void initCertificates() {
        // Add privates
        List<PrivateCompanyI> privates =
                Game.getCompanyManager().getAllPrivateCompanies();
        for (PrivateCompanyI priv : privates) {
            ipo.addPrivate(priv);
        }

        // Add public companies
        List<PublicCompanyI> companies =
                Game.getCompanyManager().getAllPublicCompanies();
        for (PublicCompanyI comp : companies) {
            for (PublicCertificateI cert : comp.getCertificates()) {
                if (cert.isInitiallyAvailable()) {
                    ipo.addCertificate(cert);
                 } else {
                    unavailable.addCertificate(cert);
               }
            }
        }
    }

    /**
     * @return IPO Portfolio
     */
    public Portfolio getIpo() {
        return ipo;
    }

    public Portfolio getScrapHeap() {
        return scrapHeap;
    }

    /**
     * @return Bank's current cash level
     */
    public int getCash() {
        return money.getCash();
    }

    /**
     * Adds cash back to the bank
     */
    public boolean addCash(int amount) {
        boolean negative = money.addCash(amount);

        /*
         * Check if the bank has broken. In some games <0 could apply, so this
         * will become configurable.
         */
        if (money.getCash() <= 0 && !broken) {
            broken = true;
            ReportBuffer.add(LocalText.getText("BankIsBroken"));
        }
        return negative;
    }

    public boolean isBroken() {
        return broken;
    }

    public boolean isJustBroken() {
        boolean result = broken && !brokenReported;
        brokenReported = true;
        return result;
    }

    /**
     * @return Portfolio of stock in Bank Pool
     */
    public Portfolio getPool() {
        return pool;
    }

    /**
     * @return Portfolio of unavailable shares
     */
    public Portfolio getUnavailable() {
        return unavailable;
    }

    /**
     * @param Set Bank's cash.
     */
    public void setCash(int i) {
        money.setCash(i);
    }

    public String getName() {
        return LocalText.getText("BANK");
    }

    public String getFormattedCash() {
        return money.toString();
    }

    public ModelObject getCashModel() {
        return money;
    }

    /**
     * Get the maximum share percentage that may be sold to the Bank Pool.
     *
     * @return The maximum percentage.
     */
    public int getPoolShareLimit() {
        return poolShareLimit;
    }

    public static String format(int amount) {
        return instance.moneyFormat.replaceFirst("@", String.valueOf(amount));
    }

}
