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

    public AddToList(List<E> list, E object, String listName, 
            ModelObject modelToUpdate) {
        this.object = object;
        this.list = list;
        this.listName = listName;
        if (modelToUpdate != null) registerModelToUpdate (modelToUpdate);

        MoveSet.add(this);
    }
    
    public AddToList(List<E> list, E object, String listName) {
        this (list, object, listName, null);
    }

    @Override
    public boolean execute() {
        list.add(object);
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
        return "AddToList " + listName + ": " + object.toString();
    }

}
