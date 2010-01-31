/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/Move.java,v 1.4 2010/01/31 22:22:30 macfreek Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.model.ModelObject;

/**
 * @author Erik Vos
 */
public abstract class Move {

    /** Any ModelObjects that need be updated.
     * Will only be used by subclasses where it matters.
     */
    protected List<ModelObject> models = null;

    protected static Logger log =
            Logger.getLogger(Move.class.getPackage().getName());

    public abstract boolean execute();

    public abstract boolean undo();

    public void registerModelToUpdate (ModelObject model) {
        if (models == null) models = new ArrayList<ModelObject>(2);
        models.add(model);
    }

    // Could also be built into execute() and update()
    public void updateModels () {
        if (models != null) {
            for (ModelObject model : models) {
                model.update();
            }
        }
    }
}
