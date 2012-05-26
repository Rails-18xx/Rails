package rails.game.state;
/**
 * Generic State wrapper
 * @author freystef
 *
 * @param <E> class to wrap
 */

public class GenericState<E> extends State {

    private E object;

    private GenericState(E object) {
        this.object = object;
    }

    /** 
     * Creates an empty GenericState
     */
    public static <E> GenericState<E> create(){
        return new GenericState<E>(null);
    }
    
    /**
     * @param object initial object contained
     */
    public static <E> GenericState<E> create(E object){
        return new GenericState<E>(object);
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
