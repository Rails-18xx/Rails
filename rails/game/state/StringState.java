package rails.game.state;

/**
 * A stateful version of a String variable
 * 
 * @author Erik Vos, Stefan Frey (v2.0)
 */
public final class StringState extends AbstractState {

    private String value;

    /**
     * String state variable with default value of an empty string ""
     * @param id id state variable
     */
    public StringState(String id) {
        this(id, "");
    }
    
    /**
     * @param id id state variable
     * @param value initial value
     */
    public StringState(String id, String value) {
        super(id);
        this.value = value;
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
