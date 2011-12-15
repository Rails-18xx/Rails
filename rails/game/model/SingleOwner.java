package rails.game.model;

import rails.game.state.AbstractItem;

public class SingleOwner<T extends Ownable> extends AbstractItem implements Owner {
    
    private final Storage<T> holder; 
    private final Class<T> clazz;
    
    @Deprecated
    public SingleOwner(Class<T> clazz){
        super();
        holder = new StorageModel<T>(this, clazz); 
        this.clazz = clazz;
    }

    public SingleOwner(String id, Class<T> clazz){
        super(id);
        holder = new StorageModel<T>(this, clazz); 
        this.clazz = clazz;
    }
    
    public <E extends Ownable> void addStorage(Storage<E> newHolder,
            Class<E> clazz) {
        throw new RuntimeException("SingleOwner cannot add other Holders");
    }
  
    public <E extends Ownable> Storage<E> getStorage(Class<E> clazz) {
        if (this.clazz != clazz) {
            throw new RuntimeException("SingleOwner connot hold objects of type" + clazz);
        }
        @SuppressWarnings("unchecked")
        Storage<E> holder = (Storage<E>)this.holder;
        return holder;
    }

    public <E extends Ownable> void addObject(E object) {
        if (this.clazz != object.getClass()) {
            throw new RuntimeException("SingleOwner connot hold objects of type" + clazz);
        }
        @SuppressWarnings("unchecked")
        Storage<E> holder = (Storage<E>)this.holder;
        holder.addObject(object);
    }

    public <E extends Ownable> void removeObject(E object) {
        if (this.clazz != object.getClass()) {
            throw new RuntimeException("SingleOwner connot hold objects of type" + clazz);
        }
        @SuppressWarnings("unchecked")
        Storage<E> holder = (Storage<E>)this.holder;
        holder.removeObject(object);
    }

}
