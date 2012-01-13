package rails.game.model;

/**
 * An interface defining an observer for states and models
 * 
 * It is a very simple approach that only relays the update information itself.
 * 
 * @author freystef
 */
public interface Observer{

    void update();
    
}
