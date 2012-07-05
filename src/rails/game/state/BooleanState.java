package rails.game.state;

/**
 * A stateful version of a boolean variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
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
     * @param value initial value
     */
    public static BooleanState create(Item parent, String id, Boolean value){
        return new BooleanState(parent, id, value);
    }
    
    public void set(boolean value) {
        new BooleanChange(this, value);
    }

    public boolean value() {
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
