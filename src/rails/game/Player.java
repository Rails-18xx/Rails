package rails.game;

import rails.game.model.CalculatedMoneyModel;
import rails.game.model.CalculatedMoneyModel.CalculationMethod;
import rails.game.model.CountingMoneyModel;
import rails.game.model.WalletMoneyModel;
import rails.game.model.CertificateCountModel;
import rails.game.model.MoneyModel;
import rails.game.model.PortfolioModel;
import rails.game.model.PortfolioOwner;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;

public class Player extends RailsAbstractItem implements MoneyOwner, PortfolioOwner, Comparable<Player> {

    // TODO: Are those still needed?
    public static int MAX_PLAYERS = 8;
    public static int MIN_PLAYERS = 2;
    
    // static data (configured)
    private int index = 0;
    
    // dynamic data (states and models)
    private final PortfolioModel portfolio = PortfolioModel.create(this);
    private final CertificateCountModel certCount = CertificateCountModel.create(portfolio);

    private final WalletMoneyModel cash = WalletMoneyModel.create(this, "cash", false);
    private final CalculatedMoneyModel freeCash;
    private final CountingMoneyModel blockedCash = CountingMoneyModel.create(this, "blockedCash", false);
    private final CalculatedMoneyModel worth;
    private final CountingMoneyModel lastORWorthIncrease = CountingMoneyModel.create(this, "lastORIncome", false);

    private final BooleanState bankrupt = BooleanState.create(this, "isBankrupt");
    private final IntegerState worthAtORStart = IntegerState.create(this, "worthAtORStart");

    private Player(RailsItem parent, String id, int index) {
        super(parent, id);
        this.index = index;

        blockedCash.setSuppressZero(true);
        lastORWorthIncrease.setDisplayNegative(true);
        
        // definitions of freeCash
        CalculationMethod freeCashMethod = new CalculationMethod(){ 
            public int calculate() {
                return cash.value() - blockedCash.value();
            }
            public boolean initialised() {
                return cash.initialised() && blockedCash.initialised();
            }
        };
        freeCash = CalculatedMoneyModel.create(this, "freeCash", freeCashMethod);
        cash.addModel(freeCash);
        blockedCash.addModel(freeCash);
        
        // define definitions of worth
        CalculationMethod worthMethod = new CalculationMethod(){
            public int calculate() {
                // if player is bankrupt cash is not counted
                // as this was generated during forced selling
                int worth;
                if (bankrupt.value()) {
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
        };
        worth = CalculatedMoneyModel.create(this, "worth", worthMethod);
        portfolio.addModel(worth);
        cash.addModel(worth);
    }
    
    public static Player create(PlayerManager parent, String id, int index) {
        return new Player(parent, id, index);
    }
   

    public String getNameAndPriority() {
        return getId() + (GameManager.getInstance().getPriorityPlayer() == this ? " PD" : "");
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
    	return bankrupt.value();
    }

    // MoneyOwner interface
    public int getCash() {
        return cash.value();
    }

    // Owner interface
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

    public WalletMoneyModel getWallet() {
        return cash;
    }

}
