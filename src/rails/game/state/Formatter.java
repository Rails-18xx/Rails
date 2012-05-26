package rails.game.state;

/**
 * An interface defining a formatter for a state variable
 * @author freystef
 */
public interface Formatter<E extends State> {
    public String formatValue(E state);
}
