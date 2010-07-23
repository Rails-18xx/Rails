/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/ObjectMove.java,v 1.8 2010/01/31 22:22:30 macfreek Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.util.Util;

/**
 * @author Erik Vos
 */
public class ObjectMove extends Move {

    Moveable moveableObject;
    MoveableHolder from;
    MoveableHolder to;
    String objectClassName;
    int[] fromPosition;
    int[] toPosition = new int[] {-1}; // Default: at end

    /**
     * Create a generic ObjectMove object. Any specific side effects must be
     * implemented in the various addXxxx and removeXxxx methods of the 'from' and
     * 'to' MoveableHolders, depending on the object type.
     *
     * @param moveableObject The object to be moved (e.g. a BaseToken).
     * @param from Where the moveableObject is removed from (e.g. a PublicCompany charter).
     * @param to Where the moveableObject is moved to (e.g. a MapHex).
     * It is moved to the end of the List in which the moved object type is stored.
     */

    public ObjectMove(Moveable moveableObject, MoveableHolder from,
            MoveableHolder to) {
        this (moveableObject, from, to, null);
    }

    /**
     * Create a generic ObjectMove object. Any specific side effects must be
     * implemented in the various addXxxx and removeXxxx methods of the 'from' and
     * 'to' MoveableHolders, depending on the object type.
     *
     * @param moveableObject The object to be moved (e.g. a BaseToken).
     * @param from Where the moveableObject is removed from (e.g. a PublicCompany charter).
     * @param to Where the moveableObject is moved to (e.g. a MapHex).
     * @param toPosition At which List index in the 'to' holder the object must be inserted,
     * -1 means at the end.
     */

    public ObjectMove(Moveable moveableObject, MoveableHolder from,
            MoveableHolder to, int[] toPosition) {

        this.moveableObject = moveableObject;
        this.from = from;
        this.to = to;
        objectClassName = moveableObject.getClass().getSimpleName();
        this.fromPosition = from != null ? from.getListIndex(moveableObject) : null;
        this.toPosition = toPosition;

        MoveSet.add(this);
    }

    @Override
    public boolean execute() {

        return (from == null || from.removeObject(moveableObject))
        && to.addObject(moveableObject, toPosition);
    }

    @Override
    public boolean undo() {

        return to.removeObject(moveableObject)
        && (from == null || from.addObject(moveableObject, fromPosition));
    }

    @Override
    public String toString() {
        if (moveableObject == null) log.error("Token is null");
        if (from == null) log.warn("From is null");
        if (to == null) log.error("To is null");
        StringBuilder buf = new StringBuilder();

        buf.append("Move ").append(objectClassName).append(": ").append(moveableObject.getName())
            .append(" from ").append(from == null ? from : from.getName());
        if (fromPosition != null) {
            buf.append("[").append(Util.joinWithDelimiter(fromPosition, ",")).append("]");
        }
        buf.append(" to ").append(to == null ? to : to.getName());
        if (toPosition != null) {
            buf.append("[").append(Util.joinWithDelimiter(toPosition, ",")).append("]");
        }
        return buf.toString();
    }

}
