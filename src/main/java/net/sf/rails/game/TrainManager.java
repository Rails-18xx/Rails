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
    protected final List<TrainType> lTrainTypes = new ArrayList<TrainType>();

    protected final Map<String, TrainType> mTrainTypes
            = new HashMap<String, TrainType>();

    protected final List<TrainCertificateType> trainCertTypes 
            = new ArrayList<TrainCertificateType>();

    protected final Map<String, TrainCertificateType> trainCertTypeMap
            = new HashMap<String, TrainCertificateType>();

    protected final Map<String, Train> trainMap
            = new HashMap<String, Train>();
    
    protected final Map<TrainCertificateType, List<Train>> trainsPerCertType 
            = new HashMap<TrainCertificateType, List<Train>>();
    
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

    protected final BooleanState phaseHasChanged = BooleanState.create(this, "phaseHasChanged");

    protected final BooleanState trainAvailabilityChanged = BooleanState.create(this, "trainAvailablityChanged");

    /** Required for the sell-train-to-foreigners feature of some games */
    protected final BooleanState anyTrainBought = BooleanState.create(this, "anyTrainBought");
    
    // Triggered phase changes
    protected final Map<TrainCertificateType, Map<Integer, Phase>> newPhases
            = new HashMap<TrainCertificateType, Map<Integer, Phase>>();

    // For initialisation only
    boolean trainPriceAtFaceValueIfDifferentPresidents = false;

    protected static Logger log =
        LoggerFactory.getLogger(TrainManager.class);
    
    /**
     * Used by Configure (via reflection) only
     */
    public TrainManager(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        TrainType newType;

        Tag defaultsTag = tag.getChild("Defaults");
        // We will use this tag later, to preconfigure TrainCertType and TrainType.

        List<Tag> typeTags;

        // Choice train types (new style)
        List<Tag> trainTypeTags = tag.getChildren("TrainType");

        if (trainTypeTags != null) {
            int trainTypeIndex = 0;
            for (Tag trainTypeTag : trainTypeTags) {
                // FIXME: Creation of Type to be rewritten
                String trainTypeId = trainTypeTag.getAttributeAsString("name");
                TrainCertificateType certType = TrainCertificateType.create(this, trainTypeId, trainTypeIndex++);
                if (defaultsTag != null) certType.configureFromXML(defaultsTag);
                certType.configureFromXML(trainTypeTag);
                trainCertTypes.add(certType);
                trainCertTypeMap.put(certType.getId(), certType);
                
                // The potential train types
                typeTags = trainTypeTag.getChildren("Train");
                if (typeTags == null) {
                    // That's OK, all properties are in TrainType, to let's reuse that tag
                    typeTags = Arrays.asList(trainTypeTag);
                }
                for (Tag typeTag : typeTags) {
                    newType = new TrainType();
                    if (defaultsTag != null) newType.configureFromXML(defaultsTag);
                    newType.configureFromXML(trainTypeTag);
                    newType.configureFromXML(typeTag);
                    lTrainTypes.add(newType);
                    mTrainTypes.put(newType.getName(), newType);
                    certType.addPotentialTrainType(newType);
                }
            }
        }
       

        // Special train buying rules
        Tag rulesTag = tag.getChild("TrainBuyingRules");
        if (rulesTag != null) {
            // A 1851 special
            trainPriceAtFaceValueIfDifferentPresidents = rulesTag.getChild("FaceValueIfDifferentPresidents") != null;
        }

        // Train obsolescence
        String obsoleteAttribute = tag.getAttributeAsString("ObsoleteTrainFor");
        if (Util.hasValue(obsoleteAttribute)) {
            try {
                obsoleteTrainFor= ObsoleteTrainForType.valueOf(obsoleteAttribute);
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
        }
        
    }

    public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        Map<Integer, String> newPhaseNames;
        Phase phase;
        String phaseName;
        PhaseManager phaseManager = root.getPhaseManager();
        
        for (TrainCertificateType certType : trainCertTypes) {
            certType.finishConfiguration(root);
            
            List<TrainType> types = certType.getPotentialTrainTypes();
            for (TrainType type : types) {
                type.finishConfiguration(root, certType);
            }
            
            // Now create the trains of this type
            Train train;
            // Multi-train certificates cannot yet be assigned a type
            TrainType initialType = types.size() == 1 ? types.get(0) : null;
            
            /* If the amount is infinite, only one train is created.
             * Each time this train is bought, another one is created.
             */
            for (int i = 0; i < (certType.hasInfiniteQuantity() ? 1 : certType.getQuantity()); i++) {
                train = Train.create(this, getNewUniqueId(certType.getId()), certType, initialType);
                addTrain(train);
                Bank.getUnavailable(this).getPortfolioModel().addTrain(train);
            }
            
            // Register any phase changes
            newPhaseNames = certType.getNewPhaseNames();
            if (newPhaseNames != null && !newPhaseNames.isEmpty()) {
                for (int index : newPhaseNames.keySet()) {
                    phaseName = newPhaseNames.get(index);
                    phase = (Phase)phaseManager.getPhaseByName(phaseName);
                    if (phase == null) {
                        throw new ConfigurationException ("New phase '"+phaseName+"' does not exist");
                    }
                    if (newPhases.get(certType) == null) newPhases.put(certType, new HashMap<Integer, Phase>());
                    newPhases.get(certType).put(index, phase);
                }
            }

        }

        // By default, set the first train type to "available".
        newTypeIndex.set(0);
        makeTrainAvailable(trainCertTypes.get(newTypeIndex.value()));
        
        // Discard Trains To where?
        if (discardToString.equalsIgnoreCase("pool")) {
            discardTo = root.getBank().getPool();
        } else if (discardToString.equalsIgnoreCase("scrapheap")) {
            discardTo = root.getBank().getScrapHeap();
        } else {
            throw new ConfigurationException ("Discard to only allow to pool or scrapheap");
        }

        // Trains "bought by foreigners" (1844, 1824)
        if (removeTrain) {
            root.getGameManager().setGameParameter(GameDef.Parm.REMOVE_TRAIN_BEFORE_SR, true);
        }
        
        // Train trading between different players at face value only (1851)
        root.getGameManager().setGameParameter(GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS,
                trainPriceAtFaceValueIfDifferentPresidents);
    }

    /** Create train without throwing exceptions.
     * To be used <b>after</b> completing initialization,
     * i.e. in cloning infinitely available trains.
     */

    public Train cloneTrain (TrainCertificateType certType) {
        Train train = null;
        List<TrainType> types = certType.getPotentialTrainTypes();
        TrainType initialType = types.size() == 1 ? types.get(0) : null;
        try {
            train = Train.create(this, getNewUniqueId(certType.getId()), certType, initialType);
        } catch (ConfigurationException e) {
            log.warn("Unexpected exception", e);
        }
        addTrain(train);
        return train;
    }

    public void addTrain (Train train) {
        trainMap.put(train.getId(), train);
        
        TrainCertificateType type = train.getCertType();
        if (!trainsPerCertType.containsKey(type)) {
            trainsPerCertType.put (type, new ArrayList<Train>());
        }
        trainsPerCertType.get(type).add(train);
    }

    public Train getTrainByUniqueId(String id) {
        return trainMap.get(id);
    }
    
    public int getNewUniqueId (String typeName) {
        int newUniqueId = lastIndexPerType.containsKey(typeName) ? lastIndexPerType.get(typeName) + 1 : 0;
        lastIndexPerType.put (typeName, newUniqueId);
        return newUniqueId;
    }
    

    /**
     * This method handles any consequences of new train buying (from the IPO),
     * such as rusting and phase changes. It must be called <b>after</b> the
     * train has been transferred.
     *
     */
    public void checkTrainAvailability(Train train, Owner from) {

        phaseHasChanged.set(false);
        if (from != Bank.getIpo(this)) return;

        TrainCertificateType boughtType, nextType;
        boughtType = train.getCertType();
        if (boughtType == (trainCertTypes.get(newTypeIndex.value()))
            && Bank.getIpo(this).getPortfolioModel().getTrainOfType(boughtType) == null) {
            // Last train bought, make a new type available.
            newTypeIndex.add(1);
            if (newTypeIndex.value() < lTrainTypes.size()) {
                nextType = (trainCertTypes.get(newTypeIndex.value()));
                if (nextType != null) {
                    if (!nextType.isAvailable()) {
                        makeTrainAvailable(nextType);
                        trainAvailabilityChanged.set(true);
                        ReportBuffer.add(this, "All " + boughtType.toText()
                                         + "-trains are sold out, "
                                         + nextType.toText() + "-trains now available");
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
    
    protected void makeTrainAvailable (TrainCertificateType type) {

        type.setAvailable();

        BankPortfolio to =
            (type.getInitialPortfolio().equalsIgnoreCase("Pool") ? Bank.getPool(this)
                    : Bank.getIpo(this));

        for (Train train : trainsPerCertType.get(type)) {
            to.getPortfolioModel().addTrain(train);
        }
    }

    // checks train obsolete condition
    private boolean isTrainObsolete(Train train, Owner lastBuyingCompany) {
        // check fist if train can obsolete at all
        if (!train.getCertType().isObsoleting()) return false;
        // and if it is in the pool (always rust)
        if (train.getOwner() == Bank.getPool(this)) return false;
        
        // then check if obsolete type
        if (obsoleteTrainFor == ObsoleteTrainForType.ALL) {
            return true;
        } else  { // otherwise it is AllExceptTriggering
            Owner owner = train.getOwner();
            return (owner instanceof PublicCompany && owner != lastBuyingCompany);
        }
    }

    protected void rustTrainType (TrainCertificateType type, Owner lastBuyingCompany) {
        type.setRusted();
        for (Train train : trainsPerCertType.get(type)) {
            Owner owner = train.getOwner();
            // check condition for train rusting
            if (isTrainObsolete(train, lastBuyingCompany)) {
                log.debug("Train " + train.getId() + " (owned by "
                        + owner.getId() + ") obsoleted");
                train.setObsolete();
                // TODO: is this still required?
                // train.getHolder().update();
            } else {
                log.debug("Train " + train.getId() + " (owned by "
                        + owner.getId() + ") rusted");
                train.setRusted();
            }
        }
        // report about event
        if (type.isObsoleting()) {
            ReportBuffer.add(this, LocalText.getText("TrainsObsolete." + obsoleteTrainFor, type.getId()));
        } else {
            ReportBuffer.add(this, LocalText.getText("TrainsRusted",type.getId()));
        }
    }
    
    public Set<Train> getAvailableNewTrains() {

        Set<Train> availableTrains = new TreeSet<Train>();
        Train train;

        for (TrainCertificateType type : trainCertTypes) {
            if (type.isAvailable()) {
                train = Bank.getIpo(this).getPortfolioModel().getTrainOfType(type);
                if (train != null) {
                    availableTrains.add(train);
                }
            }
        }
        return availableTrains;
    }

    public String getTrainCostOverview() {
        StringBuilder b = new StringBuilder();
        for (TrainCertificateType certType : trainCertTypes) {
            if (certType.getCost() > 0) {
                if (b.length() > 1) b.append(" ");
                b.append(certType.toText()).append(":").append(Bank.format(this, certType.getCost()));
                if (certType.getExchangeCost() > 0) {
                    b.append("(").append(Bank.format(this, certType.getExchangeCost())).append(")");
                }
            } else {
                for (TrainType type : certType.getPotentialTrainTypes()) {
                    if (b.length() > 1) b.append(" ");
                    b.append(type.getName()).append(":").append(Bank.format(this, type.getCost()));
                }
            }
        }
        return b.toString();
    }
    
    public TrainType getTypeByName(String name) {
        return mTrainTypes.get(name);
    }

    public List<TrainType> getTrainTypes() {
        return lTrainTypes;
    }

    public List<TrainCertificateType> getTrainCertTypes() {
        return trainCertTypes;
    }
    
    public TrainCertificateType getCertTypeByName (String name) {
        return trainCertTypeMap.get(name);
    }

    public boolean hasAvailabilityChanged() {
        return trainAvailabilityChanged.value();
    }

    public void resetAvailabilityChanged() {
        trainAvailabilityChanged.set(false);;
    }

    public boolean hasPhaseChanged() {
        return phaseHasChanged.value();
    }

    public boolean isAnyTrainBought () {
        return anyTrainBought.value();
    }
    
    public void setAnyTrainBought (boolean newValue) {
        if (isAnyTrainBought() != newValue) {
            anyTrainBought.set(newValue);
        }
    }
    
    public BankPortfolio discardTo() {
        return discardTo;
    }

    public List<TrainType> parseTrainTypes(String trainTypeName) {
        List <TrainType> trainTypes = new ArrayList<TrainType>();
        TrainType trainType;
        for (String trainTypeSingle : trainTypeName.split(",")) {
            trainType = getTypeByName(trainTypeSingle);
            if (trainType!= null) {
                trainTypes.add(trainType);
            } else {
                continue;
            }
        }
        
        return trainTypes;
    }  
    
}
 