package rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.move.MoveableHolderI;
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

    public void setShareUnit (int percentage) {
        // Only allowed for CGR, the value must be 10
        if (name.equalsIgnoreCase("CGR") && shareUnit.intValue() == 5 
                && percentage == 10) {
            shareUnit.set(percentage);
            // Drop the last 10 shares
            List<PublicCertificateI>certs = new ArrayList<PublicCertificateI>(certificates);
            int share = 0;
            MoveableHolderI scrapHeap = Bank.getScrapHeap();
            for (PublicCertificateI cert : certs) {
                if (share >= 100) {
                    cert.moveTo(scrapHeap);
                    certificates.remove(cert);
                } else {
                    share += cert.getShare();
                }
            }
            
            // Update all owner ShareModels (once)
            // to have the UI get the correct percentage
            List<Portfolio> done = new ArrayList<Portfolio>();
            Portfolio portfolio;
            for (PublicCertificateI cert : certificates) {
                portfolio = (Portfolio)cert.getHolder();
                if (!done.contains(portfolio)) {
                    portfolio.getShareModel(this).setShare();
                    done.add(portfolio);
                }
            }
        }
        
    }
    
}
