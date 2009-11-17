/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Player.java,v 1.22 2009/11/17 19:31:25 evos Exp $ */
package rails.game;

import rails.game.model.*;

/**
 * Player class holds all player-specific data
 */

public class Player implements CashHolder, Comparable<Player> {

    public static int MAX_PLAYERS = 8;

    public static int MIN_PLAYERS = 2;

    private String name = "";

    private int index = 0;

    private CashModel wallet = new CashModel(this);

    private CertCountModel certCount = new CertCountModel(this);

    private MoneyModel blockedCash;
    private CalculatedMoneyModel freeCash;
    private CalculatedMoneyModel worth;

    private boolean hasBoughtStockThisTurn = false;

    private Portfolio portfolio = null;

     public Player(String name, int index) {
        this.name = name;
        this.index = index;
        portfolio = new Portfolio(name, this);
        freeCash = new CalculatedMoneyModel(this, "getFreeCash");
        wallet.addDependent(freeCash);
        blockedCash = new MoneyModel(name + "_blockedCash");
        blockedCash.setOption(MoneyModel.SUPPRESS_ZERO);
        worth = new CalculatedMoneyModel(this, "getWorth");
        wallet.addDependent(worth);
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
            worth += cert.getCompany().getGameEndPrice() * cert.getShares();
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
