package rails.game;

import java.util.*;

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
    private static CashModel money;

    private static int gameType;

    /** The IPO */
    private static Portfolio ipo = null;
    /** The Bank Pool */
    private static Portfolio pool = null;
    /** Collection of items that will (may) become available in the future */
    private static Portfolio unavailable = null;
    /** Collection of items that have bene discarded (but are kept to allow Undo) */
    private static Portfolio scrapHeap = null;

    private static Bank instance = null;

    /** Is the bank broken (remains true once set) */
    private static boolean broken = false;
    /** Is the bank just broken (returns true exactly once) */
    private static boolean brokenReported = false;

    /**
     * The money format template. '@' is replaced by the numeric amount, the
     * rest is copied.
     */
    private static String moneyFormat = null;

    static {
        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        }
    }

    private static int poolShareLimit = DEFAULT_POOL_SHARE_LIMIT;

    protected static Logger log =
            Logger.getLogger(Bank.class.getPackage().getName());

    /**
     * @return an instance of the Bank object
     */
    public static Bank getInstance() {
        return instance;
    }

    /**
     * Central method for transferring all cash.
     * 
     * @param from Who pays the money (null = Bank).
     * @param to Who received the money (null = Bank).
     * @param amount The amount of money.
     */
    public static boolean transferCash(CashHolder from, CashHolder to,
            int amount) {
        if (from == null)
            from = instance;
        else if (to == null) to = instance;
        to.addCash(amount);
        return from.addCash(-amount);
    }

    public Bank() {

        instance = this;
        money = new CashModel(this);
        // Create the IPO and the Bank Pool.
        ipo = new Portfolio("IPO", this);
        pool = new Portfolio("Pool", this);
        unavailable = new Portfolio("Unavailable", this);
        scrapHeap = new Portfolio("ScrapHeap", this);

    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        int number, startCash, certLimit;

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

        List<Tag> playerTags = tag.getChildren("Players");
        int minPlayers = 99;
        int maxPlayers = 0;
        for (Tag playerTag : playerTags) {
            number = playerTag.getAttributeAsInteger("number");
            startCash = playerTag.getAttributeAsInteger("cash");
            certLimit = playerTag.getAttributeAsInteger("certLimit");

            Player.setLimits(number, startCash, certLimit);

            minPlayers = Math.min(minPlayers, number);
            maxPlayers = Math.max(maxPlayers, number);
        }
        Player.MIN_PLAYERS = minPlayers;
        Player.MAX_PLAYERS = maxPlayers;
    }

    /**
     * @param percentage of a company allowed to be in the Bank pool.
     */
    public static void setPoolShareLimit(int percentage) {
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
     * @return Which type of rails.game we're playing (1830, 1856, 1870, etc.)
     */
    public static int getGameType() {
        return gameType;
    }

    /**
     * @return IPO Portfolio
     */
    public static Portfolio getIpo() {
        return ipo;
    }

    public static Portfolio getScrapHeap() {
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

    public static boolean isBroken() {
        return broken;
    }

    public static boolean isJustBroken() {
        boolean result = broken && !brokenReported;
        brokenReported = true;
        return result;
    }

    /**
     * @return Portfolio of stock in Bank Pool
     */
    public static Portfolio getPool() {
        return pool;
    }

    /**
     * @return Portfolio of unavailable shares
     */
    public static Portfolio getUnavailable() {
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
    public static int getPoolShareLimit() {
        return poolShareLimit;
    }

    public static String format(int amount) {
        return moneyFormat.replaceFirst("@", String.valueOf(amount));
    }

}
