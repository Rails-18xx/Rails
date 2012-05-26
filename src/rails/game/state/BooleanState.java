package rails.game.state;

/**
 * A stateful version of a boolean variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class BooleanState extends State {
    
    private boolean value;
    
    private BooleanState(boolean value) {
        this.value = value;
    }
    
    /** 
     * Creates a BooleanState with default value false
     */
    public static BooleanState create(){
        return new BooleanState(false);
    }
    
    /**
     * @param value initial value
     */
    public static BooleanState create(Boolean value){
        return new BooleanState(value);
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
