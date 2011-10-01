package rails.game.model;

import rails.game.state.AbstractItem;
import rails.game.state.Item;

public abstract class SingleOwner<T extends Ownable> extends AbstractItem implements Owner {
    
    private final Holder<T> holder; 
    private final Class<T> clazz;
    
    public SingleOwner(Class<T> clazz){
        super();
        holder = new HolderModel<T>(this, clazz); 
        this.clazz = clazz;
    }

    public SingleOwner(Item parent, String id, Class<T> clazz){
        super(parent, id);
        holder = new HolderModel<T>(this, clazz); 
        this.clazz = clazz;
    }
    
    public <E extends Ownable> void addHolder(Holder<E> newHolder,
            Class<E> clazz) {
        throw new RuntimeException("SingleOwner cannot add other Holders");
    }
  
    public <E extends Ownable> Holder<E> getHolder(Class<E> clazz) {
        if (this.clazz != clazz) {
            throw new RuntimeException("SingleOwner connot hold objects of type" + clazz);
        }
        @SuppressWarnings("unchecked")
        Holder<E> holder = (Holder<E>)this.holder;
        return holder;
    }

    public <E extends Ownable> void addObject(E object) {
        if (this.clazz != object.getClass()) {
            throw new RuntimeException("SingleOwner connot hold objects of type" + clazz);
        }
        @SuppressWarnings("unchecked")
        Holder<E> holder = (Holder<E>)this.holder;
        holder.addObject(object);
    }

    public <E extends Ownable> void removeObject(E object) {
        if (this.clazz != object.getClass()) {
            throw new RuntimeException("SingleOwner connot hold objects of type" + clazz);
        }
        @SuppressWarnings("unchecked")
        Holder<E> holder = (Holder<E>)this.holder;
        holder.removeObject(object);
    }

}
