package rails.game.state;
/**
 * Generic State wrapper
 * @author freystef
 *
 * @param <E> class to wrap
 */

public class GenericState<E> extends State {

    private E object;

    public GenericState(String id) {
        super(id);
        this.object = null;
    }

    public GenericState(String id, E object) {
        super(id);
        this.object = object;
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
