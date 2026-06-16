package net.sf.rails.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.model.PortfolioModel;
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

    /**
     * Public accessor for the index of the currently active train type.
     * Required by GameStatus to calculate Current vs Future train costs.
     */
    public IntegerState getNewTypeIndex() {
        return newTypeIndex;
    }

    
    protected final Map<String, TrainType> mTrainTypes = new HashMap<>();

    protected final List<TrainCardType> trainCardTypes = new ArrayList<>(8);

    protected final Map<String, TrainCardType> trainCardTypeMap = new HashMap<>();

    protected final Map<String, TrainCard> trainCardMap = new HashMap<>();

    protected final Map<String, Train> trainMap = new HashMap<>();

    protected final Map<TrainCardType, List<TrainCard>> cardsPerType = new HashMap<>();

    protected final Map<TrainCardType, List<Train>> trainsPerCardType = new HashMap<>();

    protected final Map<TrainCard, List<Train>> trainsPerCard = new HashMap<>();

    protected final Map<String, Tag> defaultsTagMap = new HashMap<>();

    private boolean removeTrain = false;
    private boolean removePermanent;

    protected String discardToString = "pool";
    protected BankPortfolio discardTo;

    // defines obsolescence
    public enum ObsoleteTrainForType {
        ALL, EXCEPT_TRIGGERING
    }

    protected ObsoleteTrainForType obsoleteTrainFor = ObsoleteTrainForType.EXCEPT_TRIGGERING; // default is ALL

    // Dynamic attributes
    protected final IntegerState newTypeIndex = IntegerState.create(this, "newTypeIndex", 0);

    protected final HashMapState<String, Integer> lastIndexPerType = HashMapState.create(this, "lastIndexPerType");

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

        List<Tag> defaultsTags = tag.getChildren("Defaults");
        // New 12/2022: defaults per train category (for 18VA)
        if (defaultsTags != null) {
            String category;
            for (Tag defaultsTag : defaultsTags) {
                category = defaultsTag.getAttributeAsString("category",
                        TrainType.defaultCategory);
                defaultsTagMap.put(category, defaultsTag);
            }
        }
        // We will use these tags later, to preconfigure TrainCardType and TrainType.

        List<Tag> trainTypeTags;

        // Choice train types (new style)
        List<Tag> cardTypeTags = tag.getChildren("TrainType");

        if (cardTypeTags != null) {
            int cardTypeIndex = 0;
            for (Tag cardTypeTag : cardTypeTags) {
                // FIXME: Creation of Type to be rewritten
                String cardTypeId = cardTypeTag.getAttributeAsString("name");
                TrainCardType cardType = TrainCardType.create(this, cardTypeId, cardTypeIndex++);
                if (defaultsTags != null) {
                    Tag defaultsTag = getDefaultsPerCategory(null);
                    if (defaultsTag != null)
                        cardType.configureFromXML(defaultsTag);
                }
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
                    if (defaultsTags != null) {
                        Tag defaultsTag = getDefaultsPerCategory(trainTypeTag);
                        if (defaultsTag != null)
                            newTrainType.configureFromXML(defaultsTag);
                    }
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
            removePermanent = removeTrainTag.getAttributeAsBoolean("permanent", false);
        }
    }

    private Tag getDefaultsPerCategory(Tag trainTypeTag) throws ConfigurationException {
        String category = null;
        if (trainTypeTag != null) {
            category = trainTypeTag.getAttributeAsString("category", TrainType.defaultCategory);
        }
        if (category == null) {
            category = TrainType.defaultCategory;
        }
        return defaultsTagMap.get(category);
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

            /*
             * If the amount is infinite, only one card and train is created.
             * Each time this card is bought, another one is created.
             */
            for (int i = 0; i < (cardType.hasInfiniteQuantity() ? 1 : cardType.getQuantity()); i++) {
                createCardAndTrains(cardType);
                /*
                 * card = createTrainCard (cardType);
                 * addTrainCard(card);
                 * Bank.getUnavailable(this).getPortfolioModel().addTrainCard(card);
                 * 
                 * List<TrainType> trainTypes = cardType.getPotentialTrainTypes();
                 * for (TrainType trainType : trainTypes) {
                 * trainType.finishConfiguration(root, cardType);
                 * 
                 * // Create the trains of this TrainType
                 * train = createTrain(trainType, card);
                 * 
                 * addTrain(card, train);
                 * Bank.getUnavailable(this).getPortfolioModel().addTrain(train);
                 * }
                 * 
                 */
            }

            // Register any phase changes
            newPhaseNames = cardType.getNewPhaseNames();
            if (newPhaseNames != null && !newPhaseNames.isEmpty()) {
                for (Map.Entry<Integer, String> entry : newPhaseNames.entrySet()) {
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
        releaseInitialTrainTypes(trainCardTypes.get(newTypeIndex.value()));

        // Discard Trains To where?
        if ("pool".equalsIgnoreCase(discardToString)) {
            discardTo = root.getBank().getPool();
        } else if ("scrapheap".equalsIgnoreCase(discardToString)) {
            discardTo = root.getBank().getScrapHeap();
        } else {
            throw new ConfigurationException("Discard to only allow to pool or scrapheap");
        }

        // Trains "bought by foreigners" (1844, 1824, 18Chesapeake)
        if (removeTrain) {
            root.getGameManager().setGameParameter(GameDef.Parm.REMOVE_TRAIN_BEFORE_SR, true);
            if (removePermanent) {
                root.getGameManager().setGameParameter(GameDef.Parm.REMOVE_PERMANENT, true);
            }
        }

        // Train trading between different players at face value only (1851)
        root.getGameManager().setGameParameter(GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS,
                trainPriceAtFaceValueIfDifferentPresidents);
        root.getGameManager().setGameParameter(GameDef.Parm.DUAL_TRAIN_BECOMES_UNDECIDED_IN_POOL,
                dualTrainBecomesUndecidedInPool);
    }
    /*
     * private TrainCard createTrainCard(TrainCardType trainCardType)
     * throws ConfigurationException {
     * int sequenceNumber = getNewUniqueId(trainCardType.getId());
     * String id = trainCardType.getId() + "_" + sequenceNumber;
     * TrainCard card = new TrainCard (this, id);
     * card.setName(id);
     * card.setType(trainCardType);
     * 
     * return card;
     * }
     * 
     * private Train createTrain(TrainType trainType, TrainCard trainCard)
     * throws ConfigurationException {
     * int sequenceNumber = getNewUniqueId(trainType.getName());
     * String id = trainType.getName() + "_" + sequenceNumber;
     * Train train = Configure.create(trainCard.getType().getTrainClass(), this,
     * id);
     * train.setSortingId(sequenceNumber); // Hopefully redundant now
     * trainCard.addTrain(train);
     * train.setCard(trainCard);
     * train.setType(trainType);
     * 
     * return train;
     * }
     * 
     */

    private TrainCard createCardAndTrains(TrainCardType cardType) {
        int sequenceNumber = getNewUniqueId(cardType.getId());
        // We can't use "_" here, because for non-dual trains
        // that would duplicate the train id.
        String id = cardType.getId() + "-" + sequenceNumber;
        TrainCard card = new TrainCard(this, id);
        card.setName(id);
        card.setType(cardType);
        addTrainCard(card);
        Bank.getUnavailable(this).getPortfolioModel().addTrainCard(card);

        List<TrainType> trainTypes = cardType.getPotentialTrainTypes();
        for (TrainType trainType : trainTypes) {

            // Create the trains of this TrainType
            id = trainType.getName() + "_" + sequenceNumber;
            Train train = new Train(this, id);
            if (trainTypes.size() == 1)
                card.setActualTrain(train);
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

    public TrainCard cloneTrain(TrainCardType cardType) {
        TrainCard card = createCardAndTrains(cardType);
        /*
         * Train train = null;
         * try {
         * card = createTrainCard (cardType);
         * addTrainCard(card);
         * List<TrainType> trainTypes = cardType.getPotentialTrainTypes();
         * for (TrainType trainType : trainTypes) {
         * train = createTrain(trainType, card);
         * addTrain(card, train);
         * }
         * } catch (ConfigurationException e) {
         * log.warn("Unexpected exception", e);
         * }
         */
        /*
         * This return can only be used in games without dual train cards.
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

    /*
     * No longer used
     * public TrainCard getTrainCardByUniqueID (String id) { return
     * trainCardMap.get(id); }
     */

    public int getNewUniqueId(String typeName) {
        int newUniqueId = lastIndexPerType.containsKey(typeName) ? lastIndexPerType.get(typeName) + 1 : 0;
        lastIndexPerType.put(typeName, newUniqueId);
        return newUniqueId;
    }

    // In TrainManager.java
    public void checkTrainAvailability(Train train, Owner from) {
        // +++ Use ERROR level for high visibility +++
        // log.error(">>> checkTrainAvailability START: Train={}, Type={}, From={}",
        // (train != null ? train.getId() : "null"),
        // (train != null ? train.getCardType().getId() : "null"),
        // (from != null ? from.getId() : "null"));

        phaseHasChanged.set(false); // Reset flag at the start

        // --- Check 1: Must be bought from IPO ---
        if (from != Bank.getIpo(this)) {
            // log.error(" Condition FAIL: Train not bought from IPO (from {}). Phase check
            // skipped.", (from != null ? from.getId() : "null"));
            // log.error("<<< checkTrainAvailability END (Skipped)");
            return;
        }
        // log.error(" Condition PASS: Train bought from IPO.");

        TrainCardType boughtType = train.getCardType();
        TrainCardType currentTypeAtIndex = trainCardTypes.get(newTypeIndex.value());

        // --- Check 2: Check if LAST train of CURRENTLY available type was bought ---
        // log.error(" Checking if last of current type ({}) bought...",
        // currentTypeAtIndex.getId());
        // log.error(" Bought type: {}, Current type at index {}: {}",
        // boughtType.getId(), newTypeIndex.value(), currentTypeAtIndex.getId());
        //  Use helper method for count ***
        int cardsOfTypeInIPO = countCardsOfTypeInPortfolio(Bank.getIpo(this).getPortfolioModel(), boughtType);
        // log.error(" Cards of bought type ({}) remaining in IPO: {}",
        // boughtType.getId(), cardsOfTypeInIPO);

        if (boughtType == currentTypeAtIndex && cardsOfTypeInIPO == 0) {
            // log.error(" Condition MET: Last train of type {} bought. Advancing
            // newTypeIndex from {} to {}.",
            // boughtType.getId(), newTypeIndex.value(), newTypeIndex.value() + 1);

            newTypeIndex.add(1);
            if (newTypeIndex.value() < trainCardTypes.size()) {
                TrainCardType nextType = trainCardTypes.get(newTypeIndex.value());
                if (nextType != null) {
                    // log.error(" Releasing next train type: {}", nextType.getId());
                    releaseTrainTypes(boughtType, nextType, true); // This sets trainAvailabilityChanged
                } else {
                    // log.error(" ERROR - Next train type at index {} is null!",
                    // newTypeIndex.value());
                }
            } else {
                // log.error(" Reached end of train types (index {} >= size {}).",
                // newTypeIndex.value(), trainCardTypes.size());
            }
        } else {
            // log.error(" Condition NOT MET: Not the last train of the current type ({}) OR
            // types don't match.", currentTypeAtIndex.getId());
        }

        // In TrainManager.java -> checkTrainAvailability method

        // --- Check 3: Check for XML-defined Phase Changes ---
        // Get the count *including* the train just purchased.
        // addToBoughtFromIPO() was already called in OperatingRound.buyTrain *before*
        // this method if bought from IPO.
        int countIncludingThisOne = boughtType.getNumberBoughtFromIPO(); // <<< USE THIS COUNT
        // log.error(" Checking for phase change trigger: BoughtType={}, Total count now
        // bought from IPO={}", boughtType.getId(), countIncludingThisOne);

        Phase newPhase = null;
        if (newPhases.containsKey(boughtType)) {
            // log.error(" Found phase change rules for type {}. Rules: {}",
            // boughtType.getId(), newPhases.get(boughtType));
            Map<Integer, Phase> phaseTriggers = newPhases.get(boughtType);
            // *** FIX: Check using count *including* this purchase ***
            if (phaseTriggers.containsKey(countIncludingThisOne)) { // <<< CHECK THIS COUNT AGAINST THE RULE KEY
                newPhase = phaseTriggers.get(countIncludingThisOne);
                // log.error(" MATCH FOUND! Train count {} triggers phase: {}",
                // countIncludingThisOne, (newPhase != null ? newPhase.getId() : "null"));
            } else {
                // log.error(" No phase trigger found matching train count {}.",
                // countIncludingThisOne);
            }
        } else {
            // log.error(" No phase change rules defined in XML for type {}.",
            // boughtType.getId());
        }

        // --- Phase setting logic follows ---
        if (newPhase != null) {
            Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
            // *** FIX: Use getId() for Phase ***
            if (currentPhase != newPhase) {
                // log.error(" Setting new phase: {} (was {})", (newPhase != null ?
                // newPhase.getId() : "null"), (currentPhase != null ? currentPhase.getId() :
                // "null"));
                getRoot().getPhaseManager().setPhase(newPhase, train.getOwner()); // Set the new phase
                phaseHasChanged.set(true); // Set the flag
                // log.error(" phaseHasChanged flag SET to true.");
            } else {
                // *** FIX: Use getId() for Phase ***
                // log.error(" Triggered phase ({}) is already the current phase. No change.",
                // (newPhase != null ? newPhase.getId() : "null"));
            }
        } else {
            // log.error(" No new phase triggered by this purchase.");
        }



        // --- Check 4: Check for Rusting (Independent of phase change trigger) ---
        List<TrainCardType> rustedTrainTypes = boughtType.getRustedTrainTypes(); // Types this train rusts
        if (rustedTrainTypes != null && !rustedTrainTypes.isEmpty()) {
            // log.error(" Train type {} rusts other types: {}", boughtType.getId(),
            // rustedTrainTypes);
            for (TrainCardType typeToRust : rustedTrainTypes) {
                // log.error(" Rusting type: {}", typeToRust.getId());
                // Assuming rustTrainType handles reporting and state changes
                rustTrainType(typeToRust, train.getOwner()); // Pass owner for EXCEPT_TRIGGERING rule
            }
        } else {
            // log.error(" Train type {} does not rust other types.", boughtType.getId());
        }

        // log.error("<<< checkTrainAvailability END: phaseHasChanged={}",
        // phaseHasChanged.value());
    }

    // Helper method to count cards (Make sure this exists in TrainManager class)
    private int countCardsOfTypeInPortfolio(PortfolioModel portfolio, TrainCardType cardType) {
        int count = 0;
        if (portfolio != null && cardType != null) {
            for (TrainCard card : portfolio.getTrainsModel().getPortfolio().items()) {
                if (card.getType().equals(cardType)) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void releaseInitialTrainTypes(TrainCardType cardType) {
        releaseTrainTypes(null, cardType, false);
    }

    protected void releaseTrainTypes(TrainCardType boughtType, TrainCardType cardType, boolean reportIt) {
        List<TrainCardType> alsoReleasedTypes;
        if (!cardType.isAvailable()) {
            makeTrainsAvailable(cardType);
            trainAvailabilityChanged.set(true);
            if (reportIt) { // No reporting of the initial release (don't break test reports)
                ReportBuffer.add(this, LocalText.getText(
                        "NewTrainAvailable", boughtType.toText(), cardType.toText()));
            }
        }
        alsoReleasedTypes = cardType.getAlsoReleased();
        if (alsoReleasedTypes != null) {
            for (TrainCardType alsoReleasedType : alsoReleasedTypes) {
                if (!alsoReleasedType.isAvailable()) {
                    makeTrainsAvailable(alsoReleasedType);
                    trainAvailabilityChanged.set(true);
                    if (reportIt) {
                        ReportBuffer.add(this, LocalText.getText(
                                "NewTrainAlsoAvailable", alsoReleasedType.toText()));
                    }
                }
            }
        }

    }

    protected void makeTrainsAvailable(TrainCardType cardType) {

        cardType.setAvailable();
        // error(">>> makeTrainsAvailable: Setting card type {} AVAILABLE.",
        // cardType.getId());

        BankPortfolio to = ("Pool".equalsIgnoreCase(cardType.getInitialPortfolio()) ? Bank.getPool(this)
                : Bank.getIpo(this));

        /*
         * for (Train train : trainsPerCardType.get(cardType)) {
         * to.getPortfolioModel().addTrain(train);
         * }
         */

        for (TrainCard card : cardsPerType.get(cardType)) {
            if (!(card.getOwner() instanceof PublicCompany)) { // to circumvent a bug in 1837
                to.getPortfolioModel().addTrainCard(card);
            }
        }
    }

    // checks train obsolete condition
    private boolean isTrainObsolete(Train train, Owner lastBuyingCompany) {
        // check fist if train can obsolete at all
        if (!train.getCardType().isObsoleting())
            return false;
        // and if it is in the pool (always rust)
        if (train.getOwner() == Bank.getPool(this))
            return false;

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
                // log.debug("Train {} (owned by {}) obsoleted", train.getId(), owner.getId());
                train.setObsolete();
                // TODO: is this still required?
                // train.getHolder().update();
            } else {
                // log.debug("Train {} (owned by {}) rusted", train.getId(), owner.getId());
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
        // +++ ADD LOGGING +++
        // log.error(">>> getAvailableNewTrains: Checking available types...");
        // +++ END LOGGING +++
        Set<Train> availableTrains = new TreeSet<>();
        Train train;

        for (TrainCardType cardType : trainCardTypes) {
            // +++ ADD LOGGING +++
            boolean isAvailable = cardType.isAvailable();
            // log.error(" Checking CardType: {}, IsAvailable: {}", cardType.getId(),
            // isAvailable);
            // +++ END LOGGING +++
            if (isAvailable) { // Use the variable for clarity
                PortfolioModel ipoPortfolio = Bank.getIpo(this).getPortfolioModel(); // Get portfolio // <<< CHANGE TYPE
                                                                                     // HERE

                int countInIPO = 0;
                // Access TrainCards through ipoPortfolio -> getTrainsModel() -> getPortfolio()
                // -> items()
                for (TrainCard card : ipoPortfolio.getTrainsModel().getPortfolio().items()) { // <<< CORRECT ACCESS
                    if (card.getType().equals(cardType)) {
                        countInIPO++;
                    }
                }

                // log.error(" Type {} is available. Count in IPO: {}", cardType.getId(),
                // countInIPO);
                // +++ END LOGGING +++
                // Iterate potential TrainTypes within the CardType
                for (TrainType trainType : cardType.getPotentialTrainTypes()) {
                    // +++ ADD LOGGING +++
                    // log.error(" Checking TrainType: {}", trainType.getName());
                    // +++ END LOGGING +++
                    // Check if *this specific TrainType* exists in IPO
                    train = ipoPortfolio.getTrainOfType(trainType);
                    if (train != null) {
                        // +++ ADD LOGGING +++
                        // log.error(" FOUND available train in IPO: {}", train.getId());
                        // +++ END LOGGING +++
                        availableTrains.add(train);
                    } else {
                        // +++ ADD LOGGING +++
                        // log.error(" Train of type {} NOT found in IPO.", trainType.getName());
                        // +++ END LOGGING +++
                    }
                }
            }
        }
        // +++ ADD LOGGING +++
        // log.error("<<< getAvailableNewTrains: Found {} available trains total.",
        // availableTrains.size());
        // +++ END LOGGING +++
        return availableTrains;
    }

    public String getTrainCostOverview() {
        StringBuilder b = new StringBuilder();
        for (TrainType trainType : trainTypes) {
            if (trainType.getCost() > 0) {
                if (b.length() > 1)
                    b.append(" ");
                b.append(trainType.getName()).append(":").append(Bank.format(this, trainType.getCost()));
                if (trainType.getExchangeCost() > 0) {
                    b.append("(").append(Bank.format(this, trainType.getExchangeCost())).append(")");
                }
                /*
                 * Not needed?
                 * } else {
                 * for (TrainType type : trainType.getPotentialTrainTypes()) {
                 * if (b.length() > 1) b.append(" ");
                 * b.append(type.getName()).append(":").append(Bank.format(this,
                 * type.getCost()));
                 * }
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

    // ++ START AI STATE RESTORATION HELPER ++
    /**
     * Public accessor for the list of train cards of a specific type.
     * Needed for state re-hydration.
     */
    public List<TrainCard> getCardsForType(TrainCardType type) {
        return cardsPerType.get(type);
    }

    /**
     * Public accessor for the master list of all train objects.
     * Needed for state serialization.
     */
    public java.util.Collection<Train> getAllTrains() {
        return trainMap.values();
    }

    /**
 * Public accessor for the Bank's train pool (used as a target).
 */
public BankPortfolio getTrainPool() {
    return discardTo; 
}

/**
 * Checks if a train is considered removed/trashed from the game.
 * Relies on the TrainCard's owner being the ScrapHeap.
 */
public boolean isTrainTrashed(Train train) {
    // A train is "trashed" if its card is in the ScrapHeap.
    return train.getCard().getOwner() == getRoot().getBank().getScrapHeap();
}

/**
 * Transfers a train card from its current owner to a new owner.
 * @param train The train object whose card is to be moved.
 * @param newOwner The new owner (Company, Player, or BankPortfolio).
 */
public void transferTrain(Train train, Owner newOwner) {
    // The actual transfer happens at the card level.
    train.getCard().moveTo(newOwner);
}

/**
 * Removes a train from the game state entirely (i.e., to the scrap heap/trash).
 * @param train The train to trash.
 */
public void trashTrain(Train train) {
    // Calling setRusted on Train moves the associated TrainCard to the ScrapHeap.
    train.setRusted(); 
}

/**
 * Public accessor to get a train by its unique name/ID.
 */
public Train getTrainByUniqueId(String id) {
    return trainMap.get(id);
}

/**
 * Ruft die Liste der Züge ab, die einem bestimmten Owner gehören.
 * Dies ist kritisch für den 3-Schritte-Korrekturprozess (Schritt 2).
 * Behebt den Fehler: Portfolio enthält TrainCard, nicht Train.
 * @param owner Der Besitzer (Unternehmen, Spieler oder BankPortfolio).
 * @return Eine Liste von Train-Objekten.
 */
public List<Train> getTrains(Owner owner) {
    net.sf.rails.game.model.PortfolioModel portfolioModel = null;

    // 1. Safely retrieve the PortfolioModel, casting to the concrete class.
    if (owner instanceof net.sf.rails.game.PublicCompany pc) {
        portfolioModel = pc.getPortfolioModel();
    } else if (owner instanceof net.sf.rails.game.Player p) {
        portfolioModel = p.getPortfolioModel();
    } else if (owner instanceof net.sf.rails.game.financial.BankPortfolio bp) {
        portfolioModel = bp.getPortfolioModel(); 
    }
    
    if (portfolioModel == null) {
        return new ArrayList<>();
    }
    
    // 2. Access items, filter for TrainCard, and map to the actual Train object inside the card.
    // Die Liste der "items" enthält TrainCards. Wir extrahieren den Train aus der Card.
    return portfolioModel.getTrainsModel().getPortfolio().items().stream()
        .filter(item -> item instanceof net.sf.rails.game.TrainCard)
        .map(item -> ((net.sf.rails.game.TrainCard) item).getActualTrain()) 
        .filter(t -> t != null) // Sicherstellen, dass die TrainCard einen Train hält
        .collect(Collectors.toList());
}
}