package rails.game.state;

/**
 * A stateful version of a boolean variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class BooleanState extends AbstractState {

    private boolean value;
    
    /**
     * Boolean state variable with default value false
     * @param id id state variable
     */
    public BooleanState(String id) {
        this(id, false);
    }

    /**
     * @param id id state variable
     * @param value initial value
     */
    public BooleanState(String id, boolean value) {
        super(id);
        this.value = value;
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
