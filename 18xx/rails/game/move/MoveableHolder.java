package rails.game.move;


public interface MoveableHolder {

    /** Add an object to its list at a certain position.<br>
     * @param object The Moveable object to be added.
     * @param position Position to insert object at. O: at front, -1, at end, >0: at that position.
     * @return True if successful.
     */
    public boolean addObject(Moveable object, int[] position);

    public boolean removeObject(Moveable object);

    public String getName();

    public int[] getListIndex (Moveable object);

}
