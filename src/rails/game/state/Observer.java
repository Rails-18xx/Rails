package rails.game.state;

/**
 * An interface defining an Observer to Observable classes
 */
public interface Observer{

    void update(Observable observable, String text);
    
}
