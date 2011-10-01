package rails.game;

import java.util.List;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.Config;
import rails.common.parser.ConfigurableComponentI;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.model.CashModel;
import rails.game.model.CashOwner;
import rails.game.model.Portfolio;
import rails.game.state.AbstractItem;
import rails.game.state.BooleanState;
import rails.util.*;

public class Bank extends AbstractItem implements CashOwner, ConfigurableComponentI {

    /** Specific portfolio names */
    public static final String IPO_NAME = "IPO";
    public static final String POOL_NAME = "Pool";
    public static final String SCRAPHEAP_NAME = "ScrapHeap";
    public static final String UNAVAILABLE_NAME = "Unavailable";
    
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
    private BooleanState broken = new BooleanState(this, "Bank.broken", false);

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
        ipo = new Portfolio(ipo, IPO_NAME);
        pool = new Portfolio(pool, POOL_NAME);
        unavailable = new Portfolio(unavailable, UNAVAILABLE_NAME);
        scrapHeap = new Portfolio(scrapHeap, SCRAPHEAP_NAME);

        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        }
    }

    /**
     * @see rails.common.parser.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
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
            money.set(bankTag.getAttributeAsInteger("amount",
                    DEFAULT_BANK_AMOUNT));
        }

    }

    public void finishConfiguration (GameManager gameManager) {

        ReportBuffer.add(LocalText.getText("BankSizeIs",
                format(money.value())));

        // Add privates
        List<PrivateCompany> privates =
            gameManager.getCompanyManager().getAllPrivateCompanies();
        for (PrivateCompany priv : privates) {
            ipo.addPrivate(priv, -1);
        }

        // Add public companies
        List<PublicCompany> companies =
            gameManager.getCompanyManager().getAllPublicCompanies();
        for (PublicCompany comp : companies) {
            for (PublicCertificate cert : comp.getCertificates()) {
                if (cert.isInitiallyAvailable()) {
                    cert.moveTo(ipo);
                } else {
                    cert.moveTo(unavailable);
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
    public int getCashValue() {
        return money.value();
    }

    /**
     * Adds cash back to the bank
     */
    public void addCash(int amount) {

        money.add(amount);

        /*
         * Check if the bank has broken. In some games <0 could apply, so this
         * will become configurable.
         */
        if (money.value() <= 0 && !broken.booleanValue()) {
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
        money.set(i);
    }

    public String getId() {
        return LocalText.getText("BANK");
    }

    public String getFormattedCash() {
        return money.getData();
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
    
    public int getCash() {
        return money.value();
    }

    public CashModel getCashModel() {
        return money;
    }
    
}
