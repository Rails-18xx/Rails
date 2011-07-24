/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ViewObject.java,v 1.3 2008/06/04 19:00:39 evos Exp $*/
package rails.ui.swing.elements;

import rails.game.model.Model;
import rails.game.model.View;

public interface ViewObject extends View<String> {
    public Model<String> getModel();

    public void deRegister();

}
