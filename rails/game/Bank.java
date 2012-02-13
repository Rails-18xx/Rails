package rails.game;

import java.util.List;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.Config;
import rails.common.parser.ConfigurableComponentI;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.model.CashMoneyModel;
import rails.game.model.CashOwner;
import rails.game.model.MoneyModel;
import rails.game.model.PortfolioModel;
import rails.game.state.AbstractItem;
import rails.game.state.BooleanState;
import rails.game.state.Item;
import rails.util.*;

public class Bank extends AbstractItem implements CashOwner, ConfigurableComponentI {

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
    private final CashMoneyModel cash;

    /** The IPO */
    private final PortfolioModel ipo;
    /** The Bank Pool */
    private final PortfolioModel pool;
    /** Collection of items that will (may) become available in the future */
    private final PortfolioModel unavailable;
    /** Collection of items that have been discarded (but are kept to allow Undo) */
    private final PortfolioModel scrapHeap;

    /** Is the bank broken */
    private final BooleanState broken;

    /**
     * The money format template. '@' is replaced by the numeric amount, the
     * rest is copied.
     */
    private String moneyFormat = null;

    protected static Logger log =
        Logger.getLogger(Bank.class.getPackage().getName());

    public Bank() {
        super(Bank.class.getSimpleName());

        instance = this;

        cash = MoneyModel.createCash("cash");

        // Create the IPO and the Bank Pool.
        ipo = PortfolioModel.create(IPO_NAME);
        pool = PortfolioModel.create(POOL_NAME);
        unavailable = PortfolioModel.create(UNAVAILABLE_NAME);
        scrapHeap = PortfolioModel.create(SCRAPHEAP_NAME);

        broken = BooleanState.create("broken");
        
        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        }
    }

    @Override
    public Bank init(Item parent) {
        super.init(parent);
        
        cash.init(this);
        ipo.init(this);
        pool.init(this);
        unavailable.init(this);
        scrapHeap.init(this);
        broken.init(this);
        
        return this;
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
            ipo.addPrivate(priv, -1);
        }

        // Add public companies
        List<PublicCompany> companies =
            gameManager.getCompanyManager().getAllPublicCompanies();
        for (PublicCompany comp : companies) {
            for (PublicCertificate cert : comp.getCertificates()) {
                if (cert.isInitiallyAvailable()) {
                    ipo.moveInto(cert);
                } else {
                    unavailable.moveInto(cert);
                }
            }
        }
    }

    /**
     * @return IPO Portfolio
     */
    public PortfolioModel getIpo() {
        return ipo;
    }

    public PortfolioModel getScrapHeap() {
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
    public PortfolioModel getPool() {
        return pool;
    }

    /**
     * @return Portfolio of unavailable shares
     */
    public PortfolioModel getUnavailable() {
        return unavailable;
    }

    public String getId() {
        return LocalText.getText("BANK");
    }

    // CashOwner interface
    public CashMoneyModel getCash() {
        return cash;
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
    
}
