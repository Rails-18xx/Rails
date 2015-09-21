package net.sf.rails.game.financial;

import java.util.List;

import net.sf.rails.common.Config;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.model.PurseMoneyModel;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Change;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.CurrencyOwner;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Purse;
import net.sf.rails.game.state.Triggerable;
import net.sf.rails.game.state.UnknownOwner;
import net.sf.rails.util.Util;

public class Bank extends RailsManager implements CurrencyOwner, RailsMoneyOwner, Configurable {

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
    private final PurseMoneyModel cash = PurseMoneyModel.create(this, "cash", false, currency);
   
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
                    getRoot().getGameManager().registerBrokenBank();
                }
            }
        };
    }
    
    /**
     * Used by Configure (via reflection) only
     */
    public Bank(RailsRoot parent, String id) {
        // FIXME: This is a workaround to keep id to large caps
        super(parent, ID);
    }
    
    /**
     * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
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

    public void finishConfiguration (RailsRoot root) {

        ReportBuffer.add(this, LocalText.getText("BankSizeIs", 
                currency.format(cash.value())));

        // finish configuration of BankPortfolios
        ipo.finishConfiguration();
        pool.finishConfiguration();
        unavailable.finishConfiguration();
        scrapHeap.finishConfiguration();
        
        // Add privates
        List<PrivateCompany> privates =
            root.getCompanyManager().getAllPrivateCompanies();
        for (PrivateCompany priv : privates) {
            ipo.getPortfolioModel().addPrivateCompany(priv);
        }

        // Add public companies
        List<PublicCompany> companies =
            root.getCompanyManager().getAllPublicCompanies();
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
    
    // CurrencyOwner interface
    public Currency getCurrency() {
        return currency;
    }

    // MoneyOwner interface
    public Purse getPurse() {
        return cash.getPurse();
    }
    
    public int getCash() {
        return cash.getPurse().value();
    }
    
    public static String format(RailsItem item, Iterable<Integer> amount) {
        Currency currency = item.getRoot().getBank().getCurrency();
        return currency.format(amount);
    }

    public static String format(RailsItem item, int amount) {
        Currency currency = item.getRoot().getBank().getCurrency();
        return currency.format(amount);
    }
    
    public static Bank get(RailsItem item) {
        return item.getRoot().getBank();
    }
    
    public static BankPortfolio getIpo(RailsItem item) {
        return get(item).ipo;
    }
    
    public static BankPortfolio getPool(RailsItem item) {
        return get(item).pool;
    }
    
    public static BankPortfolio getScrapHeap(RailsItem item) {
        return get(item).scrapHeap;
    }
    
    public static BankPortfolio getUnavailable(RailsItem item) {
        return get(item).unavailable;
    }

}
