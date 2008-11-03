package rails.game.specific._1856;

import rails.game.PublicCompanyI;
import rails.game.StockRound;

public class StockRound_1856 extends StockRound {

    /**
     * Special 1856 code to check for company flotation.
     * 
     * @param company
     */
    protected void checkFlotation(PublicCompanyI company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int unsoldPercentage = company.getUnsoldPercentage();
        int floatPercentage = getCurrentPhase().getParameterAsInteger("floatPercentage");
        
        if (floatPercentage == 0) {
            log.error ("FloatPercentage is null!");
        } else {
            log.debug ("Floatpercentage is "+floatPercentage);
        }
        
        //int floatPercentage = Math.min(60, (Integer)getCurrentPhase().getParameter("floatPercentage"));

        if (unsoldPercentage <= 100 - floatPercentage) {
            // Company floats
            floatCompany(company);
        }
    }

}
