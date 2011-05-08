/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Player.java,v 1.26 2010/05/18 22:07:18 evos Exp $ */
package rails.game;

import rails.game.model.*;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;

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
    private BooleanState bankrupt;
    private MoneyModel lastORWorthIncrease;
    private IntegerState worthAtORStart;

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
        bankrupt = new BooleanState (name+"_isBankrupt", false);
        lastORWorthIncrease = new MoneyModel (name+"_lastORIncome");
        lastORWorthIncrease.setOption(MoneyModel.ALLOW_NEGATIVE);
        worthAtORStart = new IntegerState (name+"_worthAtORStart");
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

    public void addCash(int amount) {
        wallet.addCash(amount);
    }

    /**
     * Get the player's total worth.
     *
     * @return Total worth
     */
    public int getWorth() {
        // if player is bankrupt cash is not counted
        // as this was generated during forced selling
        int worth;
        if (bankrupt.booleanValue()) {
            worth = 0;
        } else {
            worth = wallet.getCash();
        }

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

    public MoneyModel getLastORWorthIncrease () {
        return lastORWorthIncrease;
    }

    public void setWorthAtORStart () {
        worthAtORStart.set(getWorth());
    }

    public void setLastORWorthIncrease () {
        lastORWorthIncrease.set(getWorth() - worthAtORStart.intValue());
    }

    public void updateWorth () {
        worth.update();
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

    public void setIndex(int index) {
        this.index = index;
    }

    public void setBankrupt () {
    	bankrupt.set(true);
    }

    public boolean isBankrupt () {
    	return bankrupt.booleanValue();
    }

    /**
     * Compare Players by their total worth, in descending order. This method
     * implements the Comparable interface.
     * second level decision is by name
     */
    public int compareTo(Player p) {
        // first by wealth
        int result = -new Integer(getWorth()).compareTo(new Integer(p.getWorth()));
        // then by name
        if (result == 0)
            result = getName().compareTo(p.getName());
        return result;
    }
}
