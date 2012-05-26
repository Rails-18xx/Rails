package rails.game;

import rails.game.model.*;
import rails.game.model.CalculatedMoneyModel.CalculationMethod;
import rails.game.state.BooleanState;
import rails.game.state.AbstractItem;
import rails.game.state.IntegerState;
import rails.game.state.Item;

/**
 * Player class holds all player-specific data
 */

public class Player extends AbstractItem implements CashOwner, PortfolioOwner, Comparable<Player> {

    // TODO: Are those still needed?
    public static int MAX_PLAYERS = 8;
    public static int MIN_PLAYERS = 2;
    
    // static data (configured)
    // TODO: How does the index work? Still needed?
    private String name = "";
    private int index = 0;
    
    // dynamic data (states and models)
    private final PortfolioModel portfolio = PortfolioModel.create();
    private final CertificateCountModel certCount = CertificateCountModel.create();

    private final CashMoneyModel cash = CashMoneyModel.create();
    private final CalculatedMoneyModel freeCash = CalculatedMoneyModel.create();
    private final CashMoneyModel blockedCash = CashMoneyModel.create();
    private final CalculatedMoneyModel worth = CalculatedMoneyModel.create();
    private final CashMoneyModel lastORWorthIncrease = CashMoneyModel.create();

    private final BooleanState bankrupt = BooleanState.create();
    private final IntegerState worthAtORStart = IntegerState.create();

    // TODO: Write internal methods for the calculation methods
    public Player(int index) {
        this.index = index;
    
    }
    
    /**
     *  @param parent restricted to PlayerManager
     */
    @Override
    public void init(Item parent, String id) {
        super.checkedInit(parent, id, PlayerManager.class);
        
        portfolio.init(this, PortfolioModel.id);
        certCount.init(portfolio, CertificateCountModel.ID);

        cash.init(this, "cash");
        freeCash.init(this, "freeCash");
        blockedCash.init(this, "blockedCash");
        blockedCash.setSuppressZero(true);
        worth.init(this, "getWorth");
        lastORWorthIncrease.init(this, "lastORIncome");
        lastORWorthIncrease.setDisplayNegative(true);
        
        bankrupt.init(this, "isBankrupt");
        worthAtORStart.init(this, "worthAtORStart");
        // define definitions of freeCash
        freeCash.initMethod(
                new CalculationMethod(){ 
                    public int calculate() {
                        return cash.value() - blockedCash.value();
                    }
                    public boolean initialised() {
                        return cash.initialised() && blockedCash.initialised();
                    }
                });
        cash.addModel(freeCash);
        blockedCash.addModel(freeCash);
        
        // define definitions of worth
        worth.initMethod(
                new CalculationMethod(){
                    public int calculate() {
                        // if player is bankrupt cash is not counted
                        // as this was generated during forced selling
                        int worth;
                        if (bankrupt.booleanValue()) {
                            worth = 0;
                        } else {
                            worth = cash.value();
                        }

                        for (PublicCertificate cert : getPortfolioModel().getCertificates()) {
                            worth += cert.getCompany().getGameEndPrice() * cert.getShares();
                        }
                        for (PrivateCompany priv : getPortfolioModel().getPrivateCompanies()) {
                            worth += priv.getBasePrice();
                        }
                        return worth;
                    }
                    public boolean initialised() {
                        return cash.initialised();
                    }
                });       
        portfolio.addModel(worth);
        cash.addModel(worth);
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
        return worth.value();
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
        lastORWorthIncrease.set(getWorth() - worthAtORStart.value());
    }

    public int getCashValue() {
        return cash.value();
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

    /**
     * Block cash allocated by a bid.
     *
     * @param amount Amount of cash to be blocked.
     * @return false if the amount was not available.
     */
    public boolean blockCash(int amount) {
        if (amount > cash.value() - blockedCash.value()) {
            return false;
        } else {
            blockedCash.change(amount);
            // TODO: is this still required?
            // freeCash.update();
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
        if (amount > blockedCash.value()) {
            return false;
        } else {
            blockedCash.change(-amount);
            // TODO: is this still required?
            // freeCash.update();
            return true;
        }
    }

    /**
     * @return the unblocked cash (available for bidding)
     */
    public int getFreeCash() {
        return freeCash.value();
    }

    public int getBlockedCash() {
        return blockedCash.value();
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

    // CashOwner interface
    public int getCash() {
        return cash.value();
    }

    // PortfolioOwner interface
    public PortfolioModel getPortfolioModel() {
        return portfolio;
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

    @Override
    public String toString() {
        return name;
    }

    public CashMoneyModel getCashModel() {
        // TODO Auto-generated method stub
        return null;
    }

}
