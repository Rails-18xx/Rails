package net.sf.rails.game;

import java.util.Map;

import com.google.common.collect.Maps;

import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.RailsMoneyOwner;
import net.sf.rails.game.model.CalculatedMoneyModel;
import net.sf.rails.game.model.CertificateCountModel;
import net.sf.rails.game.model.CountingMoneyModel;
import net.sf.rails.game.model.MoneyModel;
import net.sf.rails.game.model.PlayerNameModel;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.model.PurseMoneyModel;
import net.sf.rails.game.model.CalculatedMoneyModel.CalculationMethod;
import net.sf.rails.game.model.SoldThisRoundModel;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.ChangeActionOwner;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Purse;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.GameManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Model; // Correct base class for models
import net.sf.rails.game.state.Observable; // Base for Model, needed for update()
import net.sf.rails.game.state.Observer; // Interface for listening to changes (Adjust package if needed)
import net.sf.rails.game.RailsRoot;

public class Player extends RailsAbstractItem
        implements RailsMoneyOwner, PortfolioOwner, ChangeActionOwner, Comparable<Player> {

    // FIXME: Rails 2.0 Do we need the index number?

    // dynamic data (states and models)
    private final IntegerState index = IntegerState.create(this, "index");
    private final PortfolioModel portfolio = PortfolioModel.create(this);
    private final CertificateCountModel certCount = CertificateCountModel.create(portfolio);
    private final CertCountWithLimitModel certCountWithLimit;

    // ++ TIME MANAGEMENT ++
    // Renamed from timeElapsed
    protected final IntegerState timeBankSeconds = IntegerState.create(this, "timeBankSeconds", 0);
    // ++ New field for penalty ++
    protected final IntegerState timePenaltySeconds = IntegerState.create(this, "timePenaltySeconds", 0);
    // ++ END TIME MANAGEMENT ++

    private final PurseMoneyModel cash = PurseMoneyModel.create(this, "cash", false);
    private final CalculatedMoneyModel freeCash;
    private final CountingMoneyModel blockedCash = CountingMoneyModel.create(this, "blockedCash", false);
    private final CalculatedMoneyModel worth;
    private final CountingMoneyModel lastORWorthIncrease = CountingMoneyModel.create(this, "lastORIncome", false);

    private final BooleanState bankrupt = new BooleanState(this, "isBankrupt");
    private final IntegerState worthAtORStart = IntegerState.create(this, "worthAtORStart");
    private final Map<PublicCompany, SoldThisRoundModel> soldThisRound = Maps.newHashMap();
    private final PlayerNameModel playerNameModel = PlayerNameModel.create(this);

    private final GenericState<String> fullName = new GenericState<>(this, "fullName");

    public void setFullName(String name) {
        this.fullName.set(name);
    }

public String getFullName() {
        String name = this.fullName.value();
        if (name == null || name.trim().isEmpty()) {
            // Attempt to recover from local config (survives reloads without altering save files)
            name = net.sf.rails.common.Config.get("player.fullname." + getId());
        }
        return (name != null && !name.trim().isEmpty()) ? name : getId(); 
    }
    
    private static final Logger log = LoggerFactory.getLogger(Player.class);

    private Player(PlayerManager parent, String id, int index) {
        super(parent, id);
        this.index.set(index);

        blockedCash.setSuppressZero(true);
        lastORWorthIncrease.setDisplayNegative(true);

        // definitions of freeCash
        CalculationMethod freeCashMethod = new CalculationMethod() {
            @Override
            public int calculate() {
                return cash.value() - blockedCash.value();
            }

            @Override
            public boolean initialised() {
                return cash.initialised() && blockedCash.initialised();
            }
        };
        freeCash = CalculatedMoneyModel.create(this, "freeCash", freeCashMethod);
        cash.addModel(freeCash);
        blockedCash.addModel(freeCash);

        // define definitions of worth
        CalculationMethod worthMethod = new CalculationMethod() {
            @Override
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
                    // Fix for 5% shares (e.g. Prussia in 1835) counting as 10% shares.
                    // normalize based on standard 10% share size if the company unit is smaller.
                    PublicCompany company = cert.getCompany();
                    if (company != null) {
                        // Check if GameManager defines a fixed worth for this public company
                        // (Handles 1835 Minors M1-M6 which are Publics but have fixed pre-exchange
                        // value)
                        int fixedWorth = getParent().getRoot().getGameManager().getPublicCompanyWorth(company);

                        if (fixedWorth > -1) {
                            worth += fixedWorth;
                        } else if (company.getCurrentSpace() != null) {
                            // Standard Market Value Calculation
                            int priceBasis = Math.max(10, company.getShareUnit());
                            worth += (int) ((long) company.getCurrentSpace().getPrice() * cert.getShare() / priceBasis);
                        }
                    }
                }
                // Delegate Private Company valuation to GameManager.
                // This allows GameManager_1835 to intervene for companies exchanging for PR
                // shares.
                for (PrivateCompany priv : getPortfolioModel().getPrivateCompanies()) {
                    if (priv != null) {
                        // Note: You must add getPrivateWorth(PrivateCompany p) to GameManager.java
                        worth += getParent().getRoot().getGameManager().getPrivateWorth(priv);
                    }
                }
                for (PrivateCompany priv : getPortfolioModel().getPrivateCompanies()) {
                    worth += priv.getBasePrice();
                }
                // log.debug("$$$$$ Player {} worth is {}", id, worth);
                return worth;
            }

            @Override
            public boolean initialised() {
                return cash.initialised();
            }
        };
        worth = CalculatedMoneyModel.create(this, "worth", worthMethod);
        portfolio.addModel(worth);
        cash.addModel(worth);

        certCountWithLimit = new CertCountWithLimitModel(this); // Pass Player instance
        certCount.addModel(certCountWithLimit); // Add as listener to certCount changes

    }

    public static Player create(PlayerManager parent, String id, int index) {
        return new Player(parent, id, index);
    }

    @Override
    public PlayerManager getParent() {
        return (PlayerManager) super.getParent();
    }

    public void finishConfiguration(RailsRoot root) {
        portfolio.finishConfiguration();

        // create soldThisRound states
        for (PublicCompany company : root.getCompanyManager().getAllPublicCompanies()) {
            soldThisRound.put(company, SoldThisRoundModel.create(this, company));
        }
        // make worth aware of market model
        root.getStockMarket().getMarketModel().addModel(worth);

        // ADD LISTENER FOR LIMIT CHANGES
        // This ensures the "X / Y" display updates if the limit changes mid-game
        Observable limitModel = root.getPlayerManager().getPlayerCertificateLimitModel();
        limitModel.addModel(certCountWithLimit); // <-- THIS IS CORRECT

        // Store the limit model in our combined model for easy access
        certCountWithLimit.setLimitModel(limitModel); // <-- THIS IS CORRECT
    }

    // ADD NEW GETTER
    public Model getCertCountWithLimitModel() {
        return certCountWithLimit;
    }

    public String getName() {
        return getId();
    }

    public String getNameAndPriority() {
        boolean isPriority = (getParent().getPriorityPlayer() == this);
        if (isPriority) {
            // Using a table forces Swing to render the text and emoji in separate contexts,
            // allowing the correct font fallback for the emoji even if the name uses a text
            // font.
            return "<html><table cellpadding='0' cellspacing='0'><tr><td>" + getId()
                    + "</td><td><font size='5'> \uD83D\uDE82</font></td></tr></table></html>";
        }
        return getId();
    }

    public PlayerNameModel getPlayerNameModel() {
        return playerNameModel;
    }

    public CalculatedMoneyModel getWorthModel() {
        return worth;
    }

    public MoneyModel getLastORWorthIncrease() {
        return lastORWorthIncrease;
    }

    public void setWorthAtORStart() {
        worthAtORStart.set(getWorth());
    }

    public void setLastORWorthIncrease() {
        lastORWorthIncrease.set(getWorth() - worthAtORStart.value());
    }

    // TODO Doesn't this duplicate getCash()?
    public int getCashValue() {
        return cash.value();
    }

    public void updateWorth() {
        // FIXME: Is this method still required
        // worth.update();
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
        return index.value();
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public void setBankrupt() {
        bankrupt.set(true);
    }

    public boolean isBankrupt() {
        return bankrupt.value();
    }

    public void resetSoldThisRound() {
        for (SoldThisRoundModel state : soldThisRound.values()) {
            state.set(false);
        }
    }

    public boolean hasSoldThisRound(PublicCompany company) {
        return soldThisRound.get(company).value();
    }

    public void setSoldThisRound(PublicCompany company) {
        soldThisRound.get(company).set(true);
    }

    public SoldThisRoundModel getSoldThisRoundModel(PublicCompany company) {
        return soldThisRound.get(company);
    }

    // MoneyOwner interface
    @Override
    public Purse getPurse() {
        return cash.getPurse();
    }

    @Override
    public int getCash() {
        return cash.getPurse().value();
    }

    // Owner interface
    @Override
    public PortfolioModel getPortfolioModel() {
        return portfolio;
    }

    /**
     * Compare Players by their total worth, in descending order. This method
     * implements the Comparable interface.
     * second level decision is by name
     */
    @Override
    public int compareTo(Player p) {
        // first by wealth
        int result = -Integer.compare(getWorth(), p.getWorth());
        // then by name
        if (result == 0)
            result = getId().compareTo(p.getId());
        return result;
    }

    public PurseMoneyModel getWallet() {
        return cash;
    }

    public String toString() {
        return getId();
    }

    // ... inside Player class

    // =================================================================
    // START OF CORRECTED INNER CLASS
    // =================================================================

    /**
     * This model provides a String representation of "CurrentCerts/Limit"
     * and is a MODEL (dependant) of both the player's CertificateCountModel
     * and the game's PlayerCertificateLimitModel.
     *
     * The StateManager handles all updates.
     */
    private class CertCountWithLimitModel extends Model {

        // Store the limit model for use in calculate()
        private Observable sourceLimitModel;

        protected CertCountWithLimitModel(Player parent) {
            super(parent, "certCountWithLimitModel");
            // No need to store certCount model, we can get it from getParent()
        }

        // Setter for the limit model, called from Player.finishConfiguration
        public void setLimitModel(Observable limitModel) {
            this.sourceLimitModel = limitModel;
        }

        /**
         * Provides the text for the UI Field.
         * This is called by the StateManager when any dependency (certCount or
         * limitModel) changes.
         */
        @Override
        public String toText() {
            // Calculate the value fresh every time it's asked for.
            return calculate();
        }

        // Inside the CertCountWithLimitModel inner class in Player.java
        private String calculate() {
            try {
                // Get current count (X)
                float count = ((Player) getParent()).getCertCountModel().getParent().getCertificateCount();
                String countStr = ("" + count).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");

                // Get the limit (Y) from the GameManager, not the PlayerManager.
                // This ensures we get the 1835-specific override.
                Player player = (Player) getParent();
                GameManager gm = player.getParent().getRoot().getGameManager();
                int limit = gm.getPlayerCertificateLimit(player);

                return countStr + "/" + limit;
            } catch (Exception e) {
                // Handle potential nulls during initialization
                // log.warn("Error calculating CertCountWithLimitModel", e);
                return "0/0";
            }
        }
        // NO update() or getObservable() methods are needed
    }
    // =================================================================
    // END OF CORRECTED INNER CLASS
    // =================================================================

    // ++ TIME MANAGEMENT METHODS ++
    /**
     * @return The model holding the player's remaining time bank in seconds.
     */
    public IntegerState getTimeBankModel() {
        return timeBankSeconds;
    }

    /**
     * Adds penalty seconds when time runs out.
     * 
     * @param secondsToAdd The number of seconds to add to the penalty (usually 1
     *                     per tick).
     */
    public void addTimePenalty(int secondsToAdd) {
        if (secondsToAdd > 0) {
            this.timePenaltySeconds.add(secondsToAdd);
        }
    }

    /**
     * @return The total accumulated time penalty in seconds.
     */
    public int getTimePenaltySeconds() {
        return this.timePenaltySeconds.value();
    }

    /**
     * @return The model holding the player's accumulated time penalty in seconds.
     */
    public IntegerState getTimePenaltyModel() {
        return this.timePenaltySeconds;
    }
    // ++ END TIME MANAGEMENT METHODS ++

    /**
     * New method to get worth adjusted by time penalty.
     * Used for the secondary UI column/sorting.
     */
    public int getWorthWithTimePenalty() {
        return getWorth() - getTimePenaltySeconds();
    }

public int getWorth() {
        // if player is bankrupt cash is not counted
        // as this was generated during forced selling
        int rawWorth = 0;
        int cashValue = 0;
        int shareValue = 0;
        int privateValue = 0;

        if (bankrupt.value()) {
            rawWorth = 0;
        } else {
            cashValue = cash.value();
            rawWorth = cashValue; // Start with cash

            // Add share value
            for (PublicCertificate cert : getPortfolioModel().getCertificates()) {
                // Ensure company and price are valid before adding
                PublicCompany company = cert.getCompany();
                if (company != null) {
                    // 1. Check if GameManager defines a fixed worth for this public company 
                    // (Handles 1835 Minors M1-M6 which are Publics but have fixed pre-exchange value)
                    int fixedWorth = getParent().getRoot().getGameManager().getPublicCompanyWorth(company);
                    
                    int certWorth = 0;
                    if (fixedWorth > -1) {
                        // Use the fixed value for Minors (M1-M6)
                        certWorth = fixedWorth;
                    } else if (company.getCurrentSpace() != null) {
                        // 2. Fallback to Standard Market Value Calculation
                        int priceBasis = Math.max(10, company.getShareUnit());
                        certWorth = (int) ((long) company.getCurrentSpace().getPrice() * cert.getShare() / priceBasis);
                    }
                    shareValue += certWorth;
                    rawWorth += certWorth;
                }
            }
            // Add private company value
            for (PrivateCompany priv : getPortfolioModel().getPrivateCompanies()) {
                if (priv != null) { // Ensure private company object is not null
                    // Delegate to GameManager to handle Special Exchange values (BB, HB)
                    int pWorth = getParent().getRoot().getGameManager().getPrivateWorth(priv);
                    privateValue += pWorth;
                    rawWorth += pWorth;
                }
            }
        }


        int finalWorth = rawWorth;

        return finalWorth;
    }

    // These methods are for 're-hydrating' a saved state and bypass normal logic.

    public void setCash_AI(int cash) {
        this.cash.getPurse().setAmount_AI(cash); // FIXED: Use new public setter
    }

    public void setSoldThisRound_AI(PublicCompany company, boolean sold) {
        if (soldThisRound.containsKey(company)) {
            soldThisRound.get(company).set(sold);
        }
    }
    
    private net.sf.rails.game.ai.Actor actor = null;

    // NEW GETTER: Provides the actor instance required by
    // GameUIManager.isCurrentPlayerAI()
    public net.sf.rails.game.ai.Actor getActor() {
        return this.actor;
    }

    // NEW SETTER: Allows the AI system to assign the AIPlayer implementation.
    public void setActor(net.sf.rails.game.ai.Actor actor) {
        this.actor = actor;
    }

}