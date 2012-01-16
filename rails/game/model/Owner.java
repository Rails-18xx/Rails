package rails.game.model;

import rails.game.state.Item;

/**
 * The Owner interface is implemented by all classes that wish
 * to hold Rails Ownable objects
 * 
 * This can be done either
 * A) Directly by implementing the Holder interface
 * B) Directly by sub-classing the AbstractHolder abstract class
 * C) Indirectly by incorporating a Portfolio
 * 
 * 
 * @author freystef
 *
 */
public interface Owner extends Item {
    
    public <E extends Ownable> void addStorage(Storage<E> newHolder, Class<E> clazz);
    
    public <E extends Ownable> Storage<E> getStorage(Class<E> clazz);

    public <E extends Ownable> void addObject(E object);

    public <E extends Ownable> void removeObject(E object);
    
}
