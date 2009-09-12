package rails.game.specific._1856;

import rails.game.*;
import rails.game.state.IntegerState;

public class PublicCompany_1856 extends PublicCompany {

    private IntegerState trainNumberAvailableAtStart;

    private IntegerState moneyInEscrow;

    /** Used for CGR */
    private boolean hadPermanentTrain = false;

    @Override
    public void start(StockSpaceI startSpace) {

        super.start(startSpace);

        TrainI nextAvailableTrain
            = gameManager.getTrainManager().getAvailableNewTrains().get(0);
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

    public boolean hadPermanentTrain() {
        return hadPermanentTrain;
    }

    @Override
    public void buyTrain(TrainI train, int price) {
        super.buyTrain (train, price);
        if (train.getType().isPermanent()) hadPermanentTrain = true;
    }
    
    public int getGameEndPrice() {
        return Math.max(0, getMarketPrice() - 10 * getCurrentNumberOfLoans());
    }

}
