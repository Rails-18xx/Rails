package rails.game.specific._1856;

import rails.game.DisplayBuffer;
import rails.game.OperatingRound;
import rails.game.TrainI;
import rails.game.TrainManager;
import rails.game.state.IntegerState;
import rails.util.LocalText;

public class OperatingRound_1856 extends OperatingRound {

    /** 
     * Implements special rules for first time operating in 1856
     */
    protected boolean setNextOperatingCompany(boolean initial) {
        
        
        if (operatingCompanyIndexObject == null) {
            operatingCompanyIndexObject =
                    new IntegerState("OperatingCompanyIndex");
        }
        
        while (true) {
            if (initial) {
                operatingCompanyIndexObject.set(0);
                initial = false;
            } else {
                operatingCompanyIndexObject.add(1);
            }
            
            operatingCompanyIndex = operatingCompanyIndexObject.intValue();
            
            if (operatingCompanyIndex >= operatingCompanyArray.length) {
                return false;
            } else {
                operatingCompany = operatingCompanyArray[operatingCompanyIndex];
                
                // 1856 special: check if the company may operate
                if (!operatingCompany.hasOperated()) {
                    int soldPercentage 
                        = 100 - operatingCompany.getUnsoldPercentage();
                    
                    TrainI nextAvailableTrain = TrainManager.get().getAvailableNewTrains().get(0);
                    int trainNumber;
                    try { 
                        trainNumber = Integer.parseInt(nextAvailableTrain.getName());
                    } catch (NumberFormatException e) {
                        trainNumber = 6; // Diesel!
                    }
                    int floatPercentage = 10 * trainNumber;
                    
                    log.debug ("Float percentage is "+floatPercentage
                            +" sold percentage is "+soldPercentage);
                    

                    if (soldPercentage < floatPercentage) {
                        DisplayBuffer.add(LocalText.getText("MayNotYetOperate", new String[] {
                                operatingCompany.getName(),
                                String.valueOf(soldPercentage),
                                String.valueOf(floatPercentage)
                        }));
                        // Company may not yet operate
                        continue;
                    }
                    
                }
                return true;
            }
        }
    }

}
