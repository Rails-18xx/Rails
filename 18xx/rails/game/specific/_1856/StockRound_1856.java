package rails.game.specific._1856;

import rails.game.PublicCompanyI;
import rails.game.StockRound;
import rails.game.TrainI;
import rails.game.TrainManager;

public class StockRound_1856 extends StockRound {

    /**
     * Special 1856 code to check for company flotation.
     * 
     * @param company
     */
    protected void checkFlotation(PublicCompanyI company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int unsoldPercentage = company.getUnsoldPercentage();
        
        TrainI nextAvailableTrain = TrainManager.get().getAvailableNewTrains().get(0);
        int trainNumber;
        try { 
            trainNumber = Integer.parseInt(nextAvailableTrain.getName());
        } catch (NumberFormatException e) {
            trainNumber = 6; // Diesel!
        }
        int floatPercentage = 10 * trainNumber;
        
        log.debug ("Floatpercentage is "+floatPercentage);
        
        //int floatPercentage = Math.min(60, (Integer)getCurrentPhase().getParameter("floatPercentage"));

        if (unsoldPercentage <= 100 - floatPercentage) {
            // Company floats
            floatCompany(company);
        }
    }

}
