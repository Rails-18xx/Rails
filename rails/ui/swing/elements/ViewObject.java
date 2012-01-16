package rails.ui.swing.elements;

import rails.game.model.Model;
import rails.game.state.Observer;

// FIXME: This has to be rewritten based on the new objects
@Deprecated
public interface ViewObject extends Observer {
    public Model<String> getModel();

    public void deRegister();

}
