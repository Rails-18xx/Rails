package rails.game.specific._1856;

import rails.game.PublicCompany;
import rails.game.StockSpaceI;
import rails.game.TrainI;
import rails.game.TrainManager;
import rails.game.state.IntegerState;

public class PublicCompany_1856 extends PublicCompany {

    private IntegerState trainNumberAvailableAtStart;
    
    private IntegerState moneyInEscrow;
    
    public void start(StockSpaceI startSpace) {
        
        super.start(startSpace);
        
        TrainI nextAvailableTrain = TrainManager.get().getAvailableNewTrains().get(0);
        int trainNumber;
        try { 
            trainNumber = Integer.parseInt(nextAvailableTrain.getName());
        } catch (NumberFormatException e) {
            trainNumber = 6; // Diesel!
        }
        trainNumberAvailableAtStart
                = new IntegerState (name+"_trainAtStart");
        trainNumberAvailableAtStart.set(trainNumber);
        
        if (trainNumber == 6) {
            this.capitalisation = CAPITALISE_FULL;
        }
        
        moneyInEscrow
                = new IntegerState (name+"_moneyInEscrow", 0);
    }
    
    public int getTrainNumberAvailableAtStart () {
        return trainNumberAvailableAtStart.intValue();
    }
    
    public void setMoneyInEscrow (int amount) {
        moneyInEscrow.set(amount);
    }

    public void addMoneyInEscrow (int amount) {
        moneyInEscrow.add(amount);
    }
    
    public int getMoneyInEscrow () {
       return moneyInEscrow.intValue();
    }
}
