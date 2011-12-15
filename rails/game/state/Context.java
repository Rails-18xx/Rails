package rails.game.state;

/**
 * Contexts allow the storage and retrieval of items
 */
public interface Context extends Item {

    public static final char SEP = '.';
    
    /**
     * @return item specified by URI, if not found returns null
     */
    public Item localize(String uri);
    
    /**
     * adds item to context 
     */
    public void addItem(Item item);
    
}
