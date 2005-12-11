/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/ModelObject.java,v 1.1 2005/12/11 00:03:36 evos Exp $
 * 
 * Created on 08-Dec-2005
 * Change Log:
 */
package game.model;

import java.util.Observable;

/**
 * @author Erik Vos
 */
public abstract class ModelObject extends Observable {
    
    protected void notifyViewObjects() {
        setChanged();
        notifyObservers (toString());
        clearChanged();
    }

}
