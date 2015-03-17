package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTrainBuy;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

import com.google.common.base.Objects;

/**
 * Rails 2.0: Updated equals and toString methods (however see TODO below)
*/
public class BuyTrain extends PossibleORAction {

    // Initial settings
    transient private Train train;
    private String trainUniqueId;
    transient private Owner from;
    private String fromName;
    private int fixedCost = 0;
    private boolean forcedBuyIfNoRoute = false; // TODO Can be disabled once route checking exists
    transient private Set<Train> trainsForExchange = null;
    private String[] trainsForExchangeUniqueIds;

    /** Obsolete, but left in for backwards compatibility of saved files */
    private boolean forcedExchange = false;

    private boolean presidentMustAddCash = false; // If buying from the bank
    private boolean presidentMayAddCash = false;  // If buying from a company
    private int presidentCashToAdd = 0;

    transient private SpecialTrainBuy specialProperty = null;
    private int specialPropertyId = 0;

    private String extraMessage = null;
    
    // Added jun2011 by EV to cover dual trains.
    // NOTE: Train objects from now on represent train *certificates* 
    transient private TrainType type;
    private String typeName;

    // User settings
    private int pricePaid = 0;
    private int addedCash = 0;
    transient private Train exchangedTrain = null;
    private String exchangedTrainUniqueId;

    public static final long serialVersionUID = 2L;

    public BuyTrain(Train train, Owner from, int fixedCost) {

        this (train, train.getType(), from, fixedCost);
    }

    public BuyTrain(Train train, TrainType type, Owner from, int fixedCost) {
        this.train = train;
        this.trainUniqueId = train.getId();
        this.from = from;
        this.fromName = from.getId();
        this.fixedCost = fixedCost;
        this.type = type;
        this.typeName = type.getName();
    }

    public BuyTrain setTrainsForExchange(Set<Train> trains) {
        trainsForExchange = trains;
        if (trains != null) {
            trainsForExchangeUniqueIds = new String[trains.size()];
            int i = 0;
            for (Train train:trains) {
                trainsForExchangeUniqueIds[i++] = train.getId();
            }
        }
        return this;
    }

    public BuyTrain setPresidentMustAddCash(int amount) {
        presidentMustAddCash = true;
        presidentCashToAdd = amount;
        return this;
    }

    public BuyTrain setPresidentMayAddCash(int amount) {
        presidentMayAddCash = true;
        presidentCashToAdd = amount;
        return this;
    }

    public void setForcedBuyIfNoRoute(boolean hasNoTrains) {
        this.forcedBuyIfNoRoute = hasNoTrains;
    }

    public void setExtraMessage (String message) {
        extraMessage = message;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    /**
     * @return Returns the specialProperty.
     */
    public SpecialTrainBuy getSpecialProperty() {
        return specialProperty;
    }

    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialTrainBuy specialProperty) {
        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
    }

    public boolean hasSpecialProperty() {
        return specialProperty != null;
    }

    /**
     * To be used for all usage of train, also within this class.
     * After reloading the 2nd copy etc. of a train with unlimited quantity,
     * the train attribute will be null (because readObject() is called and the
     * train is initiated before the actions have been executed - the second
     * train is in this case only created after buying the first one).
     * @return
     */
    public Train getTrain() {
        if (train == null) {
            train = RailsRoot.getInstance().getTrainManager().getTrainByUniqueId(trainUniqueId);
        }
        return train;
    }

    public TrainType getType() {
        return type;
    }

    public Owner getFromOwner() {
        return from;
    }

    public int getFixedCost() {
        return fixedCost;
    }
    
    // for correction
    public void setFixedCost(int fixedCost) {
        this.fixedCost = fixedCost;
    }

    public boolean isForExchange() {
        return trainsForExchange != null && !trainsForExchange.isEmpty();
    }

    public Set<Train> getTrainsForExchange() {
        return trainsForExchange;
    }

    public boolean mustPresidentAddCash() {
        return presidentMustAddCash;
    }

    public boolean mayPresidentAddCash() {
        return presidentMayAddCash;
    }

    public int getPresidentCashToAdd() {
        return presidentCashToAdd;
    }

    public boolean isForcedBuyIfNoRoute() {
        return forcedBuyIfNoRoute;
    }

    public Owner getOwner() {
        return getTrain().getOwner();
    }

    public int getAddedCash() {
        return addedCash;
    }

    public void setAddedCash(int addedCash) {
        this.addedCash = addedCash;
    }

    public int getPricePaid() {
        return pricePaid;
    }

    public void setPricePaid(int pricePaid) {
        this.pricePaid = pricePaid;
    }

    public Train getExchangedTrain() {
        return exchangedTrain;
    }

    public void setExchangedTrain(Train exchangedTrain) {
        this.exchangedTrain = exchangedTrain;
        if (exchangedTrain != null)
            this.exchangedTrainUniqueId = exchangedTrain.getId();
    }

    
    
    // TODO: Check for and add the missing attributes
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        BuyTrain action = (BuyTrain)pa; 
        boolean options =  Objects.equal(this.getTrain().getType(), action.getTrain().getType()) 
                // only types have to be equal, and the getTrain() avoids train == null
                && Objects.equal(this.from, action.from)
                && (action.fixedCost == 0 || Objects.equal(this.fixedCost, action.pricePaid))
                && Objects.equal(this.trainsForExchange, action.trainsForExchange)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.train, action.train)
                && Objects.equal(this.pricePaid, action.pricePaid)
                && Objects.equal(this.addedCash, action.addedCash)
                && Objects.equal(this.exchangedTrainUniqueId, action.exchangedTrainUniqueId)
        ;
    }

    // TODO: Check for and add the missing attributes
    @Override
    public String toString() {

        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("train", train)
                    .addToString("from", from)
                    .addToString("fixedCost", fixedCost)
                    .addToString("trainsForExchange", trainsForExchange)
                    .addToStringOnlyActed("pricePaid", pricePaid)
                    .addToStringOnlyActed("addedCash", addedCash)
                    .addToStringOnlyActed("exchangedTrainUniqueId", exchangedTrainUniqueId)
                .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

//        in.defaultReadObject();
        // TEMPORARY Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();
        trainUniqueId = (String) fields.get("trainUniqueId", trainUniqueId);
        typeName = (String) fields.get("typeName", null);
        fromName = (String) fields.get("fromName", fromName);
        fixedCost = fields.get("fixedCost", fixedCost);
        forcedBuyIfNoRoute = fields.get("forcedBuyIfNoRoute", forcedBuyIfNoRoute);//TEMPORARY
        trainsForExchangeUniqueIds = (String[]) fields.get("trainsForExchangeUniqueIds", trainsForExchangeUniqueIds);
        forcedExchange = fields.get("forcedExchange", forcedExchange);
        presidentMustAddCash = fields.get("presidentMustAddCash", presidentMustAddCash);
        presidentMayAddCash = fields.get("presidentMayAddCash", presidentMayAddCash);
        presidentCashToAdd = fields.get("presidentCashToAdd", presidentCashToAdd);
        specialPropertyId = fields.get("specialPropertyId", specialPropertyId);
        pricePaid = fields.get("pricePaid", pricePaid);
        addedCash = fields.get("addedCash", addedCash);
        exchangedTrainUniqueId = (String) fields.get("exchangedTrainUniqueId", exchangedTrainUniqueId);
        extraMessage = (String) fields.get("extraMessage", extraMessage);

        RailsRoot root = RailsRoot.getInstance();
        TrainManager trainManager = root.getTrainManager();
        CompanyManager companyManager = root.getCompanyManager();

        fromName = companyManager.checkAlias (fromName);

        train = trainManager.getTrainByUniqueId(trainUniqueId);
        // Note: the 2nd etc. copy of an unlimited quantity train will become null this way.
        // Set getTrain() for how this is fixed.
        if (typeName == null) {
            if (train == null) {
                // Kludge to cover not yet cloned unlimited trains
                typeName = trainUniqueId.split("_")[0];
                type = trainManager.getTypeByName(typeName);
            } else {
                type = train.getType();
                typeName = type.getName();
            }
        } else {
            type = trainManager.getTypeByName(typeName);
        }

        // TODO: This has to be replaced by a new mechanism for owners at some time
        from = getGameManager().getPortfolioByName(fromName).getParent();
        if (trainsForExchangeUniqueIds != null
            && trainsForExchangeUniqueIds.length > 0) {
            trainsForExchange = new HashSet<Train>();
            for (int i = 0; i < trainsForExchangeUniqueIds.length; i++) {
                trainsForExchange.add(trainManager.getTrainByUniqueId(trainsForExchangeUniqueIds[i]));
            }
        }

        if (specialPropertyId > 0) {
            specialProperty =
                    (SpecialTrainBuy) SpecialProperty.getByUniqueId(getRoot(), specialPropertyId);
        }

        if (Util.hasValue(exchangedTrainUniqueId)) {
            exchangedTrain = trainManager.getTrainByUniqueId(exchangedTrainUniqueId);
        }
    }

}
