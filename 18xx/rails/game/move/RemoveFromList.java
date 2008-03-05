/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/RemoveFromList.java,v 1.1 2008/03/05 19:55:14 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.List;

/**
 * @author Erik Vos
 */
public class RemoveFromList<E> extends Move {
    
    protected List<E> list;
    protected E object;
    protected String listName;
    protected int index;
    
    public RemoveFromList (List<E> list, E object, String listName) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        index = list.indexOf(object);
        
        MoveSet.add (this);
    }
    
    public boolean execute() {
       list.remove(object);
       return true;
    }

    public boolean undo() {
        list.add(index, object);
        return true;
    }
    
    public String toString() {
        return "RemoveFrom "+listName+": " + object.toString();
    }

}
