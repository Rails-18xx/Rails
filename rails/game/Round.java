/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Round.java,v 1.6 2008/03/05 19:55:14 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game;


import java.util.*;

import org.apache.log4j.Logger;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.special.SpecialPropertyI;

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



    @Override
    public String toString () {
        return getClass().getName().replaceAll(".*\\.", "");
    }

}
