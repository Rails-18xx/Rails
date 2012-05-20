package rails.game.state;

/**
 * A stateful version of an integer variable
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class IntegerState extends State {

    private int value;
    
    private IntegerState(Integer value) {
        this.value = value;
    }

    /** 
     * Creates an IntegerState with default value of Zero
     */
    public static IntegerState create(){
        return new IntegerState(0);
    }
    
    /**
     * @param value initial value
     */
    public static IntegerState create(Integer value){
        return new IntegerState(value);
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
