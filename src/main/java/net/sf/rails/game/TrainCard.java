package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.state.Creatable;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Root;

import java.util.*;

public class TrainCard extends RailsOwnableItem<TrainCard> implements Creatable {

    private TrainCardType trainCardType;

    private String name;

    /**
     * Some specific trains cannot be traded between companies
     */
    protected boolean tradeable = true;

    private List<Train> trains = new ArrayList<>(2);

    protected final GenericState<Train> actualTrain = new GenericState<>(this, "train");

    public TrainCard(RailsItem parent, String id) {
        super(parent, id, TrainCard.class);
    }

    public TrainCardType getType() {
        return trainCardType;
    }

    public void setType(TrainCardType trainCardType) {
        this.trainCardType = trainCardType;
    }

    public String getName() {
        return name;
    }

    public boolean isTradeable() {
        return tradeable;
    }

    public void setTradeable(boolean tradeable) {
        this.tradeable = tradeable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Train> getTrains() {
        return trains;
    }

    public void addTrain(Train train) {
        this.trains.add(train);
    }

    public Train getActualTrain() {
        return actualTrain.value();
    }

    public void setActualTrain(Train actualTrain) {
        this.actualTrain.set(actualTrain);
    }

    public Train getTrain() {
        if (trains.size() == 1)
            return trains.get(0);
        else
            return actualTrain.value();
    }

    public void discard () {
        BankPortfolio discardTo;
        if (actualTrain.value().isObsolete()) {
            /* TODO: isObsolete belongs in this class */
            discardTo = Bank.getScrapHeap(this);
        } else {
            discardTo = getRoot().getTrainManager().discardTo();
        }
        if(trainCardType.isDual() && discardTo == Bank.getPool(this)) {
            if (getRoot().getTrainManager().doesDualTrainBecomesUndecidedInPool()) {
                setActualTrain(null);
            }
        }
        String discardText = LocalText.getText("CompanyDiscardsTrain", getOwner().getId(),
                actualTrain.value().getType(), discardTo.getId());
        ReportBuffer.add(this, discardText);
        this.moveTo(discardTo);
    }

    /* obsolete
    public void discard(BankPortfolio discardTo, boolean dualTrainBecomesUndecidedInPool) {
        this.moveTo(discardTo);
        if(trainCardType.isDual() && discardTo == Bank.getPool(this)) {
            if (dualTrainBecomesUndecidedInPool) {
                setActualTrain(null);
            }
        }
        String discardText = LocalText.getText("CompanyDiscardsTrain", getOwner().getId(), this.toText(), discardTo.getId());
        ReportBuffer.add(this, discardText);
    }*/

}
