package rails.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.common.parser.Config;
import rails.common.parser.Configurable;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.model.WalletMoneyModel;
import rails.game.state.BooleanState;
import rails.game.state.Change;
import rails.game.state.Observable;
import rails.game.state.Triggerable;
import rails.game.state.UnknownOwner;
import rails.util.Util;

public class Bank extends RailsManager implements MoneyOwner, Configurable {

    public static final String ID = "BANK";
    
    /** Specific portfolio names */
    public static final String IPO_NAME = "IPO";
    public static final String POOL_NAME = "Pool";
    public static final String SCRAPHEAP_NAME = "ScrapHeap";
    public static final String UNAVAILABLE_NAME = "Unavailable";
    
    /** Default limit of shares in the bank pool */
    private static final int DEFAULT_BANK_AMOUNT = 12000;
    private static final String DEFAULT_MONEY_FORMAT = "$@";

    /** The Bank currency */
    private final Currency currency = Currency.create(this, "currency");

    /** The Bank's amount of cash */
    private final WalletMoneyModel cash = WalletMoneyModel.create(this, "cash", false, currency);
   
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
    
    // Instance initializer to create a BankBroken model
    {
        new Triggerable() {
            {// instance initializer
                cash.addTrigger(this);
            }
            public void triggered(Observable obs, Change change) {
                if (cash.value() <= 0 && !broken.value()) {
                    broken.set(true);
                    cash.setText(LocalText.getText("BROKEN"));
                    GameManager.getInstance().registerBrokenBank();
                }
            }
        };
    }
    
    protected static Logger log =
        LoggerFactory.getLogger(Bank.class);

    /**
     * Used by Configure (via reflection) only
     */
    public Bank(RailsRoot parent, String id) {
        // FIXME: This is a workaround to keep id to large caps
        super(parent, ID);
        // feedback to Railsroot
        parent.setCurrency(currency);
    }
    
    /**
     * @see rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Parse the Bank element

        /* First set the money format */
        String moneyFormat = null;
        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        } else { 
            /*
             * Only use the rails.game-specific format if it has not been
             * overridden in the configuration file (see if statement above)
             */
            Tag moneyTag = tag.getChild("Money");
            if (moneyTag != null) {
                moneyFormat = moneyTag.getAttributeAsString("format");
            }
        }
        /* Make sure that we have a format */
        if (!Util.hasValue(moneyFormat)) moneyFormat = DEFAULT_MONEY_FORMAT;
        currency.setFormat(moneyFormat);

        Tag bankTag = tag.getChild("Bank");
        if (bankTag != null) {
            // initialize bank from unknown owner
            UnknownOwner unknown = getRoot().getStateManager().getWalletManager().getUnkownOwner();
            int amount = bankTag.getAttributeAsInteger("amount", DEFAULT_BANK_AMOUNT);
            currency.move(unknown, amount, this);
        }

    }

    public void finishConfiguration (GameManager gameManager) {

        ReportBuffer.add(LocalText.getText("BankSizeIs", 
                currency.format(cash.value())));

        // finish configuration of BankPortfolios
        ipo.finishConfiguration();
        pool.finishConfiguration();
        unavailable.finishConfiguration();
        scrapHeap.finishConfiguration();
        
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
     * @return the currency instance
     */
    public Currency getCurrency() {
        return currency;
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

    public String toText() {
        return LocalText.getText("BANK");
    }
    
    // MoneyOwner interface
    public int getCash() {
        return cash.value();
    }

    public WalletMoneyModel getWallet() {
        return cash;
    }
    
    
}
