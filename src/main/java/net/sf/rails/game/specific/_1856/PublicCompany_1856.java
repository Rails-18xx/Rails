package net.sf.rails.game.specific._1856;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.IntegerState;

import com.google.common.collect.Iterables;


public final class PublicCompany_1856 extends PublicCompany {

    private final IntegerState trainNumberAvailableAtStart = IntegerState.create(this, "trainNumberAvailableAtStart");
    private final IntegerState moneyInEscrow = IntegerState.create(this, "moneyInEscrow");

    /** Used for CGR */
    // TODO: Is this still used, as CGR has it owns class
    private boolean hadPermanentTrain = false;

    public PublicCompany_1856(RailsItem parent, String id) {
        super(parent, id);    
    }

    @Override
    public void start(StockSpace startSpace) {

        super.start(startSpace);

        // TODO: Refactor the code duplication
        Train nextAvailableTrain
            = Iterables.get(getRoot().getTrainManager().getAvailableNewTrains(), 0);
        int trainNumber;
        try {
            trainNumber = Integer.parseInt(nextAvailableTrain.toText());
        } catch (NumberFormatException e) {
            trainNumber = 6; // Diesel!
        }
        trainNumberAvailableAtStart.set(trainNumber);

        if (trainNumber == 6) {
            this.capitalisation = CAPITALISE_FULL;
        }

    }

    public int getTrainNumberAvailableAtStart () {
        return trainNumberAvailableAtStart.value();
    }

    public void setMoneyInEscrow (int amount) {
        moneyInEscrow.set(amount);
    }

    public void addMoneyInEscrow (int amount) {
        moneyInEscrow.add(amount);
    }

    public int getMoneyInEscrow () {
       return moneyInEscrow.value();
    }

    public boolean hadPermanentTrain() {
        return hadPermanentTrain;
    }

    @Override
    public void buyTrain(Train train, int price) {
        super.buyTrain (train, price);
        if (train.isPermanent()) hadPermanentTrain = true;
    }
    
    public int getGameEndPrice() {
        return Math.max(0, getMarketPrice() - 10 * getCurrentNumberOfLoans());
    }

}
