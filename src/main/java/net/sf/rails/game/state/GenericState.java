package net.sf.rails.game.state;

/**
 * Generic State wrapper
 *
 * @param <E> class to wrap
 * @author freystef
 */

public final class GenericState<E> extends State {

    private E object;

    /**
     * Creates a prefilled GenericState
     *
     * @param parent The parent
     * @param id     The id
     * @param object The content
     */
    public GenericState(Item parent, String id, E object) {
        super(parent, id);

        this.object = object;
    }

    /**
     * Creates an empty GenericState
     *
     * @param parent The parent
     * @param id     The id
     */
    public GenericState(Item parent, String id) {
        this(parent, id, null);
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

    /**
     * For observable objects it returns toText(), for others toString()
     * If GenericState is set to null returns empty string
     */
    @Override
    public String toText() {
        if (object == null) {
            return "";
        } else if (object instanceof Observable) {
            return ((Observable) object).toText();
        } else {
            return object.toString();
        }
    }

    void change(E object) {
        this.object = object;
    }

}
