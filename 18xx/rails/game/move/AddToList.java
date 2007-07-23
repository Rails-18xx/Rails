/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/AddToList.java,v 1.1 2007/07/23 19:59:16 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.List;

/**
 * @author Erik Vos
 */
public class AddToList<E> extends Move {
    
    protected List<E> list;
    protected E object;
    protected String listName;
    
    public AddToList (List<E> list, E object, String listName) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        
        MoveSet.add (this);
    }
    
    public boolean execute() {
       list.add(object);
       return true;
    }

    public boolean undo() {
        list.remove(list.size()-1);
        return true;
    }
    
    public String toString() {
        return "AddTo "+listName+": " + object.toString();
    }

}
