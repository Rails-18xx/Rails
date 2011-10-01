package rails.game.model;

import java.util.Map;

import com.google.common.collect.Maps;

import rails.game.state.AbstractItem;
import rails.game.state.Item;

public abstract class DirectOwner extends AbstractItem implements Owner {

    private final Map<Class<? extends Ownable>, Holder<? extends Ownable>> holders
        = Maps.newHashMap();
    
    public DirectOwner() {
        super();
    }

    public DirectOwner(Item parent, String id) {
        super(parent, id);
    }
    
    public final <E extends Ownable> void addHolder(Holder<E> holder, Class<E> clazz){
        holders.put(clazz, holder);
    }
    
    public final <E extends Ownable> Holder<E> getHolder(Class<E> clazz) {
        @SuppressWarnings("unchecked")
        Holder<E> holder = (Holder<E>) holders.get(clazz);
        return holder;
    }
    
    public final <E extends Ownable> void addObject(E object) {
        @SuppressWarnings("unchecked")
        Holder<E> holder = (Holder<E>) holders.get(object.getClass());
        holder.addObject(object);
    }
    
    public final <E extends Ownable> void removeObject(E object) {
        @SuppressWarnings("unchecked")
        Holder<E> holder = (Holder<E>) holders.get(object.getClass());
        holder.removeObject(object);
    }
    
}
