package rails.game.state;

/**
 * An interface defining an Observer to Observable classes
 */
public interface Observer{

    public void update(String text);
    
    public Observable getObservable();
    
}
