package rails.game.model;

import rails.game.Bank;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.game.state.Item;
import rails.game.state.StringState;

/**
 * A model presenting money values
 */
public class MoneyModel extends Model {
    public static final int DEFAULT = 0;
    
    // Data
    private final IntegerState value;
    private BooleanState initialised;
    private StringState fixedText = null;
    
    // Options
    private boolean suppressZero;
    private boolean suppressInitialZero;
    private boolean addPlus;
    private boolean allowNegative;
 
    private MoneyModel(String id, int value){
        super(id);
        this.value = IntegerState.create("MoneyValue",  value);
    }
    
    /** 
     * Creates an owned MoneyModel with default value of zero
     */
    public static MoneyModel create(Item parent, String id){
        return new MoneyModel(id, DEFAULT).init(parent);
    }

    /** 
     * Creates an owned MoneyModel with predefined value
     */
    public static MoneyModel create(Item parent, String id, int value){
        return new MoneyModel(id, value).init(parent);
    }
    
    /**
     * Creates an unowned MoneyModel with default with zero value
     */
    public static MoneyModel create(String id) {
        return new MoneyModel(id, DEFAULT);
    }
    
    @Override
    public MoneyModel init(Item parent){
        super.init(parent);
        this.value.init(this);
        return this;
    }

    public void setSuppressZero(boolean suppressZero) {
        this.suppressZero = suppressZero;
    }

    public void setSuppressInitialZero(boolean suppressInitialZero) {
        this.suppressInitialZero = suppressInitialZero;
    }

    public void setAddPlus(boolean addPlus) {
        this.addPlus = addPlus;
    }

    public void setAllowNegative(boolean allowNegative) {
        this.allowNegative = allowNegative;
    }

    public void set(int value) {
        boolean forced = false;

        /* Set initialisation state only if it matters */
        if (suppressInitialZero && initialised == null) {
            initialised = BooleanState.create(this, "initialised", false);
        }
        if (initialised != null && !initialised.booleanValue()) {
            initialised.set(true);
            forced = true;
        }

        /*
         * At the end, as update() is called from here. Used setForced() to
         * ensure clients are updated even at an initial zero revenue.
         * TODO: Check if the missing forced handling matters
         */
        if (forced) {
            this.value.set(value);
        } else {
            this.value.set(value);
        }
    }
    
    public void add(int value) {
        this.value.add(value);
    }
    

    /** Set a fixed text, which will override the money value
     * as long as it is not null and not "".
     * @param text
     */
    public void setText (String text) {
        if (fixedText == null) {
            fixedText = StringState.create(this, "fixedText", text);
            fixedText.init(this);
        } else {
            fixedText.set(text);
        }
        // TODO: Still required?
        update();
    }
    
    public int intValue() {
        return value.intValue();
    }

    @Override
    public String toString() {
        if (fixedText != null && !"".equals(fixedText.stringValue())) {
            return fixedText.stringValue();
        }
        int amount = value.intValue();
        if (amount == 0
            && (suppressZero 
                    || suppressInitialZero
                        && (initialised == null || !initialised.booleanValue()))) {
            return "";
        } else if (amount < 0 && !allowNegative) {
            return "";
        } else if (addPlus) {
            return "+" + Bank.format(amount);
        } else {
            return Bank.format(amount);
        }
    }

}
