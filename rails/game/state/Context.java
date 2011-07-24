package rails.game.state;


/**
 * Contexts allow the storage of contextItems
 */
public interface Context extends Item {

    /**
     * @return return context item specified by id
     */
    public Item localize(String id);
    
    /**
     * adds context item 
     */
    public void addItem(Item item);
    
}
