package rails.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import rails.common.*;
import rails.game.action.*;
import rails.game.model.CashOwner;
import rails.game.model.Owner;
import rails.game.model.Portfolio;
import rails.game.special.SpecialPropertyI;
import rails.game.state.GameItem;
import rails.game.state.ArrayListState;
import rails.game.state.BooleanState;
import rails.game.state.ChangeStack;
import rails.game.model.Owners;

/**
 * @author Erik Vos
 */
public abstract class Round extends GameItem implements RoundI {

    protected PossibleActions possibleActions = PossibleActions.getInstance();
    protected GuiHints guiHints = null;

    protected static Logger log =
        Logger.getLogger(Round.class.getPackage().getName());

    protected GameManager gameManager = null;
    protected CompanyManagerI companyManager = null;
    protected PlayerManager playerManager = null;
    protected Bank bank = null;
    protected Portfolio ipo = null;
    protected Portfolio pool = null;
    protected Portfolio unavailable = null;
    protected Portfolio scrapHeap = null;
    protected StockMarket stockMarket = null;
    protected MapManager mapManager = null;

    //protected Class<? extends RoundI> roundTypeForUI = null;
    protected BooleanState wasInterrupted = BooleanState.create(this, "RoundInterrupted", false);

    protected ChangeStack changeStack = null;


    /** Autopasses */
    protected ArrayListState<Player> autopasses = null;
    protected ArrayListState<Player> canRequestTurn = null;
    protected ArrayListState<Player> hasRequestedTurn = null;

    /**
     * Constructor with the GameManager, will call setGameManager with the parameter to initialize
     *
     * @param aGameManager The GameManager Object needed to initialize the Round Class
     *
     */
    public Round (GameManager aGameManager) {

        this.gameManager = aGameManager;

        if (gameManager == null) {
            companyManager = null;
        } else {
            companyManager = gameManager.getCompanyManager();
            playerManager = gameManager.getPlayerManager();
            bank = gameManager.getBank();
            ipo = bank.getIpo();
            pool = bank.getPool();
            unavailable = bank.getUnavailable();
            scrapHeap = bank.getScrapHeap();
            stockMarket = gameManager.getStockMarket();
            mapManager = gameManager.getMapManager();

            changeStack = gameManager.getChangeStack();
        }

        //roundTypeForUI = getClass();
        guiHints = gameManager.getUIHints();
        guiHints.setCurrentRoundType(getClass());
    }

    /*
     * (non-Javadoc)
     *
     * @see rails.game.RoundI#getCurrentPlayer()
     */
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
    public Class<? extends RoundI> getRoundTypeForUI () {
        return this.getClass();
    }

    //public void setRoundTypeForUI(Class<? extends RoundI> roundTypeForUI) {
    //    this.roundTypeForUI = roundTypeForUI;
    //}

    public String getGameOption (String name) {
        return gameManager.getGameOption(name);
    }
    /*
     * (non-Javadoc)
     *
     * @see rails.game.RoundI#getHelp()
     */
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see rails.game.RoundI#getSpecialProperties()
     */
    public List<SpecialPropertyI> getSpecialProperties() {
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

        // TODO: changeStack.start(true);
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
                                city.getId()));
                        comp.layBaseToken(hex, 0);
                    }
                } else {
                    ReportBuffer.add(LocalText.getText("NoBaseTokenExchange",
                            comp.getId(),
                            token.getOldCompanyName(),
                            city.getId()));
                }
            }
        }

        return true;
    }


    /**
     * Default version, does nothing. Subclasses should override this method
     * with a real version.
     *
     * @return
     */
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
        Portfolio holder = cert.getPortfolio();
        Owner owner = holder.getOwner();
        return owner instanceof Player
        || holder == pool;
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
            Owners.cashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getId(),
                    Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getId()));
        }

        if (capitalisationMode == PublicCompany.CAPITALISE_INCREMENTAL
                && company.canHoldOwnShares()) {
            // move all shares from ipo to the company portfolio
            Owners.moveAll(ipo, company, PublicCertificate.class);
        }
    }

    protected void finishRound() {
        // Report financials
        ReportBuffer.add("");
        for (PublicCompany c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(LocalText.getText("Has", c.getId(),
                        Bank.format(c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(LocalText.getText("Has", p.getId(),
                    Bank.format(p.getCashValue())));
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
        return wasInterrupted.booleanValue();
    }

    @Override
    public String toString() {
        return getClass().getName().replaceAll(".*\\.", "");
    }

    protected void transferCertificate(Certificate cert, Owner newOwner) {

        cert.moveTo(newOwner);
    }

    // Note: all transferred shares must come from the same old shareholder.
    protected void transferCertificates(List<? extends Certificate> certs,
            Portfolio newHolder) {

        for (Certificate cert : certs) {
            if (cert != null) {
                cert.moveTo(newHolder);
            }
        }
    }

    protected void pay (CashOwner from, CashOwner to, int amount) {
        if (to != null && amount != 0) {
            Owners.cashMove (from, to, amount);
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
        if (canRequestTurn (player)) {
            if (hasRequestedTurn == null) hasRequestedTurn = ArrayListState.create(this, "hasRequestedTurn");
            if (!hasRequestedTurn.contains(player)) hasRequestedTurn.add(player);
            return true;
        }
        return false;
    }

    public boolean canRequestTurn (Player player) {
        return canRequestTurn != null && canRequestTurn.contains(player);
    }

    public void setCanRequestTurn (Player player, boolean value) {
        if (canRequestTurn == null) canRequestTurn = ArrayListState.create(this, "canRequestTurn");
        if (value && !canRequestTurn.contains(player)) {
            canRequestTurn.add(player);
        } else if (!value && canRequestTurn.contains(player)) {
            canRequestTurn.remove(player);
        }
    }

    public void setAutopass (Player player, boolean value) {
        if (autopasses == null) autopasses = ArrayListState.create(this, "autopasses");
        if (value && !autopasses.contains(player)) {
            autopasses.add(player);
        } else if (!value && autopasses.contains(player)) {
            autopasses.remove(player);
        }
    }

    public boolean hasAutopassed (Player player) {
        return autopasses != null && autopasses.contains(player);
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
