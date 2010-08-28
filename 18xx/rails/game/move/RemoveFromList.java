/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/RemoveFromList.java,v 1.3 2009/09/23 21:38:57 evos Exp $
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
public class RemoveFromList<E> extends Move {

    protected List<E> list;
    protected E object;
    protected String listName;
    protected int index;

    public RemoveFromList(List<E> list, E object, String listName,
            ModelObject modelToUpdate) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        index = list.indexOf(object);
        if (modelToUpdate != null) registerModelToUpdate (modelToUpdate);

        MoveSet.add(this);
    }

    public RemoveFromList(List<E> list, E object, String listName) {
        this (list, object, listName, null);
    }

    @Override
    public boolean execute() {
        list.remove(object);
        updateModels();
        return true;
    }

    @Override
    public boolean undo() {
        list.add(index, object);
        updateModels();
        return true;
    }

    @Override
    public String toString() {
        return "RemoveFromList " + listName + ": " + object.toString();
    }

}
