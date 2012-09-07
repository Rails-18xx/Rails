package rails.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.*;
import rails.game.action.*;
import rails.game.model.PortfolioModel;
import rails.game.special.SpecialProperty;
import rails.game.state.ArrayListState;
import rails.game.state.BooleanState;
import rails.game.state.ChangeStack;
import rails.game.state.Creatable;
import rails.game.state.Owner;
import rails.game.state.Portfolio;

public abstract class Round extends RailsAbstractItem implements Creatable {

    protected PossibleActions possibleActions = PossibleActions.getInstance();
    protected GuiHints guiHints = null;

    protected static Logger log =
        LoggerFactory.getLogger(Round.class);

    protected GameManager gameManager = null;
    protected CompanyManager companyManager = null;
    protected PlayerManager playerManager = null;
    protected Bank bank = null;
    protected PortfolioModel ipo = null;
    protected PortfolioModel pool = null;
    protected PortfolioModel unavailable = null;
    protected PortfolioModel scrapHeap = null;
    protected StockMarket stockMarket = null;
    protected MapManager mapManager = null;

    protected final BooleanState wasInterrupted = BooleanState.create(this, "wasInterrupted");


    /** Autopasses */
    // TODO: Should this be moved to the StockRound classes?
    private final ArrayListState<Player> autopasses = ArrayListState.create(this, "autopasses");
    private final ArrayListState<Player> canRequestTurn = ArrayListState.create(this, "canRequestTurn");
    private final ArrayListState<Player> hasRequestedTurn = ArrayListState.create(this, "hasRequestedTurn");

    protected Round (GameManager parent, String id) {
        super(parent, id);

        this.gameManager = parent;

        if (gameManager == null) {
            companyManager = null;
        } else {
            companyManager = gameManager.getCompanyManager();
            playerManager = gameManager.getPlayerManager();
            bank = gameManager.getBank();
            // TODO: It would be good to work with BankPortfolio and Owner instead of PortfolioModels
            // However this requires a lot of work inside the Round classes
            ipo = bank.getIpo().getPortfolioModel();
            pool = bank.getPool().getPortfolioModel();
            unavailable = bank.getUnavailable().getPortfolioModel();
            scrapHeap = bank.getScrapHeap().getPortfolioModel();
            stockMarket = gameManager.getStockMarket();
            mapManager = gameManager.getMapManager();

            // changeStack = gameManager.getChangeStack();
        }

        guiHints = gameManager.getUIHints();
        guiHints.setCurrentRoundType(getClass());
    }
    
    public Player getCurrentPlayer() {

        if (gameManager != null) return gameManager.getCurrentPlayer();
        return null;
    }

    /**
     * @return Returns the currentPlayerIndex.
     */
    public int getCurrentPlayerIndex() {
        return getCurrentPlayer().getIndex();
    }

    public void setCurrentPlayerIndex(int newIndex) {
        gameManager.setCurrentPlayerIndex(newIndex);
    }

    public void setCurrentPlayer(Player player) {
        gameManager.setCurrentPlayer(player);
    }

    protected List<Player> getPlayers() {
        return gameManager.getPlayers();
    }

    protected int getNumberOfPlayers() {
        return gameManager.getNumberOfPlayers();
    }

    protected int getNumberOfActivePlayers () {
        int number = 0;
        for (Player player : getPlayers()) {
            if (!player.isBankrupt()) number++;
        }
        return number;
    }

    public Phase getCurrentPhase() {
        return gameManager.getCurrentPhase();
    }

    /** Allows round instances to tell the UI what type of window to raise.
     * Normally the type corresponds to the round type (e.g. OperatingRound
     * needs ORWindow), but sometimes deviations occur (such as the
     * CGRFormationRound, which isn't a StockRound type but needs StatusWindow).
     * @return
     */
    public Class<? extends Round> getRoundTypeForUI () {
        return this.getClass();
    }

    public String getGameOption (String name) {
        return gameManager.getGameOption(name);
    }

    // TODO: Remove as this is abstract class?
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO: Remove as this is abstract class?
    public List<SpecialProperty> getSpecialProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean process(PossibleAction action) {
        return true;
    }

    protected boolean exchangeTokens(ExchangeTokens action, boolean linkedMoveSet) {

        String errMsg = null;

        List<ExchangeableToken> tokens = action.getTokensToExchange();
        int min = action.getMinNumberToExchange();
        int max = action.getMaxNumberToExchange();
        int exchanged = 0;

        checks: {

            for (ExchangeableToken token : tokens) {
                if (token.isSelected()) exchanged++;
            }
            if (exchanged < min || exchanged > max) {
                errMsg = LocalText.getText("WrongNumberOfTokensExchanged",
                        action.getCompany(),
                        min, max, exchanged);
                break checks;
            }
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotExchangeTokens",
                    action.getCompany(),
                    action.toString(),
                    errMsg));

            return false;
        }

        ChangeStack.start(this, action);
        // FIMXE: if (linkedMoveSet) changeStack.linkToPreviousMoveSet();

        if (exchanged > 0) {
            MapHex hex;
            Stop city;
            String cityName, hexName;
            int cityNumber;
            String[] ct;
            PublicCompany comp = action.getCompany();

            ReportBuffer.add("");

            for (ExchangeableToken token : tokens) {
                cityName = token.getCityName();
                ct = cityName.split("/");
                hexName = ct[0];
                try {
                    cityNumber = Integer.parseInt(ct[1]);
                } catch (NumberFormatException e) {
                    cityNumber = 1;
                }
                hex = mapManager.getHex(hexName);
                city = hex.getStop(cityNumber);

                if (token.isSelected()) {

                    // For now we'll assume that the old token(s) have already been removed.
                    // This is true in the 1856 CGR formation.
                    if (hex.layBaseToken(comp, city.getNumber())) {
                        /* TODO: the false return value must be impossible. */
                        ReportBuffer.add(LocalText.getText("ExchangesBaseToken",
                                comp.getId(),
                                token.getOldCompanyName(),
                                city.getSpecificId()));
                        comp.layBaseToken(hex, 0);
                    }
                } else {
                    ReportBuffer.add(LocalText.getText("NoBaseTokenExchange",
                            comp.getId(),
                            token.getOldCompanyName(),
                            city.getSpecificId()));
                }
            }
        }

        return true;
    }


    /**
     * Default version, does nothing. Subclasses should override this method
     * with a real version.
     */
    // TODO: Remove as this is abstract class?
    public boolean setPossibleActions() {
        return false;
    }

    /** Set the operating companies in their current acting order */
    public List<PublicCompany> setOperatingCompanies() {
        return setOperatingCompanies (null, null);
    }

    public List<PublicCompany> setOperatingCompanies(List<PublicCompany> oldOperatingCompanies,
            PublicCompany lastOperatingCompany) {

        Map<Integer, PublicCompany> operatingCompanies =
            new TreeMap<Integer, PublicCompany>();
        List<PublicCompany> newOperatingCompanies;
        StockSpace space;
        int key;
        int minorNo = 0;
        boolean reorder = gameManager.isDynamicOperatingOrder()
        && oldOperatingCompanies != null && lastOperatingCompany != null;

        int lastOperatingCompanyndex;
        if (reorder) {
            newOperatingCompanies = oldOperatingCompanies;
            lastOperatingCompanyndex = oldOperatingCompanies.indexOf(lastOperatingCompany);
        } else {
            newOperatingCompanies = companyManager.getAllPublicCompanies();
            lastOperatingCompanyndex = -1;
        }

        for (PublicCompany company : newOperatingCompanies) {
            if (!reorder && !canCompanyOperateThisRound(company)) continue;

            if (reorder
                    && oldOperatingCompanies.indexOf(company) <= lastOperatingCompanyndex) {
                // Companies that have operated this round get lowest keys
                key = oldOperatingCompanies.indexOf(company);
            } else if (company.hasStockPrice()) {
                // Key must put companies in reverse operating order, because sort
                // is ascending.
                space = company.getCurrentSpace();
                key = 1000000 * (999 - space.getPrice())
                + 10000 * (99 - space.getColumn())
                + 100 * (space.getRow()+1)
                + space.getStackPosition(company);
            } else {
                key = 50 + ++minorNo;
            }
            operatingCompanies.put(new Integer(key), company);
        }

        return new ArrayList<PublicCompany>(operatingCompanies.values());
    }

    /** Can a public company operate? (Default version) */
    protected boolean canCompanyOperateThisRound (PublicCompany company) {
        return company.hasFloated() && !company.isClosed();
    }

    /**
     * Check if a company must be floated, and if so, do it. <p>This method is
     * included here because it is used in various types of Round.
     *
     * @param company
     */
    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        if (getSoldPercentage(company) >= company.getFloatPercentage()) {
            // Company floats
            floatCompany(company);
        }
    }

    /** Determine sold percentage for floating purposes */
    protected int getSoldPercentage (PublicCompany company) {

        int soldPercentage = 0;
        for (PublicCertificate cert : company.getCertificates()) {
            if (certCountsAsSold(cert)) {
                soldPercentage += cert.getShare();
            }
        }
        return soldPercentage;
    }

    /** Can be subclassed for games with special rules */
    protected boolean certCountsAsSold (PublicCertificate cert) {
        Owner owner = cert.getOwner();
        return owner instanceof Player || owner == pool.getParent();
    }

    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Full capitalisation is implemented
     * as in 1830. Partial capitalisation is implemented as in 1851. Other ways
     * to process the consequences of company flotation must be handled in
     * game-specific subclasses.
     */
    protected void floatCompany(PublicCompany company) {

        // Move cash and shares where required
        int soldPercentage = getSoldPercentage(company);
        int cash = 0;
        int capitalisationMode = company.getCapitalisation();
        if (company.hasStockPrice()) {
            int capFactor = 0;
            int shareUnit = company.getShareUnit();
            if (capitalisationMode == PublicCompany.CAPITALISE_FULL) {
                // Full capitalisation as in 1830
                capFactor = 100 / shareUnit;
            } else if (capitalisationMode == PublicCompany.CAPITALISE_INCREMENTAL) {
                // Incremental capitalisation as in 1851
                capFactor = soldPercentage / shareUnit;
            } else if (capitalisationMode == PublicCompany.CAPITALISE_WHEN_BOUGHT) {
                // Cash goes directly to treasury at each buy (as in 1856 before phase 6)
                capFactor = 0;
            }
            int price = company.getIPOPrice();
            cash = capFactor * price;
        } else {
            cash = company.getFixedPrice();
        }

        // Substract initial token cost (e.g. 1851, 18EU)
        cash -= company.getBaseTokensBuyCost();

        company.setFloated(); // After calculating cash (for 1851: price goes
        // up)

        if (cash > 0) {
            String cashText = Currency.fromBank(cash, company);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getId(),
                    cashText ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getId()));
        }

        if (capitalisationMode == PublicCompany.CAPITALISE_INCREMENTAL
                && company.canHoldOwnShares()) {
            // move all shares from ipo to the company portfolio
            // FIXME: Does this work correctly?
            Portfolio.moveAll(ipo.getCertificates(company), company);
        }
    }

    protected void finishRound() {
        // Report financials
        ReportBuffer.add("");
        for (PublicCompany c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(LocalText.getText("Has", c.getId(),
                        Currency.format(this, c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(LocalText.getText("Has", p.getId(),
                    Currency.format(this, p.getCashValue())));
        }
        // Inform GameManager
        gameManager.nextRound(this);
    }

    /** Generic stub to resume an interrupted round.
     * Only valid if implemented in a subclass.
     *
     */
    public void resume() {
        log.error("Calling Round.resume() is invalid");
    }

    public boolean wasInterrupted () {
        return wasInterrupted.value();
    }

    protected void transferCertificate(Certificate cert, PortfolioModel newHolder) {
        if (cert instanceof PublicCertificate) {
            ((PublicCertificate)cert).moveTo(newHolder.getParent());
        } else if (cert instanceof PrivateCompany) {
            newHolder.addPrivateCompany((PrivateCompany)cert);
        }
    }
    
    // Note: all transferred shares must come from the same old shareholder.
    // TODO: This is not very a very nice implementation
    protected void transferCertificates(List<? extends Certificate> certs,
            PortfolioModel newHolder) {

        for (Certificate cert : certs) {
            if (cert != null) {
                transferCertificate(cert, newHolder);
            }
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    protected Object getGameParameter (GameDef.Parm key) {
        return gameManager.getGameParameter(key);
    }

    public int getGameParameterAsInt (GameDef.Parm key) {
        if (key.defaultValue() instanceof Integer) {
            return (Integer) gameManager.getGameParameter(key);
        } else {
            return -1;
        }
    }

    public boolean getGameParameterAsBoolean (GameDef.Parm key) {
        if (key.defaultValue() instanceof Boolean) {
            return (Boolean) gameManager.getGameParameter(key);
        } else {
            return false;
        }
    }

    public String getRoundName() {
        return this.getClass().getSimpleName();
    }

    public boolean requestTurn (Player player) {
        if (canRequestTurn(player)) {
            if (!hasRequestedTurn.contains(player)) hasRequestedTurn.add(player);
            return true;
        }
        return false;
    }

    public boolean canRequestTurn (Player player) {
        return canRequestTurn.contains(player);
    }

    public void setCanRequestTurn (Player player, boolean value) {
        if (value && !canRequestTurn.contains(player)) {
            canRequestTurn.add(player);
        } else if (!value && canRequestTurn.contains(player)) {
            canRequestTurn.remove(player);
        }
    }

    public void setAutopass (Player player, boolean value) {
        if (value && !autopasses.contains(player)) {
            autopasses.add(player);
        } else if (!value && autopasses.contains(player)) {
            autopasses.remove(player);
        }
    }

    public boolean hasAutopassed (Player player) {
        return autopasses.contains(player);
    }

    public List<Player> getAutopasses() {
        return autopasses.view();
    }

    /** A stub for processing actions triggered by a phase change.
     * Must be overridden by subclasses that need to process such actions.
     * @param name (required) The name of the action to be executed
     * @param value (optional) The value of the action to be executed, if applicable
     */
    public void processPhaseAction (String name, String value) {

    }
}
