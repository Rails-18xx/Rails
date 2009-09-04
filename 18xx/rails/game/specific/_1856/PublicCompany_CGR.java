package rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.move.*;
import rails.game.state.BooleanState;

public class PublicCompany_CGR extends PublicCompany {

	public static final String NAME = "CGR";

    /** Used for CGR */
    private BooleanState hadPermanentTrain;

    /** Initialisation, to be called directly after instantiation (cloning) */
    @Override
    public void init(String name, CompanyTypeI type) {
        super.init(name, type);
        hadPermanentTrain = new BooleanState (name+"_HadPermanentTrain", false);

        // Share price is initially fixed
        canSharePriceVary.set(false);
    }

    public boolean hadPermanentTrain() {
        return hadPermanentTrain.booleanValue();
    }

    public void setHadPermanentTrain(boolean hadPermanentTrain) {
        this.hadPermanentTrain.set(hadPermanentTrain);
        canSharePriceVary.set(true);
    }

    @Override
	public boolean canRunTrains() {
        if (!hadPermanentTrain()) {
            return true;
        }
        return getNumberOfTrains() > 0;
    }

    public boolean runsWithBorrowedTrain () {
        return !hadPermanentTrain() && getNumberOfTrains() == 0;
    }

    /**
     * CGR share price does not move until a permanent train is bought.
     *
     * @param The revenue amount.
     */
    @Override
	public void withhold(int amount) {
        if (amount > 0) new CashMove(null, this, amount);
        if (hasStockPrice && !runsWithBorrowedTrain()) {
            Game.getStockMarket().withhold(this);
        }
    }

    @Override
    public void buyTrain(TrainI train, int price) {
        super.buyTrain (train, price);
        if (train.getType().isPermanent()) setHadPermanentTrain(true);
    }

    public void setShareUnit (int percentage) {
        // Only allowed for CGR, the value must be 10
        if (shareUnit.intValue() == 5
                && percentage == 10) {
            shareUnit.set(percentage);
            // Drop the last 10 shares
            List<PublicCertificateI>certs = new ArrayList<PublicCertificateI>(certificates);
            int share = 0;
            MoveableHolderI scrapHeap = Bank.getScrapHeap();
            for (PublicCertificateI cert : certs) {
                if (share >= 100) {
                    cert.moveTo(scrapHeap);
                    new RemoveFromList<PublicCertificateI>(certificates, cert, "CGR_Certs");
                } else {
                    cert.setCertificateCount(1.0f);
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

    @Override
	public boolean mustOwnATrain() {
    	if (!hadPermanentTrain()) {
    		return false;
    	} else {
    		return super.mustOwnATrain();
    	}
    }


}
