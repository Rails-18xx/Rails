package net.sf.rails.game.specific._1856;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.IntegerState;


public final class PublicCompany_1856 extends PublicCompany {

    private final IntegerState trainNumberAvailableAtStart = IntegerState.create(this, "trainNumberAvailableAtStart");
    private final IntegerState moneyInEscrow = IntegerState.create(this, "moneyInEscrow");

    public PublicCompany_1856(RailsItem parent, String id) {
        super(parent, id);    
    }

    @Override
    public void start(StockSpace startSpace) {

        super.start(startSpace);

        int trainNumber = ((GameManager_1856)getRoot().getGameManager()).getNextTrainNumberFromIpo();
        trainNumberAvailableAtStart.set(trainNumber);

        if (trainNumber == 6) {
            this.capitalisation = CAPITALISE_FULL;
        }

    }

    public int getTrainNumberAvailableAtStart () {
        return trainNumberAvailableAtStart.value();
    }

    public void addMoneyInEscrow (int amount) {
        moneyInEscrow.add(amount);
    }

    public int getMoneyInEscrow () {
       return moneyInEscrow.value();
    }

    @Override
    public void buyTrain(Train train, int price) {
        super.buyTrain (train, price);
        //if (train.isPermanent()) hadPermanentTrain = true;
    }
    
    public int getGameEndPrice() {
        return Math.max(0, getMarketPrice() - 10 * getCurrentNumberOfLoans());
    }

    @Override
    public void setClosed() {

        super.setClosed();
        ((GameManager_1856) getRoot().getGameManager()).resetCertificateLimit(false);
        //Note: The Phase Check is done inside resetCertificateLimit();
        
    }

}
