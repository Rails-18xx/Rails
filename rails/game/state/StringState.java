package rails.game.state;

/**
 * A stateful version of a String variable
 * 
 * @author Erik Vos, Stefan Frey (v2.0)
 */
public final class StringState extends State {
    
    public static final String DEFAULT = "";

    private String value;

    private StringState(String id, String value) {
        super(id);
        this.value = value;
    }

    /** 
     * Creates an owned StringState with default value of an empty string
     */
    public static StringState create(Item parent, String id){
        return new StringState(id, DEFAULT).init(parent);
    }
    
    /**
     * Creates an owned StringState
     * @param value initial value
     */
    public static StringState create(Item parent, String id, String value){
        return new StringState(id, value).init(parent);
    }
    
    /**
     * Creates an unowned StringState with default value of an empty string
     * Remark: Still requires a call to the init-method
     */
    public static StringState create(String id){
        return new StringState(id, DEFAULT);
    }
    
    @Override
    public StringState init(Item parent){
        super.init(parent);
        return this;
    }

    public void set(String value) {
        new StringChange(this, value);
    }
    
    /**
     * Append string to string state
     * No change is created if value to append is null or empty ("")
     * 
     * @param value string to append
     * @param delimiter to use before appending (only for non-empty value)
     */
    public void appendWithDelimiter(String value, String delimiter) {
        if (value == null || value.equals("") ) return;
        
        String newValue;
        if (this.value == null || this.value.equals("")) {
            newValue = value;
        } else {
            if (delimiter == null) {
                newValue = this.value + value;
            } else {
                newValue = this.value + delimiter + value;
            }
        }
        set(newValue);
    }
    
    /**
     * @return current value of string state
     */
    public String stringValue() {
        return value;
    }

    @Override 
    public String toString() {
        return value;
    }

    void change(String value) {
        this.value = value;
    }
}
