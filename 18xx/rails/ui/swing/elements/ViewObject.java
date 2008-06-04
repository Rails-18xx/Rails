/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ViewObject.java,v 1.3 2008/06/04 19:00:39 evos Exp $*/
package rails.ui.swing.elements;

import java.util.Observer;

import rails.game.model.ModelObject;

public interface ViewObject extends Observer {
    public ModelObject getModel();

    public void deRegister();

}
