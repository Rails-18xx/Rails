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
     * @param owner object containing state (usually this)
     * @param id id state variable
     */
    public BooleanState(Item owner, String id) {
        this(owner, id, false);
    }

    /**
     * @param owner object containing state (usually this)
     * @param id id state variable
     * @param value initial value
     */
    public BooleanState(Item owner, String id, boolean value) {
        super(owner, id);
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
