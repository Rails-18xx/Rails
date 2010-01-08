/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/ObjectMove.java,v 1.7 2010/01/08 21:31:00 evos Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

/**
 * @author Erik Vos
 */
public class ObjectMove extends Move {

    Moveable moveableObject;
    MoveableHolder from;
    MoveableHolder to;
    String objectClassName;

    /**
     * Create a generic ObjectMove object. Any specific side effects must be
     * implemented in the addToken and removeToken methods of the 'from' and
     * 'to' TokenHolders. <p>The parameter descriptions cover the usual case of
     * a Base Token lay, which is physically removed from a PublicCompany and
     * added to a Station on a MapHex.
     *
     * @param moveableObject The moveableObject to be moved (e.g. a BaseToken).
     * @param from Where the moveableObject is removed from (e.g. a
     * PublicCompany charter).
     * @param to Where the moveableObject is moved to (e.g. a MapHex).
     */

    public ObjectMove(Moveable moveableObject, MoveableHolder from,
            MoveableHolder to) {

        this.moveableObject = moveableObject;
        this.from = from;
        this.to = to;
        objectClassName = moveableObject.getClass().getSimpleName();

        MoveSet.add(this);
    }

    @Override
	public boolean execute() {

    	return (from == null || from.removeObject(moveableObject))
               && to.addObject(moveableObject);
    }

    @Override
	public boolean undo() {

        return to.removeObject(moveableObject)
               && (from == null || from.addObject(moveableObject));
    }

    @Override
	public String toString() {
        if (moveableObject == null) log.error("Token is null");
        if (from == null) log.warn("From is null");
        if (to == null) log.error("To is null");
        return "Move " + objectClassName + ": " + moveableObject.getName()
               + " from " + (from == null ? from : from.getName()) + " to "
               + to.getName();
    }

}
