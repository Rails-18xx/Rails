package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTrainBuy;
import net.sf.rails.game.state.Owner;
import net.sf.rails.ui.swing.ORPanel;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Objects;
// These imports are from your file
import rails.game.action.PriceMode;
import rails.game.action.PossibleAction;
import rails.game.action.LayTile;
import rails.game.action.LayBaseToken;
import rails.game.action.NullAction;

public class BuyTrain extends PossibleORAction {

    /**
     * The Mode enum has been added...
     * [Lines 29-61: Existing enum definition, unchanged]
     */

    protected static final Logger log = LoggerFactory.getLogger(ORPanel.class);

    public enum Mode {
        FREE,
        FIXED,
        MIN,
        MAX
    }

    // [Lines 64-143: Existing field definitions, unchanged]
    private transient Train train;
    private String trainUniqueId;
    private transient Owner from;
    private String fromName;
    private int fixedCost;
    private transient Set<Train> trainsForExchange = null;
    private String[] trainsForExchangeUniqueIds;
    private boolean forcedBuyIfHasRoute = false;
    private boolean forcedBuyIfNoRoute = false;
    private boolean presidentMustAddCash = false; // If buying from the bank
    private boolean presidentMayAddCash = false; // If buying from a company
    private int loansToTake = 0; // 1826, if buying from the Bank
    private int presidentCashToAdd = 0;
    private transient SpecialTrainBuy specialProperty = null;
    private int specialPropertyId = 0;
    private String extraMessage = null;
    private transient TrainType type;
    private String typeName;
    private transient Mode fixedCostMode;
    private int modeOrdinal;
    private int pricePaid = 0;
    private int addedCash = 0;
    private transient Train exchangedTrain = null;
    private String exchangedTrainUniqueId;
    public static final long serialVersionUID = 2L;

    // --- NEW FIELDS FOR HYBRID AI/HUMAN MODEL ---
    private PriceMode priceMode;
    private int minPrice;
    private int maxPrice;


    /**
     * --- NEW CONSTRUCTOR (For VARIABLE_PRICE / Human) ---
     * Captures a price range.
     */
    public BuyTrain(Train train, TrainType type, Owner from, int minPrice, int maxPrice, PriceMode mode) {
        super(train.getRoot());
        this.train = train;
        this.trainUniqueId = train.getId();
        this.from = from;
        this.fromName = from.getId();
        this.type = type;
        this.typeName = type.getName();
        
        this.priceMode = mode; // Will be VARIABLE
        this.minPrice = minPrice;
        this.maxPrice = maxPrice; // Can be Integer.MAX_VALUE
        
        // Set old fields to sensible defaults
        this.fixedCost = 0; 
        // --- [BUG FIX] ---
        // We set the mode, but *not* to FREE if fixedCost is 0
        // as this constructor is also used for fixed-price (min=max) IPO buys.
        if (minPrice == maxPrice) {
            setFixedCostMode(Mode.FIXED);
            this.fixedCost = minPrice; // Store the fixed price
        } else {
            setFixedCostMode(Mode.FREE);
        }
        // --- [END BUG FIX] ---
    }
    
    // This is the original constructor
    public BuyTrain(Train train, Owner from, int fixedCost) {
        this(train, train.getType(), from, fixedCost);
    }

    /**
     * --- [CORRECTED CONSTRUCTOR] ---
     * This is the original constructor, now modified to ALSO
     * work for the AI. It correctly sets all new and old fields.
     */
    public BuyTrain(Train train, TrainType type, Owner from, int fixedCost) {
        super(train.getRoot());
        this.train = train;
        this.trainUniqueId = train.getId();
        this.from = from;
        this.fromName = from.getId();
        this.type = type;
        this.typeName = type.getName();
        
        this.fixedCost = fixedCost; // Store the cost

        // Set the implicit mode values
        if (fixedCost == 0) {
            setFixedCostMode(Mode.FREE);
        } else {
            setFixedCostMode(Mode.FIXED);
        }

        // This constructor is now also used by the AI.
        // Set all the new fields to FIXED mode.
        this.priceMode = PriceMode.FIXED;
        this.minPrice = fixedCost;
        this.maxPrice = fixedCost;
        this.pricePaid = fixedCost; // Pre-set the price
    }


    public PriceMode getPriceMode() {
        // Handle null for old, unmodified actions
        return (this.priceMode != null) ? this.priceMode : PriceMode.FIXED;
    }

    public int getMinPrice() {
        return this.minPrice;
    }

    public int getMaxPrice() {
        return this.maxPrice;
    }

    // [Lines 230-496: All other existing methods, unchanged]
    public BuyTrain setTrainsForExchange(Set<Train> trains) {
        trainsForExchange = trains;
        if (trains != null) {
            trainsForExchangeUniqueIds = new String[trains.size()];
            int i = 0;
            for (Train train : trains) {
                trainsForExchangeUniqueIds[i++] = train.getId();
            }
        } else {
            trainsForExchangeUniqueIds = null;
        }
        return this;
    }

    public BuyTrain setTrainForExchange(Train train) {
        Set<Train> trains = new HashSet<>();
        trains.add(train);
        setTrainsForExchange(trains);
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

    public void setExtraMessage(String message) {
        extraMessage = message;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public int getLoansToTake() {
        return loansToTake;
    }

    public void setLoansToTake(int loansToTake) {
        this.loansToTake = loansToTake;
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
     * * @return The train bought
     */
    public Train getTrain() {
        if (train == null) {
            train = root.getTrainManager().getTrainByUniqueId(trainUniqueId);
        }
        return train;
    }

    // Only for fixing BuyTrain actions by ListAndFixSavedFiles
    public void setTrain(Train train) {
        this.train = train;
        this.trainUniqueId = train.getId();
    }

    public TrainType getType() {
        return type;
    }

    public Owner getFromOwner() {
        return from;
    }

    public String getFromName() {
        return fromName;
    }

    public int getFixedCost() {
        return fixedCost;
    }

    public Mode getFixedCostMode() {
        return fixedCostMode;
    }

    public void setFixedCostMode(Mode fixedCostMode) {
        this.fixedCostMode = fixedCostMode;
        this.modeOrdinal = fixedCostMode == null ? 0 : fixedCostMode.ordinal();
    }

    public int getModeOrdinal() {
        return fixedCostMode.ordinal();
    }

    // For use in ListAndFixSavedFiles only. Mode is not changed!
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

    public void setPresidentCashToAdd(int presidentCashToAdd) {
        this.presidentCashToAdd = presidentCashToAdd;
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
        // Allow the UI to override the price during human negotiation,
        // even if the legacy engine constructed this action as FIXED.
        if (this.priceMode == PriceMode.FIXED && pricePaid > 0) {
            this.pricePaid = pricePaid;
        } else if (this.priceMode == PriceMode.FIXED) {
            this.pricePaid = this.fixedCost; // Ignore external input if mode is fixed and no valid price passed
        } else {
            // Clamp the value to the permitted minimum and maximum bounds
            if (pricePaid < this.minPrice) {
                this.pricePaid = this.minPrice;
            } else if (pricePaid > this.maxPrice) {
                this.pricePaid = this.maxPrice;
            } else {
                this.pricePaid = pricePaid;
            }
        }

        // Absolute fallback to prevent negative money errors if bounds were improperly
        // initialized
        if (this.pricePaid < 0) {
            this.pricePaid = 1;
        }
    }

    public Train getExchangedTrain() {
        return exchangedTrain;
    }

    public void setExchangedTrain(Train exchangedTrain) {
        this.exchangedTrain = exchangedTrain;
        if (exchangedTrain != null) {
            this.exchangedTrainUniqueId = exchangedTrain.getId();
        } else {
            this.exchangedTrainUniqueId = null;
        }
    }

    // TODO: Check for and add the missing attributes
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this)
            return true;
        // super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption))
            return false;

        // check asOption attributes
        BuyTrain action = (BuyTrain) pa;
        // boolean options = Objects.equal(this.getTrain().getType(),
        // action.getTrain().getType())
        boolean options = Objects.equal(this.getTrain(), action.getTrain())
                // only types have to be equal, and the getTrain() avoids train == null
                && Objects.equal(this.from, action.from)
                && this.fixedCost == action.fixedCost
                && Objects.equal(this.trainsForExchange, action.trainsForExchange)
                && this.loansToTake == action.loansToTake;

        // finish if asOptions check
        if (asOption)
            return options;

        // check asAction attributes
        return options
                && Objects.equal(this.train, action.train)
                // && Objects.equal(this.pricePaid, action.pricePaid)
                && Objects.equal(this.addedCash, action.addedCash)
                && Objects.equal(this.exchangedTrainUniqueId, action.exchangedTrainUniqueId);
    }

    // TODO: Check for and add the missing attributes
    @Override
    public String toString() {

        // To shorten this long text, the booleans are made implicit
        String addCash = presidentMustAddCash ? "presMustAdd" : presidentMayAddCash ? "presMayAdd" : "cashToAdd";
        String useSP = specialProperty != null
                ? specialProperty.getOriginalCompany().getId()
                : null;
        return super.toString() +
                RailsObjects.stringHelper(this)
                        .addToString("train", train)
                        .addToString("from", from)
                        .addToString("fixedCost", fixedCost)
                        .addToString("mode",
                                (fixedCostMode != null ? fixedCostMode.toString() : ""))
                        .addToString("trainsForExchange", trainsForExchange)
                        .addToString(addCash, presidentCashToAdd)
                        .addToString("loansToTake", loansToTake)
                        .addToString("useSP", useSP)
                        .addToStringOnlyActed("pricePaid", pricePaid)
                        .addToStringOnlyActed("addedCash", addedCash)
                        .addToStringOnlyActed("exchangedTrain", exchangedTrainUniqueId)
                        .toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // TEMPORARY Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();
        trainUniqueId = (String) fields.get("trainUniqueId", trainUniqueId);
        typeName = (String) fields.get("typeName", null);
        fromName = (String) fields.get("fromName", fromName);
        fixedCost = fields.get("fixedCost", fixedCost);
        modeOrdinal = fields.get("modeOrdinal", 0);
        fixedCostMode = Mode.values()[modeOrdinal];
        trainsForExchangeUniqueIds = (String[]) fields.get("trainsForExchangeUniqueIds", trainsForExchangeUniqueIds);
        forcedBuyIfNoRoute = fields.get("forcedBuyIfNoRoute", forcedBuyIfNoRoute);
        forcedBuyIfHasRoute = fields.get("forcedBuyIfHasRoute", forcedBuyIfHasRoute);
        presidentMustAddCash = fields.get("presidentMustAddCash", presidentMustAddCash);
        presidentMayAddCash = fields.get("presidentMayAddCash", presidentMayAddCash);
        presidentCashToAdd = fields.get("presidentCashToAdd", presidentCashToAdd);
        specialPropertyId = fields.get("specialPropertyId", specialPropertyId);
        pricePaid = fields.get("pricePaid", pricePaid);
        addedCash = fields.get("addedCash", addedCash);
        loansToTake = fields.get("loansToTake", loansToTake);
        exchangedTrainUniqueId = (String) fields.get("exchangedTrainUniqueId", exchangedTrainUniqueId);
        extraMessage = (String) fields.get("extraMessage", extraMessage);

        TrainManager trainManager = root.getTrainManager();
        CompanyManager companyManager = root.getCompanyManager();

        fromName = companyManager.checkAlias(fromName);

        train = trainManager.getTrainByUniqueId(trainUniqueId);
        // Note: the 2nd etc. copy of an unlimited quantity train will become null this
        // way.
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