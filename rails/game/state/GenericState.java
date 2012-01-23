package rails.game.state;
/**
 * Generic State wrapper
 * @author freystef
 *
 * @param <E> class to wrap
 */

public class GenericState<E> extends State {

    private E object;

    private GenericState(String id, E object) {
        super(id);
        this.object = object;
    }

    /** 
     * Creates an owned and empty GenericState
     */
    public static <E> GenericState<E> create(Item parent, String id){
        return new GenericState<E>(id, null).init(parent);
    }
    
    /**
     * Creates an owned GenericState
     * @param value initial value
     */
    public static <E> GenericState<E> create(Item parent, String id, E object){
        return new GenericState<E>(id, object).init(parent);
    }
    
    /**
     * Creates an unowned and empty GenericState
     * Remark: Still requires a call to the init-method
     */
    public static <E> GenericState<E> create(String id){
        return new GenericState<E>(id, null);
    }
  
    @Override
    public GenericState<E> init(Item parent){
        super.init(parent);
        return this;
    }
 
    
    private void set(E object, boolean forced) {
        if (object == null) {
            if (this.object != null) {
                new GenericStateChange<E>(this, object);
            }
        } else if (!object.equals(this.object) || forced) {
                new GenericStateChange<E>(this, object);
        }
    }

    public void set(E object) {
        set(object, false);
    }

    public void setForced(E object) {
        set(object, true);
    }

    public E get() {
        return this.object;
    }

    void change(E object) {
        this.object = object; 
    }

}
