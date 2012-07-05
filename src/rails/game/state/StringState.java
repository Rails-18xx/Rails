package rails.game.state;

/**
 * A stateful version of a String variable
 */
public final class StringState extends State {
    
    private String value;

    private StringState(Item parent, String id, String value) {
        super(parent, id);
        this.value = value;
    }

    /** 
     * Creates a StringState with default value of an empty string
     */
    public static StringState create(Item parent, String id){
        return new StringState(parent, id, "");
    }
    
    /**
     * @param text initial String
     */
    public static StringState create(Item parent, String id, String text){
        return new StringState(parent, id, text);
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
    public String value() {
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
