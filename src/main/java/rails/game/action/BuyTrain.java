package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import net.sf.rails.game.*;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTrainBuy;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

import com.google.common.base.Objects;

/**
 * Rails 2.0: Updated equals and toString methods (however see TODO below)
*/
public class BuyTrain extends PossibleORAction {

    // Initial settings
    private transient Train train;
    private String trainUniqueId;
    private transient Owner from;
    private String fromName;
    private int fixedCost;

    /** In 1837 also used for volutary train scrapping */
    private transient Set<Train> trainsForExchange = null;
    private String[] trainsForExchangeUniqueIds;

    /** This variable is set if and only if a major company has no trains.
     *
     * Its intended effect is to force a train buy if the company <i>has</i> a route.
     * Currently, its only effect is that, when its value is true,
     * an extra red-coloured message is displayed above the map.
     *
     * This variable replaces the disused variable forcedExchange since v2.3.1.
     */
    private boolean forcedBuyIfHasRoute = false;

    /** This variable is set if the company has no trains,
     * and is forced to buy one even if it has no route.
     * The latter condition is effectuated by setting
     * the game parameter MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE.
     * It is used in 18EU and similar games.
     *
     * The intended effect of this attribute is to force a train buy
     * if the company has <i>no</i> route.
     * Currently, its only effect is to differentiate
     * the red message above the map, mentioned above.
     *
     * Previously, this variable had the role that 'forcedBuyIfHasRoute' now has,
     * and as such it was grossly misnamed. It could not be renamed
     * for backwards compatibility reasons. Its usage was changed since v2.3.1.
     */
    private boolean forcedBuyIfNoRoute = false;


    private boolean presidentMustAddCash = false; // If buying from the bank
    private boolean presidentMayAddCash = false;  // If buying from a company

    /**
     * The amount of cash a company is missing to buy a train.
     * In SOH: any cash that can be raised by selling treasury
     * shares is subtracted.
     */
    private int presidentCashToAdd = 0;

    private transient SpecialTrainBuy specialProperty = null;
    private int specialPropertyId = 0;

    private String extraMessage = null;

    // Added jun2011 by EV to cover dual trains.
    // NOTE: Train objects from now on represent train *certificates*
    private transient TrainType type;
    private String typeName;

    // User settings
    private int pricePaid = 0;
    private int addedCash = 0;
    private transient Train exchangedTrain = null;
    private String exchangedTrainUniqueId;

    public static final long serialVersionUID = 2L;

    public BuyTrain(Train train, Owner from, int fixedCost) {
        this (train, train.getType(), from, fixedCost);
    }

    public BuyTrain(Train train, TrainType type, Owner from, int fixedCost) {
        super(train.getRoot());
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

    public BuyTrain setTrainForExchange(Train train) {
        Set<Train> trains = new HashSet<>();
        trains.add (train);
        setTrainsForExchange (trains);
        return this;
    }

    public BuyTrain setPresidentMustAddCash(int amount) {
        presidentMustAddCash = amount > 0;
        presidentCashToAdd = amount;
        return this;
    }

    public BuyTrain setPresidentMayAddCash(int amount) {
        presidentMayAddCash = amount > 0;
        presidentCashToAdd = amount;
        return this;
    }

    public void setForcedBuyIfNoRoute(boolean hasNoTrains) {
        this.forcedBuyIfNoRoute = hasNoTrains;
    }

    public void setForcedBuyIfHasRoute(boolean forcedBuyIfHasRoute) {
        this.forcedBuyIfHasRoute = forcedBuyIfHasRoute;
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
     * @return The train bought
     */
    public Train getTrain() {
        if (train == null) {
            train = root.getTrainManager().getTrainByUniqueId(trainUniqueId);
        }
        return train;
    }

    // Only for fixing BuyTrain actions by ListAndFixSavedFiles
    public void setTrain (Train train) {
        this.train = train;
        this.trainUniqueId = train.getId();
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

    public boolean isForcedBuyIfHasRoute() {
        return forcedBuyIfHasRoute;
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
        //boolean options =  Objects.equal(this.getTrain().getType(), action.getTrain().getType())
        boolean options =  Objects.equal(this.getTrain(), action.getTrain())
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

        // To shorten this long text, the booleans are made implicit
        String addCash = presidentMustAddCash ? "presMustAdd" :
                          presidentMayAddCash ? "presMayAdd" : "cashToAdd";
        String useSP = specialProperty != null
                ? specialProperty.getOriginalCompany().getId()
                : null;
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("train", train)
                    .addToString("from", from)
                    .addToString("fixedCost", fixedCost)
                    .addToString("trainsForExchange", trainsForExchange)
                    .addToString(addCash, presidentCashToAdd)
                    .addToString("useSP", useSP)
                    .addToStringOnlyActed("pricePaid", pricePaid)
                    .addToStringOnlyActed("addedCash", addedCash)
                    .addToStringOnlyActed("exchangedTrain", exchangedTrainUniqueId)
                .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (in instanceof GameLoader.RailsObjectInputStream) {
            // TEMPORARY Custom reading for backwards compatibility
            ObjectInputStream.GetField fields = in.readFields();
            trainUniqueId = (String) fields.get("trainUniqueId", trainUniqueId);
            typeName = (String) fields.get("typeName", null);
            fromName = (String) fields.get("fromName", fromName);
            fixedCost = fields.get("fixedCost", fixedCost);
            trainsForExchangeUniqueIds = (String[]) fields.get("trainsForExchangeUniqueIds", trainsForExchangeUniqueIds);
            forcedBuyIfNoRoute = fields.get("forcedBuyIfNoRoute", forcedBuyIfNoRoute);
            forcedBuyIfHasRoute = fields.get("forcedBuyIfHasRoute", forcedBuyIfHasRoute);
            presidentMustAddCash = fields.get("presidentMustAddCash", presidentMustAddCash);
            presidentMayAddCash = fields.get("presidentMayAddCash", presidentMayAddCash);
            presidentCashToAdd = fields.get("presidentCashToAdd", presidentCashToAdd);
            specialPropertyId = fields.get("specialPropertyId", specialPropertyId);
            pricePaid = fields.get("pricePaid", pricePaid);
            addedCash = fields.get("addedCash", addedCash);
            exchangedTrainUniqueId = (String) fields.get("exchangedTrainUniqueId", exchangedTrainUniqueId);
            extraMessage = (String) fields.get("extraMessage", extraMessage);

            TrainManager trainManager = root.getTrainManager();
            CompanyManager companyManager = root.getCompanyManager();

            fromName = companyManager.checkAlias(fromName);

            train = trainManager.getTrainByUniqueId(trainUniqueId);
            // Note: the 2nd etc. copy of an unlimited quantity train will become null this way.
            // Set getTrain() for how this is fixed.
            if (typeName == null) {
                if (train == null) {
                    // Kludge to cover not yet cloned unlimited trains
                    typeName = trainUniqueId.split("_")[0];
                    type = trainManager.getTrainTypeByName(typeName);
                } else {
                    type = train.getType();
                    typeName = type.getName();
                }
            } else {
                type = trainManager.getTrainTypeByName(typeName);
            }

            // TODO: This has to be replaced by a new mechanism for owners at some time
            from = root.getPortfolioManager().getPortfolioByName(fromName).getParent();
            if (trainsForExchangeUniqueIds != null && trainsForExchangeUniqueIds.length > 0) {
                trainsForExchange = new HashSet<>();
                for (String trainsForExchangeUniqueId : trainsForExchangeUniqueIds) {
                    trainsForExchange.add(trainManager.getTrainByUniqueId(trainsForExchangeUniqueId));
                }
            }

            if (specialPropertyId > 0) {
                specialProperty = (SpecialTrainBuy) SpecialProperty.getByUniqueId(root, specialPropertyId);
            }

            if (Util.hasValue(exchangedTrainUniqueId)) {
                exchangedTrain = trainManager.getTrainByUniqueId(exchangedTrainUniqueId);
            }
        } else {
            in.defaultReadObject();
        }
    }

    public void applyRailsRoot(RailsRoot root) {
        super.applyRailsRoot(root);

        TrainManager trainManager = root.getTrainManager();
        CompanyManager companyManager = root.getCompanyManager();

        fromName = companyManager.checkAlias(fromName);

        train = trainManager.getTrainByUniqueId(trainUniqueId);
        // Note: the 2nd etc. copy of an unlimited quantity train will become null this way.
        // Set getTrain() for how this is fixed.
        if (typeName == null) {
            if (train == null) {
                // Kludge to cover not yet cloned unlimited trains
                typeName = trainUniqueId.split("_")[0];
                type = trainManager.getTrainTypeByName(typeName);
            } else {
                type = train.getType();
                typeName = type.getName();
            }
        } else {
            type = trainManager.getTrainTypeByName(typeName);
        }

        // TODO: This has to be replaced by a new mechanism for owners at some time
        from = root.getPortfolioManager().getPortfolioByName(fromName).getParent();
        if (trainsForExchangeUniqueIds != null && trainsForExchangeUniqueIds.length > 0) {
            trainsForExchange = new HashSet<>();
            for (String trainsForExchangeUniqueId : trainsForExchangeUniqueIds) {
                trainsForExchange.add(trainManager.getTrainByUniqueId(trainsForExchangeUniqueId));
            }
        }

        if (specialPropertyId > 0) {
            specialProperty = (SpecialTrainBuy) SpecialProperty.getByUniqueId(root, specialPropertyId);
        }

        if (Util.hasValue(exchangedTrainUniqueId)) {
            exchangedTrain = trainManager.getTrainByUniqueId(exchangedTrainUniqueId);
        }
    }

}
