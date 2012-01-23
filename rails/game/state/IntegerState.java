package rails.game.state;

/**
 * A stateful version of an integer variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class IntegerState extends State {

    public static final int DEFAULT = 0;

    private int value;
    
    private IntegerState(String id, Integer value) {
        super(id);
        this.value = value;
    }

    /** 
     * Creates an owned IntegerState with default value of Zero
     */
    public static IntegerState create(Item parent, String id){
        return new IntegerState(id, DEFAULT).init(parent);
    }
    
    /**
     * Creates an owned IntegerState
     * @param value initial value
     */
    public static IntegerState create(Item parent, String id, Integer value){
        return new IntegerState(id, value).init(parent);
    }
    
    /**
     * Creates an unowned IntegerState with default value of Zero
     * Remark: Still requires a call to the init-method
     */
    public static IntegerState create(String id){
        return new IntegerState(id, DEFAULT);
    }

    /**
     * Creates an unowned IntegerState
     * Remark: Still requires a call to the init-method
     * @param value initial value
     */
    public static IntegerState create(String id, Integer value){
        return new IntegerState(id, value);
    }
    
    @Override
    public IntegerState init(Item parent){
        super.init(parent);
        return this;
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
