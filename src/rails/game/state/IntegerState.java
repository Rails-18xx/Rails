package rails.game.state;

/**
 * A stateful version of an integer variable
 */
public final class IntegerState extends State {

    private int value;
    
    private IntegerState(Item parent, String id, Integer value) {
        super(parent, id);
        this.value = value;
    }

    /** 
     * Creates an IntegerState with default value of Zero
     */
    public static IntegerState create(Item parent, String id){
        return new IntegerState(parent, id, 0);
    }
    
    /**
     * @param value initial value
     */
    public static IntegerState create(Item parent, String id, Integer value){
        return new IntegerState(parent, id, value);
    }

    public void set(int value) {
        if (value != this.value) new IntegerChange(this, value);
    }

    public int add(int value) {
        int newValue = this.value + value;
        set(this.value + value);
        return newValue;
    }

    public int value() {
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
