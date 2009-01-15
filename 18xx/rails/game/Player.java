/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Player.java,v 1.16 2009/01/15 20:53:28 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.model.*;
import rails.util.LocalText;

/**
 * Player class holds all player-specific data
 */

public class Player implements CashHolder, Comparable<Player> {

    /** Default limit to percentage of a company a player may hold */
    private static final int DEFAULT_PLAYER_SHARE_LIMIT = 60;

    public static int MAX_PLAYERS = 8;

    public static int MIN_PLAYERS = 2;

    private static int[] playerStartCash = new int[MAX_PLAYERS];

    private static int[] playerCertificateLimits = new int[MAX_PLAYERS];

    private static int playerCertificateLimit = 0;

    private static int playerShareLimit = DEFAULT_PLAYER_SHARE_LIMIT;
    // May need to become an array

    private String name = "";

    private int index = 0;

    private CashModel wallet = new CashModel(this);

    private CertCountModel certCount = new CertCountModel(this);

    private MoneyModel blockedCash;
    private CalculatedMoneyModel freeCash;
    private CalculatedMoneyModel worth;

    private boolean hasBoughtStockThisTurn = false;

    private Portfolio portfolio = null;

    public static void setLimits(int number, int cash, int certLimit) {
        if (number > 1 && number <= MAX_PLAYERS) {
            playerStartCash[number] = cash;
            playerCertificateLimits[number] = certLimit;
        }
    }

    /**
     * Initialises each Player's parameters which depend on the number of
     * players. To be called when all Players have been added.
     *
     */
    public static void initPlayers(List<Player> players) {
        int numberOfPlayers = players.size();
        int startCash = playerStartCash[numberOfPlayers];

        // Give each player the initial cash amount
        int index = 0;
        for (Player player : players) {
            player.index = index++;
            Bank.transferCash(null, player, startCash);
            ReportBuffer.add(LocalText.getText("PlayerIs",
                    index,
                    player.getName() ));
        }
        ReportBuffer.add(LocalText.getText("PlayerCash", Bank.format(startCash)));
        ReportBuffer.add(LocalText.getText("BankHas",
                Bank.format(Bank.getInstance().getCash())));

        // Set the sertificate limit
        playerCertificateLimit = playerCertificateLimits[numberOfPlayers];
    }

    /**
     * @return Certificate Limit for Players
     */
    public static int getCertLimit() {
        return playerCertificateLimit;
    }

    public static void setShareLimit(int percentage) {
        playerShareLimit = percentage;
    }

    public static int getShareLimit() {
        return playerShareLimit;
    }

    public Player(String name) {
        this.name = name;
        portfolio = new Portfolio(name, this);
        freeCash = new CalculatedMoneyModel(this, "getFreeCash");
        wallet.addDependent(freeCash);
        blockedCash = new MoneyModel(name + "_blockedCash");
        blockedCash.setOption(MoneyModel.SUPPRESS_ZERO);
        worth = new CalculatedMoneyModel(this, "getWorth");
        wallet.addDependent(worth);
    }

    public boolean isOverLimits() {

        // Over the total certificate hold Limit?
        if (portfolio.getNumberOfCountedCertificates() > playerCertificateLimit)
            return true;

        // Over the hold limit of any company?
        for (PublicCompanyI company : Game.getCompanyManager().getAllPublicCompanies()) {
            if (company.hasStarted() && company.hasStockPrice()
                && !mayBuyCompanyShare(company, 0)) return true;
        }

        return false;
    }

    /**
     * Check if a player may buy the given number of certificates.
     *
     * @param number Number of certificates to buy (usually 1 but not always
     * so).
     * @return True if it is allowed.
     */
    public boolean mayBuyCertificate(PublicCompanyI comp, int number) {
        if (comp.hasFloated() && comp.getCurrentSpace().isNoCertLimit())
            return true;
        if (portfolio.getNumberOfCountedCertificates() + number > playerCertificateLimit)
            return false;
        return true;
    }

    /**
     * Check if a player may buy the given number of shares from a given
     * company, given the "hold limit" per company, that is the percentage of
     * shares of one company that a player may hold (typically 60%).
     *
     * @param company The company from which to buy
     * @param number The number of shares (usually 1 but not always so).
     * @return True if it is allowed.
     */
    public boolean mayBuyCompanyShare(PublicCompanyI company, int number) {
        // Check for per-company share limit
        if (portfolio.getShare(company) + number * company.getShareUnit() > playerShareLimit
            && !company.getCurrentSpace().isNoHoldLimit()) return false;
        return true;
    }

    /**
     * Return the number of <i>additional</i> shares of a certain company and
     * of a certain size that a player may buy, given the share "hold limit" per
     * company, that is the percentage of shares of one company that a player
     * may hold (typically 60%). <p>If no hold limit applies, it is taken to be
     * 100%.
     *
     * @param company The company from which to buy
     * @param number The share unit (typically 10%).
     * @return The maximum number of such shares that would not let the player
     * overrun the per-company share hold limit.
     */
    public int maxAllowedNumberOfSharesToBuy(PublicCompanyI company,
            int shareSize) {

        int limit;
        if (!company.hasStarted()) {
            limit = playerShareLimit;
        } else {
            limit =
                    company.getCurrentSpace().isNoHoldLimit() ? 100
                            : playerShareLimit;
        }
        return (limit - portfolio.getShare(company)) / shareSize;
    }

    /**
     * @return Returns the player's portfolio.
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * @return Returns the player's name.
     */
    public String getName() {
        return name;
    }

    public String getNameAndPriority() {
        return name + (GameManager.getInstance().getPriorityPlayer() == this ? " PD" : "");
    }

    /**
     * @return Returns the player's wallet.
     */
    public int getCash() {
        return wallet.getCash();
    }

    public ModelObject getCashModel() {
        return wallet;
    }

    public boolean addCash(int amount) {
        boolean result = wallet.addCash(amount);
        return result;
    }

    /**
     * Get the player's total worth.
     *
     * @return Total worth
     */
    public int getWorth() {
        int worth = wallet.getCash();

        for (PublicCertificateI cert : portfolio.getCertificates()) {
            worth += cert.getCompany().getMarketPrice();
        }
        for (PrivateCompanyI priv : portfolio.getPrivateCompanies()) {
            worth += priv.getBasePrice();
        }
        return worth;
    }

    public CalculatedMoneyModel getWorthModel() {
        return worth;
    }

    public CertCountModel getCertCountModel() {
        return certCount;
    }

    public CalculatedMoneyModel getFreeCashModel() {
        return freeCash;
    }

    public MoneyModel getBlockedCashModel() {
        return blockedCash;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @return Returns the hasBoughtStockThisTurn.
     */
    public boolean hasBoughtStockThisTurn() {
        return hasBoughtStockThisTurn;
    }

    /**
     * Block cash allocated by a bid.
     *
     * @param amount Amount of cash to be blocked.
     * @return false if the amount was not available.
     */
    public boolean blockCash(int amount) {
        if (amount > wallet.getCash() - blockedCash.intValue()) {
            return false;
        } else {
            blockedCash.add(amount);
            freeCash.update();
            return true;
        }
    }

    /**
     * Unblock cash.
     *
     * @param amount Amount to be unblocked.
     * @return false if the given amount was not blocked.
     */
    public boolean unblockCash(int amount) {
        if (amount > blockedCash.intValue()) {
            return false;
        } else {
            blockedCash.add(-amount);
            freeCash.update();
            return true;
        }
    }

    /**
     * Return the unblocked cash (available for bidding)
     *
     * @return
     */
    public int getFreeCash() {
        return wallet.getCash() - blockedCash.intValue();
    }

    public int getBlockedCash() {
        return blockedCash.intValue();
    }

    public int getIndex() {
        return index;
    }

    /**
     * Compare Players by their total worth, in descending order. This method
     * implements the Comparable interface.
     */
    public int compareTo(Player p) {
        return -new Integer(getWorth()).compareTo(new Integer(p.getWorth()));
    }
}
