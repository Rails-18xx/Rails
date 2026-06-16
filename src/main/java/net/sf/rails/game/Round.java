package net.sf.rails.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.rails.common.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.game.financial.Bank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;

// Cannot be abstract because must be instantiatable to make it stateful, see GameManager_1837.
public /* abstract */ class Round extends RailsAbstractItem implements RoundFacade {

    private static final Logger log = LoggerFactory.getLogger(Round.class);

    protected final PossibleActions possibleActions;
    protected final GuiHints guiHints;

    protected final GameManager gameManager;
    protected final CompanyManager companyManager;
    protected final PlayerManager playerManager;
    protected final Bank bank;
    protected final PortfolioModel ipo;
    protected final PortfolioModel pool;
    protected final PortfolioModel unavailable;
    protected final PortfolioModel scrapHeap;
    protected final StockMarket stockMarket;
    protected final MapManager mapManager;

    protected final BooleanState wasInterrupted = new BooleanState(this, "wasInterrupted");

    protected Round(GameManager parent, String id) {
        super(parent, id);

        this.gameManager = parent;
        this.possibleActions = gameManager.getPossibleActions();

        companyManager = getRoot().getCompanyManager();
        playerManager = getRoot().getPlayerManager();
        bank = getRoot().getBank();
        // TODO: It would be good to work with BankPortfolio and Owner instead of
        // PortfolioModels
        // However this requires a lot of work inside the Round classes
        ipo = bank.getIpo().getPortfolioModel();
        pool = bank.getPool().getPortfolioModel();
        unavailable = bank.getUnavailable().getPortfolioModel();
        scrapHeap = bank.getScrapHeap().getPortfolioModel();
        stockMarket = getRoot().getStockMarket();
        mapManager = getRoot().getMapManager();

        guiHints = gameManager.getUIHints();
        guiHints.setCurrentRoundType(getClass());
    }

    // called from GameManager
    @Override
    public boolean process(PossibleAction action) {
        return true;
    }

    /**
     * Default version, does nothing. Subclasses should override this method
     * with a real version.
     */
    @Override
    public boolean setPossibleActions() {
        return false;
    }

    /**
     * Generic stub to resume an interrupted round.
     * Only valid if implemented in a subclass.
     */
    // called from GameManager
    @Override
    public void resume() {
        log.error("Calling Round.resume() is invalid");
    }

    // called from GameManager and GameUIManager
    @Override
    public String getRoundName() {
        return this.getClass().getSimpleName();
    }

    /**
     * A stub for processing actions triggered by a phase change.
     * Must be overridden by subclasses that need to process such actions.
     *
     * @param name  (required) The name of the action to be executed
     * @param value (optional) The value of the action to be executed, if applicable
     */
    // can this be moved to GameManager, not yet as there are internal dependencies
    // called from GameManager
    @Override
    public void processPhaseAction(String name, String value) {

    }

    /**
     * Returns the 'phase number', defined as 2 for phase 2, etc.
     * For use in games where some share-related rules depend on that number,
     * such as 18Scan and SOH.
     * 
     * @return The phase number
     */
    protected int getPhaseNumber() {
        // The index starts at 0, so we must add 2
        return gameManager.getCurrentPhase().getIndex() + 2;
    }

    /**
     * Set the operating companies in their current acting order
     */
    public List<PublicCompany> setOperatingCompanies() {
        return setOperatingCompanies(null, null);
    }

    public List<PublicCompany> setOperatingCompanies(String type) {
        List<PublicCompany> selectedCompanies = new ArrayList<>();
        for (PublicCompany comp : setOperatingCompanies()) {
            if (type.equals(comp.getType().getId())) {
                selectedCompanies.add(comp);
            }
        }
        return selectedCompanies;
    }

    // What is the reason of that to have that here => move to OR?
    // this is still required for 18EU StockRound as due to the merger there are
    // companies that have to discard trains
    // called only internally
    public List<PublicCompany> setOperatingCompanies(List<PublicCompany> oldOperatingCompanies,
            PublicCompany lastOperatingCompany) {

        Map<Integer, PublicCompany> operatingCompanies = new TreeMap<>();
        List<PublicCompany> newOperatingCompanies;
        StockSpace space;
        int key;
        int minorNo = 0;
        boolean reorder = gameManager.isDynamicOperatingOrder()
                && oldOperatingCompanies != null && lastOperatingCompany != null;

        int lastOperatingCompanyIndex;
        if (reorder) {
            newOperatingCompanies = oldOperatingCompanies;
            lastOperatingCompanyIndex = oldOperatingCompanies.indexOf(lastOperatingCompany);
        } else {
            newOperatingCompanies = companyManager.getAllPublicCompanies();
            lastOperatingCompanyIndex = -1;
        }

        for (PublicCompany company : newOperatingCompanies) {
            if (!reorder && !canCompanyOperateThisRound(company))
                continue;

            if (reorder
                    && oldOperatingCompanies.indexOf(company) <= lastOperatingCompanyIndex) {
                // Companies that have operated this round get lowest keys
                key = oldOperatingCompanies.indexOf(company);
            } else if (company.hasStockPrice()) {
                // Key must put companies in reverse operating order, because sort
                // is ascending.
                space = company.getCurrentSpace();
                key = 1000000 * (999 - space.getPrice())
                        + 10000 * (99 - space.getColumn())
                        + 100 * (space.getRow() + 1)
                        + space.getStackPosition(company);
            } else {
                key = 50 + ++minorNo;
            }
            operatingCompanies.put(key, company);
        }

        return new ArrayList<>(operatingCompanies.values());
    }

    /**
     * Can a public company operate? (Default version)
     */
    // What is the reason of that to have that here? => move to OR?
    // is called by setOperatingCompanies above
    // called only internally
    protected boolean canCompanyOperateThisRound(PublicCompany company) {
        return company.hasFloated() && !company.isClosed();
    }

    /**
     * Check if a company must be floated, and if so, do it.
     * <p>
     * This method is
     * included here because it is used in various types of Round.
     *
     * @param company Company to be checked for being floatable
     */
    // What is the reason of that to have that here? => best to move it to
    // PublicCompany in the long-run
    // is called by StartRound as well
    // called only internally
    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated())
            return;

        if (company.getSoldPercentage() >= company.getFloatPercentage()) {
            // Company floats
            floatCompany(company);
        }
    }

    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation.
     * <p>
     * Full capitalisation is implemented
     * as in 1830. Partial capitalisation is implemented as in 1851. Other ways
     * to process the consequences of company flotation must be handled in
     * game-specific subclasses.
     */
    // What is the reason of that to have that here? => move to SR?
    // called by checkFlotation above
    // move it to PublicCompany in the long-run
    // called only internally
    protected void floatCompany(PublicCompany company) {

        if (company.hasFloated())
            return;

        int cash = getCashOnFloating(company);

        // Subtract initial token cost (e.g. 1851, 18EU)
        cash -= company.getBaseTokensBuyCost();

        company.setFloated(); // After calculating cash (for 1851: price goes
        // up)

        if (cash > 0) {
            String cashText = Currency.fromBank(cash, company);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",
                    company.getId(),
                    cashText));
        } else {
            ReportBuffer.add(this, LocalText.getText("Floats",
                    company.getId()));
        }

        if (company.getCapitalisation() == PublicCompany.CAPITALISE_INCREMENTAL
                && company.canHoldOwnShares()) {
            // move all shares from ipo to the company portfolio
            // FIXME: Does this work correctly?
            Portfolio.moveAll(ipo.getCertificates(company), company);
        }
    }

    protected int getCashOnFloating(PublicCompany company) {
        // Move cash and shares where required
        int soldPercentage = company.getSoldPercentage();
        int cash;
        int capitalisationMode = company.getCapitalisation();
        if (company.hasStockPrice()) {
            int capFactor = 0;
            int shareUnit = company.getShareUnit();
            if (capitalisationMode == PublicCompany.CAPITALISE_FULL) {
                // Full capitalisation as in 1830
                capFactor = 100 / shareUnit;
            } else if (capitalisationMode == PublicCompany.CAPITALISE_PART) {
                // Like full capitalisation, but for less that 100%
                // E.g. 18Scan SJ: only for 70%
                capFactor = company.getCapitalisationShares();
            } else if (capitalisationMode == PublicCompany.CAPITALISE_INCREMENTAL) {
                // Incremental capitalisation as in 1851
                capFactor = soldPercentage / shareUnit;
            } else if (capitalisationMode == PublicCompany.CAPITALISE_WHEN_BOUGHT) {
                // Cash goes directly to treasury at each buy (as in 1856 before phase 6)
                capFactor = 0;
            }
            int price = (company.hasParPrice() ? company.getIPOPrice() : company.getMarketPrice());
            cash = capFactor * price;
        } else if (capitalisationMode == PublicCompany.CAPITALISE_FIXED_CASH) {
            cash = company.getCapitalisationFixedCash();
        } else {
            cash = company.getFixedPrice();
        }
        log.debug("Company {} receives {} on floating", company, Bank.format(this, cash));
        return cash;
    }

    /**
     * Stub, to be overridden where needed.
     * Used in 1826
     * 
     * @param company The floating public company
     * @return
     */
    protected int getCustomCapitalization(PublicCompany company) {
        return 0;
    }

    protected void finishRound() {
        finishRound(true);
    }

    // Could be moved somewhere else (RoundUtils?)
    // called only internally. EV: why is that an issue?
    protected void finishRound(boolean reportFinancials) {

        if (reportFinancials) {
            ReportBuffer.add(this, "");
            for (PublicCompany c : companyManager.getAllPublicCompanies()) {
                if (c.hasFloated() && !c.isClosed()) {
                    ReportBuffer.add(this, LocalText.getText("Has", c.getId(),
                            Bank.format(this, c.getCash())));
                }
            }
            for (Player p : playerManager.getPlayers()) {
                if (!p.isBankrupt()) {
                    ReportBuffer.add(this, LocalText.getText("Has", p.getId(),
                            Bank.format(this, p.getCashValue())));
                }
            }
        }
        // This is the "1835-aware" patch.
        // We check if the gameManager is an instance of the 1835-subclass.
        // If it is, we MUST cast it to force the compiler to call the
        // overridden nextRound() method that checks the priority queue.
        if (gameManager instanceof net.sf.rails.game.specific._1835.GameManager_1835) {
            ((net.sf.rails.game.specific._1835.GameManager_1835) gameManager).nextRound(this);
        } else {
            // Otherwise, do the normal, non-polymorphic call.
            gameManager.nextRound(this);
        }
    }

    // called only from 1835 Operating Round?
    public boolean wasInterrupted() {
        return wasInterrupted.value();
    }

    /** Stub to allow lower subclasses to provide their own window title */
    public String getOwnWindowTitle() {
        return null;
    }

    public List<PossibleAction> getPossibleActionsList() {
        return possibleActions.getList();
    }// File: Round.java


    @Override
    public Player getCurrentPlayer() {
        return null;
    }
    
    /**
     * Generates individual discard actions for every physical train.
     * This resolves the bug where "2" and "2+" were conflated and
     * fulfills the requirement for unique buttons per train.
     */
    public void generateGroupedDiscardActions(PublicCompany company, PossibleActions possibleActions) {
        possibleActions.clear();
        List<net.sf.rails.game.Train> trains = new java.util.ArrayList<>(company.getPortfolioModel().getTrainList());
        java.util.Map<String, java.util.Set<net.sf.rails.game.Train>> typeGroups = new java.util.HashMap<>();

        for (net.sf.rails.game.Train train : trains) {
            if (train == null)
                continue;
            // Extract the type (e.g., "2" from "2_0", "2+" from "2+_2")
            String type = train.getName().replaceAll("(.*)_\\d+", "$1");
            typeGroups.computeIfAbsent(type, k -> new java.util.HashSet<>()).add(train);
        }

        for (java.util.Map.Entry<String, java.util.Set<net.sf.rails.game.Train>> entry : typeGroups.entrySet()) {
            rails.game.action.DiscardTrain action = new rails.game.action.DiscardTrain(company, entry.getValue());
            action.setButtonLabel(entry.getKey());
            possibleActions.add(action);
        }
    }

    /**
     * CENTRALIZED HELPER: Executes the physical move of a train to the Bank Pool.
     * Uses 'this.pool' which is the correct PortfolioModel required by the UI.
     */
    protected void executeDiscardTrain(DiscardTrain action) {
        Train train = action.getSelectedTrain();
        if (train == null) {
            log.warn("DiscardTrain action has no train selected.");
            return;
        }

        // FIX: Use the 'pool' field directly defined in Round.java.
        // It is already a 'PortfolioModel', so no type mismatch occurs.
        PortfolioModel targetPool = this.pool;

        // 2. Move the train
        if (train.getCard() != null) {
            // Move the card (Certificate) to the pool
            train.getCard().moveTo(targetPool);
        } else {
            // Move the train object directly to the pool
            train.moveTo(targetPool);
        }

        // 3. Log/Report
        String companyName = action.getPlayer() != null ? action.getPlayer().getName() : "Company";
        if (action.getCompany() != null)
            companyName = action.getCompany().getId();

        String msg = companyName + " discards " + train.getName();
        ReportBuffer.add(this, msg);
        log.info(msg);
    }

    /**
     * MASTER FUNCTION: Checks train limit and generates discard actions if
     * necessary.
     * Returns TRUE if the company is over the limit (blocking normal play).
     */
    public boolean enforceTrainLimit(PublicCompany company) {
        if (company == null)
            return false;

        int count = company.getNumberOfTrains();
        int limit = company.getCurrentTrainLimit();

        if (count > limit) {
            log.info("LIMIT ENFORCEMENT: " + company.getId() + " has " + count + "/" + limit + " trains.");

            // CRITICAL: We clear actions HERE because it is a MANDATORY state.
            possibleActions.clear();

            // Generate buttons (without clearing internally)
            generateGroupedDiscardActions(company);

            return true; // Blocking
        }
        return false; // Not blocking
    }

    /**
     * Internal helper to generate buttons. Modified to provide individual actions
     * for each physical train, bypassing the buggy startsWith() grouping logic.
     */
    protected void generateGroupedDiscardActions(PublicCompany company) {
        List<net.sf.rails.game.Train> trains = new java.util.ArrayList<>(company.getPortfolioModel().getTrainList());
        java.util.Map<String, java.util.Set<net.sf.rails.game.Train>> typeGroups = new java.util.HashMap<>();

        for (net.sf.rails.game.Train train : trains) {
            if (train == null)
                continue;
            String type = train.getName().replaceAll("(.*)_\\d+", "$1");
            typeGroups.computeIfAbsent(type, k -> new java.util.HashSet<>()).add(train);
        }

        for (java.util.Map.Entry<String, java.util.Set<net.sf.rails.game.Train>> entry : typeGroups.entrySet()) {
            rails.game.action.DiscardTrain action = new rails.game.action.DiscardTrain(company, entry.getValue());
            action.setButtonLabel(entry.getKey());
            possibleActions.add(action);
        }
    }

    /**
     * Checks if a "Pass" action should terminate an interrupted round (e.g.,
     * Formation Round).
     * Returns true if the pass was handled and the round finished.
     */
    protected boolean handleInterruptedPass(PossibleAction action) {
        if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            // Only force-finish if this round actually interrupted another round (e.g. OR)
            if (gameManager.getInterruptedRound() != null) {
                log.info("1837_FIX: Interrupted round " + getRoundName() + " received PASS. Force-finishing.");
                finishRound();
                return true;
            }
        }
        return false;
    }

}
