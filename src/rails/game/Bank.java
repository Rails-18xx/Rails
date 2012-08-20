package rails.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.common.parser.Config;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.model.CashMoneyModel;
import rails.game.model.CashOwner;
import rails.game.state.AbstractItem;
import rails.game.state.BooleanState;
import rails.game.state.Configurable;
import rails.game.state.Item;
import rails.util.*;

public class Bank extends AbstractItem implements CashOwner, Configurable {

    private static Bank instance = null;

    /** Specific portfolio names */
    public static final String IPO_NAME = "IPO";
    public static final String POOL_NAME = "Pool";
    public static final String SCRAPHEAP_NAME = "ScrapHeap";
    public static final String UNAVAILABLE_NAME = "Unavailable";
    
    /** Default limit of shares in the bank pool */
    private static final int DEFAULT_BANK_AMOUNT = 12000;
    private static final String DEFAULT_MONEY_FORMAT = "$@";

    /** The Bank's amount of cash */
    private final CashMoneyModel cash = CashMoneyModel.create(this, "cash", false);
   
    /** The IPO */
    private final BankPortfolio ipo = BankPortfolio.create(this, IPO_NAME);
    /** The Bank Pool */
    private final BankPortfolio pool = BankPortfolio.create(this, POOL_NAME);
    /** Collection of items that will (may) become available in the future */
    private final BankPortfolio unavailable = BankPortfolio.create(this, UNAVAILABLE_NAME);
    /** Collection of items that have been discarded (but are kept to allow Undo) */
    private final BankPortfolio scrapHeap = BankPortfolio.create(this, SCRAPHEAP_NAME);

    /** Is the bank broken */
    private final BooleanState broken = BooleanState.create(this, "broken");

    /**
     * The money format template. '@' is replaced by the numeric amount, the
     * rest is copied.
     */
    private String moneyFormat = null;

    protected static Logger log =
        LoggerFactory.getLogger(Bank.class.getPackage().getName());

    /**
     * Used by Configure (via reflection) only
     */
    public Bank(Item parent, String id) {
        super(parent, id);
        instance = this;

        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        }
    }
    
    /**
     * @see rails.game.state.Configurable#configureFromXML(org.w3c.dom.Element)
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
            cash.set(bankTag.getAttributeAsInteger("amount",
                    DEFAULT_BANK_AMOUNT));
        }

    }

    public void finishConfiguration (GameManager gameManager) {

        ReportBuffer.add(LocalText.getText("BankSizeIs",
                format(cash.value())));

        // Add privates
        List<PrivateCompany> privates =
            gameManager.getCompanyManager().getAllPrivateCompanies();
        for (PrivateCompany priv : privates) {
            ipo.getPortfolioModel().addPrivateCompany(priv);
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
    public BankPortfolio getIpo() {
        return ipo;
    }

    public BankPortfolio getScrapHeap() {
        return scrapHeap;
    }

    /* FIXME: Add broken check somewhere
         * Check if the bank has broken. In some games <0 could apply, so this
         * will become configurable.
        if (cash.value() <= 0 && !broken.booleanValue()) {
            broken.set(true);
            cash.setText(LocalText.getText("BROKEN"));
            GameManager.getInstance().registerBrokenBank();
        }
        */

    /**
     * @return Portfolio of stock in Bank Pool
     */
    public BankPortfolio getPool() {
        return pool;
    }

    /**
     * @return Portfolio of unavailable shares
     */
    public BankPortfolio getUnavailable() {
        return unavailable;
    }

    public String getId() {
        return LocalText.getText("BANK");
    }

    // CashOwner interface
    public int getCash() {
        return cash.value();
    }

    public static String format(int amount) {
        // Replace @ with the amount
        String result = instance.moneyFormat.replaceFirst("@", String.valueOf(amount));
        // Move any minus to the front
        if (amount < 0) result = result.replaceFirst("(.+)-", "-$1");
        return result;
    }

    public static String formatIntegerArray(int[] amountList) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < amountList.length;++i) {
            if (!(i == 0)) result.append(",");
            result.append(format(amountList[i]));
        }
        return result.toString();
    }

    public CashMoneyModel getCashModel() {
        return cash;
    }
    
}
