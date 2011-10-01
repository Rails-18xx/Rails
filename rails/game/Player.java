package rails.game;

import rails.game.model.*;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;

/**
 * Player class holds all player-specific data
 */

public class Player extends PortfolioCashOwner implements Comparable<Player> {

    public static int MAX_PLAYERS = 8;

    public static int MIN_PLAYERS = 2;

    private String name = "";

    private int index = 0;

    private CertificateCountModel certCount;

    private MoneyModel blockedCash;
    private CalculatedMoneyModel freeCash;
    private CalculatedMoneyModel worth;
    private BooleanState bankrupt;
    private MoneyModel lastORWorthIncrease;
    private IntegerState worthAtORStart;

    private boolean hasBoughtStockThisTurn = false;

    public Player(PlayerManager parent, String name, int index) {
        super(parent, name); // intializes the PortfolioCashOwner
        
        this.name = name;
        this.index = index;
        freeCash = new CalculatedMoneyModel(this, "getFreeCash");
        blockedCash = new MoneyModel(this, "blockedCash");
        blockedCash.setSuppressZero(true);
        worth = new CalculatedMoneyModel(this, "getWorth");
        bankrupt = new BooleanState (this, "isBankrupt", false);
        lastORWorthIncrease = new MoneyModel (this, "lastORIncome");
        lastORWorthIncrease.setAllowNegative(true);
        worthAtORStart = new IntegerState (this, "worthAtORStart");

        certCount = new CertificateCountModel(getPortfolio());

        getCashModel().addObserver(freeCash);
        getCashModel().addObserver(worth);
    }

    /**
     * @return Returns the player's name.
     */
    public String getId() {
        return name;
    }

    public String getNameAndPriority() {
        return name + (GameManager.getInstance().getPriorityPlayer() == this ? " PD" : "");
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
            worth = this.getCashModel().value();
        }

        for (PublicCertificate cert : getPortfolio().getCertificates()) {
            worth += cert.getCompany().getGameEndPrice() * cert.getShares();
        }
        for (PrivateCompany priv : getPortfolio().getPrivateCompanies()) {
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

    public int getCashValue() {
        return getCashModel().value();
    }
    
    public void updateWorth () {
        // TODO: Is this method still required
        worth.update();
    }

    public CertificateCountModel getCertCountModel() {
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
        if (amount > getCashModel().value() - blockedCash.intValue()) {
            return false;
        } else {
            blockedCash.add(amount);
            // TODO: is this still required?
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
            // TODO: is this still required?
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
        return getCashModel().value() - blockedCash.intValue();
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
            result = getId().compareTo(p.getId());
        return result;
    }

}
