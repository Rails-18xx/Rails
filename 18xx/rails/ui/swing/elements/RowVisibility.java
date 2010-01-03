package rails.ui.swing.elements;

import java.util.Observable;

import javax.swing.JComponent;

import rails.game.model.ModelObject;
import rails.game.state.BooleanState;

public class RowVisibility implements ViewObject {

    private RowHideable parent;
    private ModelObject modelObject;
    private int rowIndex;
    private boolean lastValue;
    
    public RowVisibility (RowHideable parent, int rowIndex, ModelObject model) {
        this.parent = parent;
        this.modelObject = model;
        this.rowIndex = rowIndex;
        modelObject.addObserver(this);
        lastValue = !((BooleanState)modelObject).booleanValue();
    }
    
    public boolean lastValue () {
        return lastValue;
    }
    
    /** Needed to satisfy the ViewObject interface. */
    public ModelObject getModel() {
        return modelObject;
    }
    
    /** Needed to satisfy the Observer interface. 
     * The closedObject model will send true if the company is closed. */
    public void update(Observable o1, Object o2) {
        if (o2 instanceof Boolean) {
            lastValue = !(Boolean)o2;
            parent.setRowVisibility(rowIndex, lastValue);
        }
    }

    /** Needed to satisfy the ViewObject interface. Currently not used. */
    public void deRegister() {
        if (modelObject != null)
            modelObject.deleteObserver(this);
    }
}
