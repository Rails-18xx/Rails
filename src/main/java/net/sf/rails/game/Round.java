package net.sf.rails.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;


// Cannot be abstract because must be instantiatable to make it stateful, see GameManager_1837.
public /*abstract*/ class Round extends RailsAbstractItem implements RoundFacade {

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
        // TODO: It would be good to work with BankPortfolio and Owner instead of PortfolioModels
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
     * @return The phase number
     */
    protected int getPhaseNumber () {
        // The index starts at 0, so we must add 2
        return gameManager.getCurrentPhase().getIndex() + 2;
    }

    /**
     * Set the operating companies in their current acting order
     */
    // What is the reason of that to have that here? => move to OR?
    // EV: No, also used in 1837/CoalExchangeRound, which is an SR type
    public List<PublicCompany> setOperatingCompanies() {
        return setOperatingCompanies(null, null);
    }

    public List<PublicCompany> setOperatingCompanies (String type) {
        List<PublicCompany> selectedCompanies = new ArrayList<>();
        for (PublicCompany comp : setOperatingCompanies()) {
            if (type.equals(comp.getType().getId())) {
                selectedCompanies.add (comp);
            }
        }
        return selectedCompanies;
    }

    // What is the reason of that to have that here => move to OR?
    // this is still required for 18EU StockRound as due to the merger there are companies that have to discard trains
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
            if (!reorder && !canCompanyOperateThisRound(company)) continue;

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
     * Check if a company must be floated, and if so, do it. <p>This method is
     * included here because it is used in various types of Round.
     *
     * @param company Company to be checked for being floatable
     */
    // What is the reason of that to have that here? => best to move it to PublicCompany in the long-run
    // is called by StartRound as well
    // called only internally
    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        if (company.getSoldPercentage() >= company.getFloatPercentage()) {
            // Company floats
            floatCompany(company);
        }
    }

    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Full capitalisation is implemented
     * as in 1830. Partial capitalisation is implemented as in 1851. Other ways
     * to process the consequences of company flotation must be handled in
     * game-specific subclasses.
     */
    // What is the reason of that to have that here? => move to SR?
    // called by checkFlotation above
    // move it to PublicCompany in the long-run
    // called only internally
    protected void floatCompany(PublicCompany company) {

        if (company.hasFloated()) return;

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

    protected int getCashOnFloating (PublicCompany company) {
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

    /** Stub, to be overridden where needed.
     * Used in 1826
     * @param company The floating public company
     * @return
     */
    protected int getCustomCapitalization (PublicCompany company) {
        return 0;
    }

    protected void finishRound () {
        finishRound (true);
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
        // Inform GameManager
        gameManager.nextRound(this);
    }

    // called only from 1835 Operating Round?
    public boolean wasInterrupted() {
        return wasInterrupted.value();
    }

    /** Stub to allow lower subclasses to provide their own window title */
    public String getOwnWindowTitle() {
        return null;
    }

}
