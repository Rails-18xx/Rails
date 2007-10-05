/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ViewObject.java,v 1.2 2007/10/05 22:02:30 evos Exp $*/
package rails.ui.swing.elements;


import java.util.Observer;

import rails.game.model.ModelObject;

public interface ViewObject extends Observer
{
	public ModelObject getModel();

	public void deRegister();

}
