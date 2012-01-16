package rails.game.model;

import java.util.Map;

import com.google.common.collect.Maps;

import rails.game.state.GameItem;

public class DirectOwner extends GameItem implements Owner {

    private final Map<Class<? extends Ownable>, Storage<? extends Ownable>> storages
        = Maps.newHashMap();
    
    @Deprecated
    public DirectOwner() {
        super();
    }

    public DirectOwner(String id) {
        super(id);
    }
    
    public final <E extends Ownable> void addStorage(Storage<E> storage, Class<E> clazz){
        storages.put(clazz, storage);
    }
    
    public final <E extends Ownable> Storage<E> getStorage(Class<E> clazz) {
        @SuppressWarnings("unchecked")
        Storage<E> storage = (Storage<E>) storages.get(clazz);
        return storage;
    }
    
    public final <E extends Ownable> void addObject(E object) {
        @SuppressWarnings("unchecked")
        Storage<E> storage = (Storage<E>) storages.get(object.getClass());
        storage.addObject(object);
    }
    
    public final <E extends Ownable> void removeObject(E object) {
        @SuppressWarnings("unchecked")
        Storage<E> storage = (Storage<E>) storages.get(object.getClass());
        storage.removeObject(object);
    }
    
}
