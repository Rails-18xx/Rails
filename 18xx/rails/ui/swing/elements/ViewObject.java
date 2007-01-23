package rails.ui.swing.elements;


import java.util.Observer;

import rails.game.model.ModelObject;

public interface ViewObject extends Observer
{
	public ModelObject getModel();

	public void deRegister();

}
