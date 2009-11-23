/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Round.java,v 1.28 2009/11/23 18:32:44 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.game.action.*;
import rails.game.move.CashMove;
import rails.game.move.MoveStack;
import rails.game.special.SpecialPropertyI;
import rails.game.state.BooleanState;
import rails.util.LocalText;

/**
 * @author Erik Vos
 */
public abstract class Round implements RoundI {

    protected PossibleActions possibleActions = PossibleActions.getInstance();

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

    protected Class<? extends RoundI> roundTypeForUI = null;
    protected BooleanState wasInterrupted = new BooleanState  ("RoundInterrupted", false);

    protected MoveStack moveStack = null;

	/**
	 * Constructor with the GameManager, will call setGameManager with the parameter to initialize
	 *
	 * @param aGameManager The GameManager Object needed to initialize the Round Class
	 *
	 */
	public Round (GameManagerI aGameManager) {

        this.gameManager = aGameManager;

        if (aGameManager == null) {
            companyManager = null;
        } else {
            companyManager = aGameManager.getCompanyManager();
            playerManager = aGameManager.getPlayerManager();
            bank = aGameManager.getBank();
            ipo = bank.getIpo();
            pool = bank.getPool();
            unavailable = bank.getUnavailable();
            scrapHeap = bank.getScrapHeap();
            stockMarket = aGameManager.getStockMarket();
            mapManager = aGameManager.getMapManager();

            moveStack = aGameManager.getMoveStack();
        }

        roundTypeForUI = getClass();
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

    public void setRoundTypeForUI(Class<? extends RoundI> roundTypeForUI) {
        this.roundTypeForUI = roundTypeForUI;
    }

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

    protected boolean exchangeTokens (ExchangeTokens action) {

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

        if (exchanged > 0) {
            MapHex hex;
            City city;
            String cityName, hexName;
            int cityNumber;
            String[] ct;
            PublicCompanyI comp = action.getCompany();

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
                city = hex.getCity(cityNumber);

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

    /** Get the operating companies in their current acting order */
    public PublicCompanyI[] getOperatingCompanies() {

        List<PublicCompanyI> companies =
                companyManager.getAllPublicCompanies();
        Map<Integer, PublicCompanyI> operatingCompanies =
                new TreeMap<Integer, PublicCompanyI>();
        StockSpaceI space;
        int key;
        int minorNo = 0;
        for (PublicCompanyI company : companies) {
            if (!company.hasFloated() || company.isClosed()) continue;
            // Key must put companies in reverse operating order, because sort
            // is ascending.
            if (company.hasStockPrice()) {
                space = company.getCurrentSpace();
                key =
                        1000000 * (999 - space.getPrice()) + 10000
                                * (99 - space.getColumn()) + 100
                                * space.getRow()
                                + space.getStackPosition(company);
            } else {
                key = ++minorNo;
            }
            operatingCompanies.put(new Integer(key), company);
        }

        return operatingCompanies.values().toArray(new PublicCompanyI[0]);
    }

    /**
     * Check if a company must be floated, and if so, do it. <p>This method is
     * included here because it is used in various types of Round.
     *
     * @param company
     */
    protected void checkFlotation(PublicCompanyI company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int unsoldPercentage = company.getUnsoldPercentage();
        if (unsoldPercentage <= 100 - company.getFloatPercentage()) {
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
    protected void floatCompany(PublicCompanyI company) {

        // Move cash and shares where required
        int unsoldPercentage = company.getUnsoldPercentage();
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
                capFactor = (100 - unsoldPercentage) / shareUnit;
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

    protected void executeTradeCertificate(Certificate cert, Portfolio newHolder, int price) {

        Portfolio oldHolder = (Portfolio) cert.getHolder();
        cert.moveTo(newHolder);

        if (price != 0) {
            new CashMove(newHolder.getOwner(), oldHolder.getOwner(), price);
        }

    }

    public GameManagerI getGameManager() {
        return gameManager;
    }


}
