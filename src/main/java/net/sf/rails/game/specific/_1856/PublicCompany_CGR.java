package net.sf.rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.GenericState;


public final class PublicCompany_CGR extends PublicCompany implements RevenueStaticModifier {

    public static final String NAME = "CGR";

    /**
     * Special rules apply before CGR has got its first permanent train
     */
    private final BooleanState hadPermanentTrain = new BooleanState(this, "hadPermanentTrain");

    /**
     * If no player has 2 shares, we need a separate attribute to mark the president.
     */
    private final GenericState<Player> temporaryPresident = new GenericState<>(this, "temporaryPresident");

    public PublicCompany_CGR(RailsItem parent, String id) {
        super(parent, id);
        // Share price is initially fixed
        // TODO: Is this the correct location or should that moved to some stage later?
        this.canSharePriceVary.set(false);
    }

    @Override
    public void finishConfiguration(RailsRoot root) throws ConfigurationException {
        super.finishConfiguration(root);

        // add revenue modifier for the case that there is no train
        getRoot().getRevenueManager().addStaticModifier(this);
    }

    public boolean hadPermanentTrain() {
        return this.hadPermanentTrain.value();
    }

    public void setHadPermanentTrain(boolean hadPermanentTrain) {
        this.hadPermanentTrain.set(hadPermanentTrain);
        this.canSharePriceVary.set(true);
    }

    public boolean hasTemporaryPresident() {
        return getTemporaryPresident() != null;
    }

    public Player getTemporaryPresident() {
        return this.temporaryPresident.value();
    }

    @Override
    public boolean mayBuyTrainType(Train train) {
        return !"4".equals(train.toText());
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
        this.temporaryPresident.set(temporaryPresident);
    }

    @Override
    public boolean hasTrains() {
        if (!hadPermanentTrain()) {
            return true;
        }
        return getNumberOfTrains() > 0;
    }

    public boolean runsWithBorrowedTrain() {
        return !hadPermanentTrain() && getNumberOfTrains() == 0;
    }

    /**
     * CGR share price does not move until a permanent train is bought.
     *
     * @param amount The revenue amount.
     */
    @Override
    public void withhold(int amount) {
        if (this.hasStockPrice && this.canSharePriceVary.value()) {
            getRoot().getStockMarket().withhold(this);
        }
    }

    @Override
    public void buyTrain(Train train, int price) {
        super.buyTrain(train, price);
        if (train.isPermanent()) {
            setHadPermanentTrain(true);
        }
    }

    @Override
    public void setShareUnit(int percentage) {

        int oldShareUnit = this.shareUnit.value();
        super.setShareUnit(percentage);

        // temp. fix to get rid of unnecessary certificates if size is 10.
        if (oldShareUnit == 5 && percentage == 10) {
            // Drop the last 10 shares
            //2018-10-07-MBr: With the remodeled base classes this approach isnt valid anymore as shares
            //with the id-10 are assigned before shares with the id-2
            List<PublicCertificate> certs = new ArrayList<>(this.certificates.view());
            BankPortfolio scrapHeap = getRoot().getBank().getScrapHeap();
            for (PublicCertificate cert : certs) {
                if (cert.getOwner().getId().equalsIgnoreCase("Unavailable")) {
                    cert.moveTo(scrapHeap);
                    this.certificates.remove(cert);
                } else {
                    cert.setCertificateCount(1.0f);
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

    @Override
    public String getExtraShareMarks() {
        return (hasTemporaryPresident() ? "T" : "");
    }

    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // check if the running company is the cgr
        if (revenueAdapter.getCompany() != this) {
            return false;
        }

        // add the diesel train
        if (runsWithBorrowedTrain()) {
            if (GameOption.getValue( this,
                    "1856AlternateTrains").equalsIgnoreCase("yes")) {
                revenueAdapter.addTrainByString("8");
            } else {
                revenueAdapter.addTrainByString("D");
            }
            return true;
        }
        return false;
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {

        return null;
    }
}
