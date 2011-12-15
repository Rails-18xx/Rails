package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;

import rails.game.state.ArrayListState;

/**
 * StorageModel is an implementation that stores arbitrary ownable objects
 * @author freystef
 *
 * @param <T> The type of objects to store
 */

public class StorageModel<T extends Ownable> extends AbstractModel<String> implements Storage<T> {

    private final ArrayListState<T> storageList;
    private final Owner owner;
    
    public static <T extends Ownable> StorageModel<T> create(Owner owner, Class<T> clazz) {
        StorageModel<T> holderModel = new StorageModel<T>(owner, clazz);
        owner.addStorage(holderModel, clazz);
        return holderModel;
    }
    
    protected StorageModel(Owner owner, Class<T> clazz) {
        this(owner, clazz, "");
    }
    
    protected StorageModel(Owner owner, Class<T> clazz, String postfix_id) {
        super(owner, clazz.getName() + postfix_id);
        this.owner = owner;
        
        // create the state variable
        storageList = new ArrayListState<T>(owner, clazz.getName());
        storageList.addObserver(this);
    }
        
    public String getData() {
        return storageList.getData();
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
