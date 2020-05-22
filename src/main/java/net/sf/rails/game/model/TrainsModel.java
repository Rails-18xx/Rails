package net.sf.rails.game.model;

import net.sf.rails.game.*;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.game.state.PortfolioSet;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;

import java.util.*;

/**
 * This class is named TrainsModel for backwards compatibility,
 * but it actually models TrainCards.
 * Lists of both traincards and trains are provided.
 */
public class TrainsModel extends RailsModel {

    public static final String ID = "TrainsModel";
    
    private final PortfolioSet<TrainCard> trainCards;
    
    private boolean abbrList = false;

    private TrainsModel(RailsOwner parent, String id) {
        super(parent, id);
        trainCards = PortfolioSet.create(parent, "trains", TrainCard.class);
        trainCards.addModel(this);
    }
    
    /** 
     * @return fully initialized TrainsModel
     */
    public static TrainsModel create(RailsOwner parent) {
        return new TrainsModel(parent, ID);
    }
    
    @Override
    public RailsOwner getParent() {
        return (RailsOwner)super.getParent();
    }
    
    public Portfolio<TrainCard> getPortfolio() {
        return trainCards;
    }
    
    public void setAbbrList(boolean abbrList) {
        this.abbrList = abbrList;
    }
    
    public ImmutableSet<TrainCard> getTrainCards() {
        return trainCards.items();
    }

    public void addTrain (Train train) {
        TrainCard card = train.getCard();
        trainCards.add(card);
        if (card.getType().isDual()) {
            card.setActualTrain(train);
        }

    }

    public List<Train> getTrains() {
        List<Train> trains = new ArrayList<>();
        for (TrainCard card : getTrainCards()) {
            if (card.getType().isDual() && card.getActualTrain() != null) {
                trains.add (card.getActualTrain());
            } else {
                trains.addAll(card.getTrains());
            }
        }
        return trains;
    }

    public Train getTrainOfType(TrainType trainType) {
        for (Train train:getTrains()) {
            if (train.getType() == trainType) return train;
        }
        return null;
    }

    public TrainCard getTrainCardOfType (TrainCardType cardType) {
        for (TrainCard card:getTrainCards()) {
            if (card.getType() == cardType) return card;
        }
        return null;
    }
    
    /**
     * Make a full list of trains, like "2 2 3 3", to show in any field
     * describing train possessions, except the IPO.
     */
    private String makeListOfTrains() {
        if (trainCards.isEmpty()) return "";

        StringBuilder b = new StringBuilder();
        for (Train train:getTrains()) {
            if (b.length() > 0) b.append(" ");
            if (train.isObsolete()) b.append("[");
            b.append(train.toText());
            if (train.isObsolete()) b.append("]");
        }

        return b.toString();
    }

    /**
     * Make an abbreviated list of trains, like "2(6) 3(5)" etc, to show in the
     * IPO.
     */
    public String makeListOfTrainCards() {
        if (trainCards.isEmpty()) return "";

        // create a bag with train types
        Multiset<TrainCardType> trainCardTypes = HashMultiset.create();
        for (TrainCard card:trainCards) {
            trainCardTypes.add(card.getType());
        }
        
        StringBuilder b = new StringBuilder();
        
        for (TrainCardType cardType:ImmutableSortedSet.copyOf(trainCardTypes.elementSet())) {
            if (b.length() > 0) b.append(" ");
            b.append(cardType.toText()).append("(");
            if (cardType.hasInfiniteQuantity()) {
                b.append("+");
            } else {
                b.append(trainCardTypes.count(cardType));
            }
            b.append(")");
        }

        return b.toString();
    }
    
    @Override
    public String toText() {
        if (!abbrList) {
            return makeListOfTrains();
        } else {
            return makeListOfTrainCards();
        }
    }
}
