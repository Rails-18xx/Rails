/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/ViewObject.java,v 1.1 2005/12/11 00:03:37 evos Exp $
 * 
 * Created on 08-Dec-2005
 * Change Log:
 */
package ui.elements;

import game.model.ModelObject;

import java.util.Observer;

/**
 * @author Erik Vos
 */
public interface ViewObject extends Observer {
    
    public ModelObject getModel();
    
    public void deRegister();

}
