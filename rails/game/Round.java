/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Round.java,v 1.7 2008/03/11 20:00:33 evos Exp $
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
public class Round implements RoundI {

    protected PossibleActions possibleActions = PossibleActions.getInstance();

	protected static Logger log = Logger.getLogger(Round.class.getPackage().getName());

    /**
     *
     */
    public Round() {
        super();
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see rails.game.RoundI#getCurrentPlayer()
     */
    public Player getCurrentPlayer() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see rails.game.RoundI#getHelp()
     */
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see rails.game.RoundI#getSpecialProperties()
     */
    public List<SpecialPropertyI> getSpecialProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean process (PossibleAction action) {
    	return true;
    }

    /** Default version, does nothing.
     * Subclasses should override this method with a real version.
     * @return
     */
    public boolean setPossibleActions () {
        return false;
    }

    /** Get the operating companies in their current acting order */
    public PublicCompanyI[] getOperatingCompanies() {

        List<PublicCompanyI> companies
            = Game.getCompanyManager().getAllPublicCompanies();
        Map<Integer, PublicCompanyI> operatingCompanies
            = new TreeMap<Integer, PublicCompanyI>();
        StockSpaceI space;
        int key;
        int minorNo = 0;
        for (PublicCompanyI company : companies)
        {
            if (!company.hasFloated() || company.isClosed())
                continue;
            // Key must put companies in reverse operating order, because sort
            // is ascending.
            if (company.hasStockPrice())
            {
                space = company.getCurrentPrice();
                key = 1000000 * (999 - space.getPrice()) + 10000
                        * (99 - space.getColumn()) + 100 * space.getRow()
                        + space.getStackPosition(company);
            }
            else
            {
                key = ++minorNo;
            }
            operatingCompanies.put(new Integer(key), company);
        }
        return operatingCompanies.values()
                .toArray(new PublicCompanyI[0]);
    }

    /** Check if a company must be floated, and if so, do it.
     * <p>This method is included here because it is used in various types of Round.
     * @param company
     */
    protected void checkFlotation (PublicCompanyI company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int unsoldPercentage = company.getUnsoldPercentage();

        if (unsoldPercentage <= 100 - company.getFloatPercentage()) {
            // Company floats
            floatCompany (company);
        }
    }

    /** Float a company, including a default implementation
     * of moving cash and shares as a result of flotation.
     * <p>Full capitalisation is implemented as in 1830.
     * Partial capitalisation is implemented as in 1851.
     * Other ways to process the consequences of company
     * flotation must be handled in game-specific subclasses.
     * */
    protected void floatCompany (PublicCompanyI company) {

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
            }
            int price = (company.hasParPrice()
                    ? company.getParPrice()
                    : company.getCurrentPrice())
                .getPrice();
            cash = capFactor * price;
        } else {
            cash = company.getFixedPrice();
        }

        cash -= company.getBaseTokensBuyCost();

        company.setFloated(); // After calculating cash (for 1851: price goes up)

        new CashMove(Bank.getInstance(), company, cash);
        ReportBuffer.add(LocalText.getText("FloatsWithCash",
                new String[] {
                company.getName(),
                Bank.format(cash)
                }));

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
    public String toString () {
        return getClass().getName().replaceAll(".*\\.", "");
    }

}
