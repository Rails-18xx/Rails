package rails.game;

import java.util.List;

import org.apache.log4j.Logger;

import rails.game.model.CashModel;
import rails.game.model.ModelObject;
import rails.game.state.BooleanState;
import rails.util.*;

public class Bank implements CashHolder, ConfigurableComponentI {

    /** Default limit of shares in the bank pool */
    private static final int DEFAULT_BANK_AMOUNT = 12000;
    private static final String DEFAULT_MONEY_FORMAT = "$@";

    /** The Bank's amount of cash */
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
    private BooleanState broken = new BooleanState("Bank.broken", false);

    /**
     * The money format template. '@' is replaced by the numeric amount, the
     * rest is copied.
     */
    private String moneyFormat = null;

    protected static Logger log =
        Logger.getLogger(Bank.class.getPackage().getName());

    public Bank() {

        instance = this;

        money = new CashModel(this);
        // Create the IPO and the Bank Pool.
        ipo = new Portfolio(Portfolio.IPO_NAME, this);
        pool = new Portfolio(Portfolio.POOL_NAME, this);
        unavailable = new Portfolio(Portfolio.UNAVAILABLE_NAME, this);
        scrapHeap = new Portfolio(Portfolio.SCRAPHEAP_NAME, this);

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

    }

    public void finishConfiguration (GameManagerI gameManager) {

        ReportBuffer.add(LocalText.getText("BankSizeIs",
                format(money.getCash())));

        // Add privates
        List<PrivateCompanyI> privates =
            gameManager.getCompanyManager().getAllPrivateCompanies();
        for (PrivateCompanyI priv : privates) {
            ipo.addPrivate(priv, -1);
        }

        // Add public companies
        List<PublicCompanyI> companies =
            gameManager.getCompanyManager().getAllPublicCompanies();
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
    public void addCash(int amount) {

        money.addCash(amount);

        /*
         * Check if the bank has broken. In some games <0 could apply, so this
         * will become configurable.
         */
        if (money.getCash() <= 0 && !broken.booleanValue()) {
            broken.set(true);
            money.setText(LocalText.getText("BROKEN"));
            GameManager.getInstance().registerBrokenBank();
        }
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

    public static String format(int amount) {
        // Replace @ with the amount
        String result = instance.moneyFormat.replaceFirst("@", String.valueOf(amount));
        // Move any minus to the front
        if (amount < 0) result = result.replaceFirst("(.+)-", "-$1");
        return result;
    }
    // start sfy 1889 for integerarrays
    public static String formatIntegerArray(int[] amountList) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < amountList.length;++i) {
            if (!(i == 0)) result.append(",");
            result.append(format(amountList[i]));
        }
        return result.toString();
    }
    // end sfy 1889
}
