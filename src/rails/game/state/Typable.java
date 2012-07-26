package rails.game.state;

/**
 * This interface can be implemented to define a specific type
 * It can be used e.g. to structure a Portfolio
 * @param <T> indicates the class used for typing
 */
public interface Typable<T> {

    public T getType();
    
}
