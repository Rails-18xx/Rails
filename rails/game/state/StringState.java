package rails.game.state;

/**
 * A stateful version of a String variable
 * 
 * @author Erik Vos, Stefan Frey (v2.0)
 */
public final class StringState extends State {
    
    private String value;

    private StringState(String value) {
        this.value = value;
    }

    /** 
     * Creates a StringState with default value of an empty string
     */
    public static StringState create(){
        return new StringState("");
    }
    
    /**
     * @param text initial String
     */
    public static StringState create(String text){
        return new StringState(text);
    }
    
    @Override
    public StringState init(Item parent, String id){
        super.init(parent, id);
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
