package rails.game.state;

/**
 * A stateful version of a boolean variable
 */
public final class BooleanState extends State {
    
    private boolean value;
    
    private BooleanState(Item parent, String id, boolean value) {
        super(parent, id);
        this.value = value;
    }
    
    /** 
     * Creates a BooleanState with default value false
     */
    public static BooleanState create(Item parent, String id){
        return new BooleanState(parent, id, false);
    }
    
    /**
     * Creates a BooleanState with defined initial value
     * @param value initial value
     */
    public static BooleanState create(Item parent, String id, Boolean value){
        return new BooleanState(parent, id, value);
    }
    
    /**
     * @param value set state to this value
     */
    public void set(boolean value) {
        if (value != this.value) new BooleanChange(this, value);
    }

    /**
     * @return current value of state variable
     */
    public boolean value() {
        return value;
    }
    
    @Override
    public String toText() {
        return Boolean.toString(value);
    }

    void change(boolean value) {
        this.value = value;
    }
 
}
