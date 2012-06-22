package rails.game.state;
/**
 * Generic State wrapper
 * @author freystef
 *
 * @param <E> class to wrap
 */

public final class GenericState<E> extends State {

    private E object;

    private GenericState(Item parent, String id, E object) {
        super(parent, id);
        this.object = object;
    }

    /** 
     * Creates an empty GenericState
     */
    public static <E> GenericState<E> create(Item parent, String id){
        return new GenericState<E>(parent, id, null);
    }
    
    /**
     * @param object initial object contained
     */
    public static <E> GenericState<E> create(Item parent, String id, E object){
        return new GenericState<E>(parent, id, object);
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
