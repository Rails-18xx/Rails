/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Round.java,v 1.14 2008/12/23 19:55:29 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.move.CashMove;
import rails.game.special.SpecialPropertyI;
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

    /** Default constructor cannot be used */
    private Round () {}

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
        }

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
            new CashMove(Bank.getInstance(), company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash", new String[] {
                company.getName(), Bank.format(cash) }));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getName()));
        }

        if (capitalisationMode == PublicCompanyI.CAPITALISE_INCREMENTAL
            && company.canHoldOwnShares()) {
            List<Certificate> moving = new ArrayList<Certificate>();
            for (Certificate ipoCert : Bank.getIpo().getCertificatesPerCompany(
                    company.getName())) {
                moving.add(ipoCert);
            }
            for (Certificate ipoCert : moving) {
                ipoCert.moveTo(company.getPortfolio());
            }
        }
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

    /**
     * Who receives the cash when a certificate is bought.
     * Normally this is owner of the previously holding portfolio.
     * This method must be called <i>before</i> transferring the certificate.
     * @param cert
     * @param newHolder
     * @return
     */
    protected CashHolder getSharePriceRecipient (Certificate cert, int price) {
        return ((Portfolio)cert.getHolder()).getOwner();
    }

}
