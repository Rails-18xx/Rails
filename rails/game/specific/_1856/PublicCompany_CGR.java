package rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.move.*;
import rails.game.state.*;

public class PublicCompany_CGR extends PublicCompany {

	public static final String NAME = "CGR";

    /** Special rules apply before CGR has got its first permanent train */
    private BooleanState hadPermanentTrain;

    /** If no player has 2 shares, we need a separate attribute to mark the president. */
    private State temporaryPresident = null;

    /* Cope with multiple 5% share sales in one turn */
    private IntegerState sharesSoldSoFar;
    private IntegerState squaresDownSoFar;

    /** Initialisation, to be called directly after instantiation (cloning) */
    @Override
    public void init(String name, CompanyTypeI type) {
        super.init(name, type);
        hadPermanentTrain = new BooleanState (name+"_HadPermanentTrain", false);

        // Share price is initially fixed
        canSharePriceVary.set(false);

        sharesSoldSoFar = new IntegerState(name+"_SharesSoldSoFar", 0);
        squaresDownSoFar = new IntegerState(name+"_SquaresDownSoFar", 0);
    }

    public boolean hadPermanentTrain() {
        return hadPermanentTrain.booleanValue();
    }

    public void setHadPermanentTrain(boolean hadPermanentTrain) {
        this.hadPermanentTrain.set(hadPermanentTrain);
        canSharePriceVary.set(true);
    }

    public boolean hasTemporaryPresident () {
    	return getTemporaryPresident() != null;
    }

    public Player getTemporaryPresident() {
    	if (temporaryPresident != null) {
    		return (Player) temporaryPresident.getObject();
    	} else {
    		return null;
    	}
	}
    
    public boolean mayBuyTrainType (TrainI train) {
        return !"4".equals(train.getName());
    }

    @Override
	public Player getPresident() {
    	if (hasTemporaryPresident()) {
    		return getTemporaryPresident();
    	} else {
    		return super.getPresident();
    	}
    }

	public void setTemporaryPresident(Player temporaryPresident) {
		if (this.temporaryPresident == null) {
			this.temporaryPresident = new State ("CGR_TempPres", Player.class);
		}
		this.temporaryPresident.set(temporaryPresident);
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
        if (amount > 0) new CashMove(bank, this, amount);
        if (hasStockPrice && !runsWithBorrowedTrain()) {
            stockMarket.withhold(this);
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
            MoveableHolderI scrapHeap = bank.getScrapHeap();
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
	public void adjustSharePrice (int actionPerformed, int numberOfSharesSold,
            StockMarketI stockMarket) {

        if (actionPerformed == StockRound.SOLD) {
            if (canSharePriceVary()) {
                int numberOfSpaces;
                if (shareUnit.intValue() == 5) {
                    // Take care for selling 5% shares in multiple blocks per turn
                    numberOfSpaces
                        = (sharesSoldSoFar.intValue() + numberOfSharesSold)/2
                        - squaresDownSoFar.intValue();
                    sharesSoldSoFar.add(numberOfSharesSold);
                    squaresDownSoFar.add(numberOfSpaces);
                } else {
                    numberOfSpaces = numberOfSharesSold;
                }
                stockMarket.sell(this, numberOfSpaces);
            }
        }
    }

    @Override
	public void setOperated() {
        super.setOperated();

        // Reset the share selling counts
        // TODO Should this be a generic function?
        sharesSoldSoFar.set(0);
        squaresDownSoFar.set(0);
    }

    @Override
	public boolean mustOwnATrain() {
    	if (!hadPermanentTrain()) {
    		return false;
    	} else {
    		return super.mustOwnATrain();
    	}
    }

    @Override
	public String getExtraShareMarks () {
    	return (hasTemporaryPresident() ? "T" : "");
    }
}
