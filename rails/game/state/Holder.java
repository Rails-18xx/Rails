package rails.game.state;

public interface Holder<E extends Moveable> extends Item {

    /** Add an object to its list at a certain position.<br>
     * @param object The Moveable object to be added.
     * @param position Position to insert object at. O: at front, -1, at end, >0: at that position.
     * @return True if successful.
     */
    public boolean addObject(E object, int position);

    public boolean removeObject(E object);

    public int getListIndex(E object);

}
