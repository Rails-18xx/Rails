package ui.elements;

import game.model.ModelObject;

import java.util.Observer;

public interface ViewObject extends Observer
{
	public ModelObject getModel();

	public void deRegister();

}
