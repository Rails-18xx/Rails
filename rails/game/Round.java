/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Round.java,v 1.42 2010/05/25 20:27:17 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.common.*;
import rails.game.action.*;
import rails.game.move.*;
import rails.game.special.SpecialPropertyI;
import rails.game.state.BooleanState;

/**
 * @author Erik Vos
 */
public abstract class Round implements RoundI {

    protected PossibleActions possibleActions = PossibleActions.getInstance();
    protected GuiHints guiHints = null;

    protected static Logger log =
        Logger.getLogger(Round.class.getPackage().getName());

    protected GameManagerI gameManager = null;
    protected CompanyManagerI companyManager = null;
    protected PlayerManager playerManager = null;
    protected Bank bank = null;
    protected Portfolio ipo = null;
    protected Portfolio pool = null;
    protected Portfolio unavailable = null;
    protected Portfolio scrapHeap = null;
    protected StockMarketI stockMarket = null;
    protected MapManager mapManager = null;

    //protected Class<? extends RoundI> roundTypeForUI = null;
    protected BooleanState wasInterrupted = new BooleanState  ("RoundInterrupted", false);

    protected MoveStack moveStack = null;


    /** Autopasses */
    protected List<Player> autopasses = null;
    protected List<Player> canRequestTurn = null;
    protected List<Player> hasRequestedTurn = null;

    /**
     * Constructor with the GameManager, will call setGameManager with the parameter to initialize
     *
     * @param aGameManager The GameManager Object needed to initialize the Round Class
     *
     */
    public Round (GameManagerI aGameManager) {

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

            moveStack = gameManager.getMoveStack();
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

    public PhaseI getCurrentPhase() {
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

        moveStack.start(true);
        if (linkedMoveSet) moveStack.linkToPreviousMoveSet();

        if (exchanged > 0) {
            MapHex hex;
            Stop city;
            String cityName, hexName;
            int cityNumber;
            String[] ct;
            PublicCompanyI comp = action.getCompany();

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
                                comp.getName(),
                                token.getOldCompanyName(),
                                city.getName()));
                        comp.layBaseToken(hex, 0);
                    }
                } else {
                    ReportBuffer.add(LocalText.getText("NoBaseTokenExchange",
                            comp.getName(),
                            token.getOldCompanyName(),
                            city.getName()));
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
    public List<PublicCompanyI> setOperatingCompanies() {
        return setOperatingCompanies (null, null);
    }

    public List<PublicCompanyI> setOperatingCompanies(List<PublicCompanyI> oldOperatingCompanies,
            PublicCompanyI lastOperatingCompany) {

        Map<Integer, PublicCompanyI> operatingCompanies =
            new TreeMap<Integer, PublicCompanyI>();
        List<PublicCompanyI> newOperatingCompanies;
        StockSpaceI space;
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

        for (PublicCompanyI company : newOperatingCompanies) {
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
                + 100 * (space.getRow()+1)
                + space.getStackPosition(company);
            } else {
                key = 50 + ++minorNo;
            }
            operatingCompanies.put(new Integer(key), company);
        }

        return new ArrayList<PublicCompanyI>(operatingCompanies.values());
    }

    /** Can a public company operate? (Default version) */
    protected boolean canCompanyOperateThisRound (PublicCompanyI company) {
        return company.hasFloated() && !company.isClosed();
    }

    /**
     * Check if a company must be floated, and if so, do it. <p>This method is
     * included here because it is used in various types of Round.
     *
     * @param company
     */
    protected void checkFlotation(PublicCompanyI company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        if (getSoldPercentage(company) >= company.getFloatPercentage()) {
            // Company floats
            floatCompany(company);
        }
    }

    /** Determine sold percentage for floating purposes */
    protected int getSoldPercentage (PublicCompanyI company) {

        int soldPercentage = 0;
        for (PublicCertificateI cert : company.getCertificates()) {
            if (certCountsAsSold(cert)) {
                soldPercentage += cert.getShare();
            }
        }
        return soldPercentage;
    }

    /** Can be subclassed for games with special rules */
    protected boolean certCountsAsSold (PublicCertificateI cert) {
        Portfolio holder = cert.getPortfolio();
        CashHolder owner = holder.getOwner();
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
    protected void floatCompany(PublicCompanyI company) {

        // Move cash and shares where required
        int soldPercentage = getSoldPercentage(company);
        int cash = 0;
        int capitalisationMode = company.getCapitalisation();
        if (company.hasStockPrice()) {
            int capFactor = 0;
            int shareUnit = company.getShareUnit();
            if (capitalisationMode == PublicCompanyI.CAPITALISE_FULL) {
                // Full capitalisation as in 1830
                capFactor = 100 / shareUnit;
            } else if (capitalisationMode == PublicCompanyI.CAPITALISE_INCREMENTAL) {
                // Incremental capitalisation as in 1851
                capFactor = soldPercentage / shareUnit;
            } else if (capitalisationMode == PublicCompanyI.CAPITALISE_WHEN_BOUGHT) {
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
            new CashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getName(),
                    Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getName()));
        }

        if (capitalisationMode == PublicCompanyI.CAPITALISE_INCREMENTAL
                && company.canHoldOwnShares()) {
            List<Certificate> moving = new ArrayList<Certificate>();
            for (Certificate ipoCert : ipo.getCertificatesPerCompany(
                    company.getName())) {
                moving.add(ipoCert);
            }
            for (Certificate ipoCert : moving) {
                ipoCert.moveTo(company.getPortfolio());
            }
        }
    }

    protected void finishRound() {
        // Report financials
        ReportBuffer.add("");
        for (PublicCompanyI c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(LocalText.getText("Has", c.getName(),
                        Bank.format(c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(LocalText.getText("Has", p.getName(),
                    Bank.format(p.getCash())));
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

    protected void transferCertificate(Certificate cert, Portfolio newHolder) {

        cert.moveTo(newHolder);
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

    protected void pay (CashHolder from, CashHolder to, int amount) {
        if (to != null && amount != 0) {
            new CashMove (from, to, amount);
        }
    }

    protected void pay (Portfolio from, Portfolio to, int amount) {
        if (to != null && amount != 0) {
            new CashMove (from.getOwner(), to.getOwner(), amount);
        }
    }

    public GameManagerI getGameManager() {
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
            if (hasRequestedTurn == null) hasRequestedTurn = new ArrayList<Player>(2);
            if (!hasRequestedTurn.contains(player)) hasRequestedTurn.add(player);
            return true;
        }
        return false;
    }

    public boolean canRequestTurn (Player player) {
        return canRequestTurn != null && canRequestTurn.contains(player);
    }

    public void setCanRequestTurn (Player player, boolean value) {
        if (canRequestTurn == null) canRequestTurn = new ArrayList<Player>(4);
        if (value && !canRequestTurn.contains(player)) {
            new AddToList<Player>(canRequestTurn, player, "CanRequestTurn");
        } else if (!value && canRequestTurn.contains(player)) {
            new RemoveFromList<Player>(canRequestTurn, player, "CanRequestTurn");
        }
    }

    public void setAutopass (Player player, boolean value) {
        if (autopasses == null) autopasses = new ArrayList<Player>(4);
        if (value && !autopasses.contains(player)) {
            new AddToList<Player>(autopasses, player, "Autopasses");
        } else if (!value && autopasses.contains(player)) {
            new RemoveFromList<Player>(autopasses, player, "Autopasses");
        }
    }

    public boolean hasAutopassed (Player player) {
        return autopasses != null && autopasses.contains(player);
    }

    public List<Player> getAutopasses() {
        return autopasses;
    }

    /** A stub for processing actions triggered by a phase change.
     * Must be overridden by subclasses that need to process such actions.
     * @param name (required) The name of the action to be executed
     * @param value (optional) The value of the action to be executed, if applicable
     */
    public void processPhaseAction (String name, String value) {

    }
}
