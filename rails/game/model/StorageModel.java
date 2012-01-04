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

public class StorageModel<T extends Ownable> extends Model<String> implements Storage<T> {

    private final ArrayListState<T> storageList;
    private Owner owner;
    
    /**
     * Create a StorageModel 
     * @param <T> type what to store
     * @param owner
     * @param clazz
     * @return the created StorageModel
     */
    public static <T extends Ownable> StorageModel<T> create(Owner owner, Class<T> clazz) {
        StorageModel<T> holderModel = new StorageModel<T>(clazz);
        holderModel.init(owner);
        owner.addStorage(holderModel, clazz);
        return holderModel;
    }
    
    /**
     * Defines a StorageModel with id that equals the clazz name
     * @param clazz
     */
    
    protected StorageModel(Class<T> clazz) {
        this(clazz, "");
    }
    
    /**
     * Defines a StorageModel with id that equals the class name extended by the postfix_id
     * @param clazz
     * @param postfix_id
     */
    protected StorageModel(Class<T> clazz, String postfix_id) {
        super(clazz.getName() + postfix_id);
        
        storageList = new ArrayListState<T>(clazz.getName());
    }
    
    /**
     * Initialization of a StorageModel only works for an Owner
     * @param owner 
     */
    public void init(Owner owner) {
        super.init(owner);
        this.owner = owner;
        storageList.init(this);
    }

    /** 
     * This method throws an IllegalArgumentException as StorageModel works only for Owners
     */
    @Override
    public void init(Item parent){
        throw new IllegalArgumentException("StorageModel init() only works for Owners");
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
