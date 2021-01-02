package net.sf.rails.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrainManager extends RailsManager implements Configurable {
    // Static attributes
    protected final List<TrainType> trainTypes = new ArrayList<>();

    protected final Map<String, TrainType> mTrainTypes = new HashMap<>();

    protected final List<TrainCardType> trainCardTypes = new ArrayList<>(8);

    protected final Map<String, TrainCardType> trainCardTypeMap = new HashMap<>();

    protected final Map<String, TrainCard> trainCardMap = new HashMap<>();

    protected final Map<String, Train> trainMap = new HashMap<>();

    protected final Map<TrainCardType, List<TrainCard>> cardsPerType = new HashMap<>();

    protected final Map<TrainCardType, List<Train>> trainsPerCardType = new HashMap<>();

    protected final Map<TrainCard, List<Train>> trainsPerCard = new HashMap<>();

    private boolean removeTrain = false;

    protected String discardToString = "pool";
    protected BankPortfolio discardTo;

    // defines obsolescence
    public enum ObsoleteTrainForType {ALL, EXCEPT_TRIGGERING}

    protected ObsoleteTrainForType obsoleteTrainFor = ObsoleteTrainForType.EXCEPT_TRIGGERING; // default is ALL

    // Dynamic attributes
    protected final IntegerState newTypeIndex = IntegerState.create(this, "newTypeIndex", 0);

    protected final HashMapState<String, Integer> lastIndexPerType =
            HashMapState.create(this, "lastIndexPerType");

    protected final BooleanState phaseHasChanged = new BooleanState(this, "phaseHasChanged");

    protected final BooleanState trainAvailabilityChanged = new BooleanState(this, "trainAvailablityChanged");

    /**
     * Required for the sell-train-to-foreigners feature of some games
     */
    protected final BooleanState anyTrainBought = new BooleanState(this, "anyTrainBought");

    // Triggered phase changes
    protected final Map<TrainCardType, Map<Integer, Phase>> newPhases = new HashMap<>();

    // For initialisation only
    protected boolean trainPriceAtFaceValueIfDifferentPresidents = false;

    // For dual trains: does the chosen train type become undecided in the pool?
    protected boolean dualTrainBecomesUndecidedInPool = false;

    private static final Logger log = LoggerFactory.getLogger(TrainManager.class);

    /**
     * Used by Configure (via reflection) only
     */
    public TrainManager(RailsRoot parent, String id) {
        super(parent, id);
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        TrainType newTrainType;

        Tag defaultsTag = tag.getChild("Defaults");
        // We will use this tag later, to preconfigure TrainCardType and TrainType.

        List<Tag> trainTypeTags;

        // Choice train types (new style)
        List<Tag> cardTypeTags = tag.getChildren("TrainType");

        if (cardTypeTags != null) {
            int cardTypeIndex = 0;
            for (Tag cardTypeTag : cardTypeTags) {
                // FIXME: Creation of Type to be rewritten
                String cardTypeId = cardTypeTag.getAttributeAsString("name");
                TrainCardType cardType = TrainCardType.create(this, cardTypeId, cardTypeIndex++);
                if (defaultsTag != null) cardType.configureFromXML(defaultsTag);
                cardType.configureFromXML(cardTypeTag);
                trainCardTypes.add(cardType);
                trainCardTypeMap.put(cardType.getId(), cardType);

                // The potential train types
                trainTypeTags = cardTypeTag.getChildren("Train");
                if (trainTypeTags == null) {
                    // That's OK, all properties are in TrainType, to let's reuse that tag
                    trainTypeTags = Arrays.asList(cardTypeTag);
                }
                for (Tag trainTypeTag : trainTypeTags) {
                    newTrainType = new TrainType();
                    if (defaultsTag != null) newTrainType.configureFromXML(defaultsTag);
                    newTrainType.configureFromXML(cardTypeTag);
                    newTrainType.configureFromXML(trainTypeTag);
                    trainTypes.add(newTrainType);
                    mTrainTypes.put(newTrainType.getName(), newTrainType);
                    cardType.addPotentialTrainType(newTrainType);
                }
            }
        }


        // Special train buying rules
        Tag rulesTag = tag.getChild("TrainBuyingRules");
        if (rulesTag != null) {
            // A 1851 special
            trainPriceAtFaceValueIfDifferentPresidents = rulesTag.getChild("FaceValueIfDifferentPresidents") != null;
            // For dual trains (default: false) for 18VA and 18Scan(?)
            dualTrainBecomesUndecidedInPool = rulesTag.getChild("DualTrainBecomesUndecidedInPool") != null;
        }

        // Train obsolescence
        String obsoleteAttribute = tag.getAttributeAsString("ObsoleteTrainFor");
        if (Util.hasValue(obsoleteAttribute)) {
            try {
                obsoleteTrainFor = ObsoleteTrainForType.valueOf(obsoleteAttribute);
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }

        // Trains discard
        Tag discardTag = tag.getChild("DiscardTrain");
        if (discardTag != null) {
            discardToString = discardTag.getAttributeAsString("to");
        }

        // Are trains sold to foreigners?
        Tag removeTrainTag = tag.getChild("RemoveTrainBeforeSR");
        if (removeTrainTag != null) {
            // Trains "bought by foreigners" (1844, 1824)
            removeTrain = true; // completed in finishConfiguration()
            // to determine if permanent trains are also removed
            boolean removePermanent = removeTrainTag.getAttributeAsBoolean("permanent", false);
        }

    }

    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        Map<Integer, String> newPhaseNames;
        Phase phase;
        String phaseName;
        PhaseManager phaseManager = root.getPhaseManager();

        for (TrainCardType cardType : trainCardTypes) {
            cardType.finishConfiguration(root);
            for (TrainType trainType : cardType.getPotentialTrainTypes()) {
                trainType.finishConfiguration(root, cardType);
            }

            // Create the cards of this TrainCardType

            /* If the amount is infinite, only one card and train is created.
             * Each time this card is bought, another one is created.
             */
            for (int i = 0; i < (cardType.hasInfiniteQuantity() ? 1 : cardType.getQuantity()); i++) {
                createCardAndTrains (cardType);
                /*
                card = createTrainCard (cardType);
                addTrainCard(card);
                Bank.getUnavailable(this).getPortfolioModel().addTrainCard(card);

                List<TrainType> trainTypes = cardType.getPotentialTrainTypes();
                for (TrainType trainType : trainTypes) {
                    trainType.finishConfiguration(root, cardType);

                    // Create the trains of this TrainType
                    train = createTrain(trainType, card);

                    addTrain(card, train);
                    Bank.getUnavailable(this).getPortfolioModel().addTrain(train);
                }

                 */
            }

            // Register any phase changes
            newPhaseNames = cardType.getNewPhaseNames();
            if (newPhaseNames != null && !newPhaseNames.isEmpty()) {
                for ( Map.Entry<Integer, String> entry : newPhaseNames.entrySet()) {
                    phaseName = entry.getValue();
                    phase = phaseManager.getPhaseByName(phaseName);
                    if (phase == null) {
                        throw new ConfigurationException("New phase '" + phaseName + "' does not exist");
                    }
                    newPhases.computeIfAbsent(cardType, k -> new HashMap<>());
                    newPhases.get(cardType).put(entry.getKey(), phase);
                }
            }

        }

        // By default, set the first train type to "available".
        newTypeIndex.set(0);
        makeTrainsAvailable(trainCardTypes.get(newTypeIndex.value()));

        // Discard Trains To where?
        if ( "pool".equalsIgnoreCase(discardToString)) {
            discardTo = root.getBank().getPool();
        } else if ( "scrapheap".equalsIgnoreCase(discardToString)) {
            discardTo = root.getBank().getScrapHeap();
        } else {
            throw new ConfigurationException("Discard to only allow to pool or scrapheap");
        }

        // Trains "bought by foreigners" (1844, 1824)
        if (removeTrain) {
            root.getGameManager().setGameParameter(GameDef.Parm.REMOVE_TRAIN_BEFORE_SR, true);
        }

        // Train trading between different players at face value only (1851)
        root.getGameManager().setGameParameter(GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS,
                trainPriceAtFaceValueIfDifferentPresidents);
        root.getGameManager().setGameParameter(GameDef.Parm.DUAL_TRAIN_BECOMES_UNDECIDED_IN_POOL,
                dualTrainBecomesUndecidedInPool);
    }
    /*
    private TrainCard createTrainCard(TrainCardType trainCardType)
            throws ConfigurationException {
        int sequenceNumber = getNewUniqueId(trainCardType.getId());
        String id = trainCardType.getId() + "_" + sequenceNumber;
        TrainCard card = new TrainCard (this, id);
        card.setName(id);
        card.setType(trainCardType);

        return card;
    }

    private Train createTrain(TrainType trainType, TrainCard trainCard)
            throws ConfigurationException {
        int sequenceNumber = getNewUniqueId(trainType.getName());
        String id = trainType.getName() + "_" + sequenceNumber;
        Train train = Configure.create(trainCard.getType().getTrainClass(), this, id);
        train.setSortingId(sequenceNumber); // Hopefully redundant now
        trainCard.addTrain(train);
        train.setCard(trainCard);
        train.setType(trainType);

        return train;
    }

     */

    private TrainCard createCardAndTrains (TrainCardType cardType) {
        int sequenceNumber = getNewUniqueId(cardType.getId());
        // We can't use "_" here, because for non-dual trains
        // that would duplicate the train id.
        String id = cardType.getId() + "-" + sequenceNumber;
        TrainCard card = new TrainCard (this, id);
        card.setName (id);
        card.setType (cardType);
        addTrainCard(card);
        Bank.getUnavailable(this).getPortfolioModel().addTrainCard(card);

        List<TrainType> trainTypes = cardType.getPotentialTrainTypes();
        for (TrainType trainType : trainTypes) {

            // Create the trains of this TrainType
            id = trainType.getName() + "_" + sequenceNumber;
            Train train = new Train(this, id);
            card.addTrain(train);
            train.setCard(card);
            train.setType(trainType);
            train.setName(id);

            addTrain(card, train);
        }

        return card;

    }
    /**
     * Create train without throwing exceptions.
     * To be used <b>after</b> completing initialization,
     * i.e. in cloning infinitely available trains.
     */

    public TrainCard cloneTrain (TrainCardType cardType) {
        TrainCard card = createCardAndTrains (cardType);
        /*
        Train train = null;
        try {
            card = createTrainCard (cardType);
            addTrainCard(card);
            List<TrainType> trainTypes = cardType.getPotentialTrainTypes();
            for (TrainType trainType : trainTypes) {
                train = createTrain(trainType, card);
                addTrain(card, train);
            }
        } catch (ConfigurationException e) {
            log.warn("Unexpected exception", e);
        }
         */
         /* This return can only be used in games without dual train cards.
         * Only known usage is by 18GA.
         */
        return card;
    }

    public void addTrainCard(TrainCard card) {
        trainCardMap.put(card.getId(), card);

        TrainCardType type = card.getType();
        if (!cardsPerType.containsKey(type)) {
            cardsPerType.put(type, new ArrayList<>());
        }
        cardsPerType.get(type).add(card);
    }

    public void addTrain(TrainCard card, Train train) {
        trainMap.put(train.getId(), train);

        TrainCardType type = train.getCardType();
        if (!trainsPerCardType.containsKey(type)) {
            trainsPerCardType.put(type, new ArrayList<>());
        }
        trainsPerCardType.get(type).add(train);
    }

    public Train getTrainByUniqueId(String id) {
        return trainMap.get(id);
    }

    /* No longer used
    public TrainCard getTrainCardByUniqueID (String id) { return trainCardMap.get(id); }
     */

    public int getNewUniqueId(String typeName) {
        int newUniqueId = lastIndexPerType.containsKey(typeName) ? lastIndexPerType.get(typeName) + 1 : 0;
        lastIndexPerType.put(typeName, newUniqueId);
        return newUniqueId;
    }


    /**
     * This method handles any consequences of new train buying (from the IPO),
     * such as rusting and phase changes. It must be called <b>after</b> the
     * train has been transferred.
     */
    public void checkTrainAvailability(Train train, Owner from) {

        phaseHasChanged.set(false);
        if (from != Bank.getIpo(this)) return;

        TrainCardType boughtType, nextType;
        boughtType = train.getCardType();
        List<TrainCardType> alsoReleasedTypes;
        if (boughtType == (trainCardTypes.get(newTypeIndex.value()))
                && Bank.getIpo(this).getPortfolioModel().getTrainCardOfType(boughtType) == null) {
            // Last train bought, make a new type available.
            newTypeIndex.add(1);
            if (newTypeIndex.value() < trainTypes.size()) {
                nextType = (trainCardTypes.get(newTypeIndex.value()));
                if (nextType != null) {
                    if (!nextType.isAvailable()) {
                        makeTrainsAvailable(nextType);
                        trainAvailabilityChanged.set(true);
                        ReportBuffer.add(this, LocalText.getText(
                               "NewTrainAvailable", boughtType.toText(), nextType.toText()));
                    }
                    alsoReleasedTypes = nextType.getAlsoReleased();
                    if (alsoReleasedTypes != null) {
                        for (TrainCardType alsoReleasedType : alsoReleasedTypes) {
                            if (!alsoReleasedType.isAvailable()) {
                                makeTrainsAvailable(alsoReleasedType);
                                trainAvailabilityChanged.set(true);
                                ReportBuffer.add(this, LocalText.getText(
                                        "NewTrainAlsoAvailable", alsoReleasedType.toText()));
                            }
                        }
                    }
                }
            }
        }

        int trainIndex = boughtType.getNumberBoughtFromIPO();
        if (trainIndex == 1) {
            // First train of a new type bought
            ReportBuffer.add(this, LocalText.getText("FirstTrainBought",
                    boughtType.toText()));
        }

        // New style phase changes, can be triggered by any bought train.
        Phase newPhase;
        if (newPhases.get(boughtType) != null
                && (newPhase = newPhases.get(boughtType).get(trainIndex)) != null) {
            getRoot().getPhaseManager().setPhase(newPhase, train.getOwner());
            phaseHasChanged.set(true);
        }
    }

    protected void makeTrainsAvailable(TrainCardType cardType) {

        cardType.setAvailable();

        BankPortfolio to = ("Pool".equalsIgnoreCase(cardType.getInitialPortfolio()) ? Bank.getPool(this) : Bank.getIpo(this));

        /*
        for (Train train : trainsPerCardType.get(cardType)) {
            to.getPortfolioModel().addTrain(train);
        } */

        for (TrainCard card : cardsPerType.get(cardType)) {
            to.getPortfolioModel().addTrainCard(card);
        }
    }

    // checks train obsolete condition
    private boolean isTrainObsolete(Train train, Owner lastBuyingCompany) {
        // check fist if train can obsolete at all
        if (!train.getCardType().isObsoleting()) return false;
        // and if it is in the pool (always rust)
        if (train.getOwner() == Bank.getPool(this)) return false;

        // then check if obsolete type
        if (obsoleteTrainFor == ObsoleteTrainForType.ALL) {
            return true;
        } else { // otherwise it is AllExceptTriggering
            Owner owner = train.getOwner();
            return (owner instanceof PublicCompany && owner != lastBuyingCompany);
        }
    }

    protected void rustTrainType(TrainCardType type, Owner lastBuyingCompany) {
        type.setRusted();
        for (Train train : trainsPerCardType.get(type)) {
            Owner owner = train.getOwner();
            // check condition for train rusting
            if (isTrainObsolete(train, lastBuyingCompany)) {
                log.debug("Train {} (owned by {}) obsoleted", train.getId(), owner.getId());
                train.setObsolete();
                // TODO: is this still required?
                // train.getHolder().update();
            } else {
                log.debug("Train {} (owned by {}) rusted", train.getId(), owner.getId());
                train.setRusted();
            }
        }
        // report about event
        if (type.isObsoleting()) {
            ReportBuffer.add(this, LocalText.getText("TrainsObsolete." + obsoleteTrainFor, type.getId()));
        } else {
            ReportBuffer.add(this, LocalText.getText("TrainsRusted", type.getId()));
        }
    }

    public Set<Train> getAvailableNewTrains() {

        Set<Train> availableTrains = new TreeSet<>();
        Train train;

        for (TrainCardType cardType : trainCardTypes) {
            if (cardType.isAvailable()) {
                for (TrainType trainType : cardType.getPotentialTrainTypes()) {
                    train = Bank.getIpo(this).getPortfolioModel().getTrainOfType(trainType);
                    if (train != null) {
                        availableTrains.add(train);
                    }
                }
            }
        }
        return availableTrains;
    }

    public String getTrainCostOverview() {
        StringBuilder b = new StringBuilder();
        for (TrainType trainType : trainTypes) {
            if (trainType.getCost() > 0) {
                if (b.length() > 1) b.append(" ");
                b.append(trainType.getName()).append(":").append(Bank.format(this, trainType.getCost()));
                if (trainType.getExchangeCost() > 0) {
                    b.append("(").append(Bank.format(this, trainType.getExchangeCost())).append(")");
                }
                /* Not needed?
            } else {
                for (TrainType type : trainType.getPotentialTrainTypes()) {
                    if (b.length() > 1) b.append(" ");
                    b.append(type.getName()).append(":").append(Bank.format(this, type.getCost()));
                }
                 */
            }
        }
        return b.toString();
    }

    public TrainType getTrainTypeByName(String name) {
        return mTrainTypes.get(name);
    }

    public List<TrainType> getTrainTypes() {
        return trainTypes;
    }

    public List<TrainCardType> getTrainCardTypes() {
        return trainCardTypes;
    }

    public TrainCardType getCardTypeByName(String name) {
        return trainCardTypeMap.get(name);
    }

    public boolean hasAvailabilityChanged() {
        return trainAvailabilityChanged.value();
    }

    public void resetAvailabilityChanged() {
        trainAvailabilityChanged.set(false);
    }

    public boolean hasPhaseChanged() {
        return phaseHasChanged.value();
    }

    public boolean isAnyTrainBought() {
        return anyTrainBought.value();
    }

    public void setAnyTrainBought(boolean newValue) {
        if (isAnyTrainBought() != newValue) {
            anyTrainBought.set(newValue);
        }
    }

    public BankPortfolio discardTo() {
        return discardTo;
    }

    public boolean doesDualTrainBecomesUndecidedInPool() {
        return dualTrainBecomesUndecidedInPool;
    }

    public List<TrainType> parseTrainTypes(String trainTypeName) {
        List<TrainType> trainTypes = new ArrayList<>();
        TrainType trainType;
        for (String trainTypeSingle : trainTypeName.split(",")) {
            trainType = getTrainTypeByName(trainTypeSingle);
            if (trainType != null) {
                trainTypes.add(trainType);
            }
        }

        return trainTypes;
    }

}
