package rails.game.state;

/**
 * A stateful version of an integer variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class IntegerState extends AbstractState {

    private int value;
    
    /**
     * Integer state variable with default value zero
     * @param owner object containing state (usually this)
     * @param id id state variable
     */
    public IntegerState(Item owner, String id) {
        this(owner, id, 0);
    }

    /**
     * @param owner object containing state (usually this)
     * @param id id state variable
     * @param value initial value
     */
    public IntegerState(Item owner, String id, int value) {
        super(owner, id);
        this.value = value;
    }

    public void set(int value) {
        new IntegerChange(this, value);
    }

    public int add(int value) {
        int newValue = this.value + value;
        set(this.value + value);
        return newValue;
    }

    public int intValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    void change(int value) {
        this.value = value;
    }
    
}
