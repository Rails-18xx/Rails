package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;

import rails.game.state.ArrayListState;
import rails.game.state.Item;

/**
 * StorageModel is an implementation that stores arbitrary ownable objects
 * @author freystef
 *
 * @param <T> The type of objects to store
 */
@Deprecated
public class StorageModel<T extends Ownable> extends Model implements Storage<T> {

    private final ArrayListState<T> storageList;
    private Owner owner;
    
    /**
     * Defines a StorageModel with id that equals the clazz name
     */
    protected StorageModel(Class<T> clazz) {
        this(clazz, "");
    }
    
    /**
     * Defines a StorageModel with id that equals the class name extended by the postfix_id
     */
    protected StorageModel(Class<T> clazz, String postfix_id) {
        super(clazz.getName() + postfix_id);
        
        storageList = ArrayListState.create(clazz.getName());
    }
    
    /**
     * Creates an initialized StorageModel 
     */
    public static <T extends Ownable> StorageModel<T> create(Owner owner, Class<T> clazz) {
        StorageModel<T> holderModel = new StorageModel<T>(clazz).init(owner);
        owner.addStorage(holderModel, clazz);
        return holderModel;
    }
    
    /** 
     * @param parent restricted to Owners
     */
    @Override
    public StorageModel<T> init(Item parent){
        super.init(parent);
        if (parent instanceof Owner) {
            this.owner = (Owner)parent;
            storageList.init(this);
        } else {
            throw new IllegalArgumentException("StorageModel init() only works for Owners");
        }
        return this;
    }
    
    @Override
    public String toString() {
        return storageList.toString();
    }

    public final boolean addObject(T object){
        storageList.add(object);
        return true;
    }
    
    public final boolean addObject(T object, int position) {
        storageList.add(position, object);
        return true;
    }

    public final boolean removeObject(T object) {
        return storageList.remove(object);
    }
    
    public final Owner getOwner() {
        return owner;
    }

    public final T get(int index) {
        return storageList.get(index);
    }
    
    public final int getListIndex(T object) {
        return storageList.indexOf(object);
    }
    
    public final int size() {
        return storageList.size();
    }

    public final boolean isEmpty() {
        return storageList.isEmpty();
    }

    public ImmutableList<T> view() {
        return storageList.view();
    }
    
    public final Iterator<T> iterator() {
        return storageList.iterator();
    }

}
