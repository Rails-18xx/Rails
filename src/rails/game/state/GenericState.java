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
     * {@inheritDoc}
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

    public void set(E object) {
        if (object == null) {
            if (this.object != null) {
                new GenericStateChange<E>(this, object);
            }
        } else if (object != this.object) {
            new GenericStateChange<E>(this, object);
        }
    }

    public E value() {
        return this.object;
    }

    @Override
    public String observerText() {
        return object.toString();
    }
    
    void change(E object) {
        this.object = object; 
    }

}
