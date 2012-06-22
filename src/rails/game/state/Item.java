package rails.game.state;

/**
 * An item is defined by two (final) attributes:
 * 
 * Id: A string identifier, which should be unique inside the context used.
 * Parent: The parent of the item in the item hierarchy
 * 
 * 
 * Implied attributes are
 * Context: The nearest context in the item hierarchy
 * URI: From the nearest context
 * FullURI: From the root context
 */
public interface Item {

    static final char SEP = '.';

    String getId();

    Item getParent();
    
    Context getContext();

    /** 
     * @return a string which allows to identify the item in the Context
     */
    String getURI();
    
    /**
     * @return a string which allows to locate the item from the Root
     */
    String getFullURI();
}
