package rails.game.state;

/**
 * A stateful version of a boolean variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class BooleanState extends State {

    public static final boolean DEFAULT = false; 
    
    private boolean value;
    
    private BooleanState(String id, boolean value) {
        super(id);
        this.value = value;
    }
    
    /** 
     * Creates an owned BooleanState with default value false
     */
    public static BooleanState create(Item parent, String id){
        return new BooleanState(id, DEFAULT).init(parent);
    }
    
    /**
     * Creates an owned BooleanState
     * @param value initial value
     */
    public static BooleanState create(Item parent, String id, Boolean value){
        return new BooleanState(id, value).init(parent);
    }
    
    /**
     * Creates an unowned BooleanState with default value false
     * Remark: Still requires a call to the init-method
     */
    public static BooleanState create(String id){
        return new BooleanState(id, DEFAULT);
    }
    
    @Override
    public BooleanState init(Item parent){
        super.init(parent);
        return this;
    }

    public void set(boolean value) {
        new BooleanChange(this, value);
    }

    public boolean booleanValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    void change(boolean value) {
        this.value = value;
    }
 
}
