package rails.game.state;
/**
 * A context describe a service that allows to locate items
 */
public interface Context extends Item {

    public Item localize(String uri);

    public void addItem(Item item);

    public void removeItem(Item item);
    
    public Root getRoot();
    
}
