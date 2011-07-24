package rails.game.state;
/**
 * Generic State wrapper
 * @author freystef
 *
 * @param <E> class to wrap
 */

public final class GenericState<E> extends AbstractState {

    private E object;

    public GenericState(Item owner, String id) {
        super(owner, id);
        this.object = null;
    }

    public GenericState(Item owner, String id, E object) {
        super(owner, id);
        this.object = object;
    }

    private void set(E object, boolean forced) {
        if (object == null) {
            if (this.object != null) {
                new StateChange<E>(this, object);
            }
        } else if (!object.equals(this.object) || forced) {
                new StateChange<E>(this, object);
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
