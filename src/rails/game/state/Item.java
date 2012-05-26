package rails.game.state;

/**
 * An item is defined by three attributes:
 * 
 * Id: A string identifier, which should be unique inside the context used.
 * Parent: The parent of the item in the item hierarchy
 * Context: The nearest context in the item hierarchy
 *  
 * At creation time the id is defined as a final value.
 * A constructor of an item typically has a String argument to set the id.
 * 
 * The definition of the parent is delayed until the initialization of the item,
 * however the contract of an item implies that it cannot be changed after the initialization.
 *  
 */
public interface Item {

    static final char SEP = '.';

    void init(Item parent, String id);
    
    boolean isInitialized();
    
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
