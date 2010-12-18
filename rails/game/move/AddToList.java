/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/AddToList.java,v 1.5 2010/01/31 22:22:29 macfreek Exp $
 *
 * Created on 18-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.List;

import rails.game.model.ModelObject;

/**
 * @author Erik Vos
 */
public class AddToList<E> extends Move {

    protected List<E> list;
    protected E object;
    protected String listName;
    protected Integer index; // if supplied insert at index position

    public AddToList(List<E> list, E object, String listName,
            ModelObject modelToUpdate) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        if (modelToUpdate != null) registerModelToUpdate (modelToUpdate);
        this.index = null;

        MoveSet.add(this);
    }

    public AddToList(List<E> list, E object, String listName) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        this.index = null;

        MoveSet.add(this);
    }

    public AddToList(List<E> list, E object, int index, String listName) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        this.index = index;

        MoveSet.add(this);
   }

    @Override
    public boolean execute() {
        if (index == null) {
            list.add(object);
        } else {
            list.add(index, object);
        }
        updateModels();
        return true;
    }

    @Override
    public boolean undo() {
        list.remove(object);
        updateModels();
        return true;
    }

    @Override
    public String toString() {
        if (index == null)
            return "AddToList " + listName + ": " + object.toString();
        else
            return "AddToList " + listName + ": " + object.toString() + " at index " + index;
    }

}
