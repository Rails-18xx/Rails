package rails.game.state;

import java.security.InvalidParameterException;
import rails.game.model.Model;

/**
 * State is an abstract generic class
 * that defines the base layer of objects that contain game state.
 * 
 * All State(s) are Item(s) themselves.
 * 
 * It allows to add a Formatter to change the String output dynamically.
 * 
 * @author freystef
 *
 */
public abstract class State extends Observable {
    
    // optional formatter
    private Formatter<State> formatter = null;

    protected State(String id){
        super(id);
    }
    
    /**
     * Remark: If the parent of the state is a model, the model is registered automatically
     */
    @Override
    public State init(Item parent) {
        // set parent using AbstractItem.init() method
        super.init(parent);
        
        // check if the parent's context is a GameContext
        if (parent.getContext() instanceof Context) {
            // register state
            ((Context)parent.getContext()).getStateManager().registerState(this); 
        } else {
            throw new InvalidParameterException("Invalid parent: States can only be created inside a GameContext hierachy");
        }
        
        // check if parent is a model
        if (parent instanceof Model) {
            addModel((Model)parent);
        }
        
        return this;
    }

    /*
    * Replaces the standard getContext() method:
     * @return GameContext object where the State object exists in
     */
    public Context getContext() {
        return (Context)super.getContext();
    }

    /**
     * Adds a Formatter 
     * @param formatter
     */
    public void setFormatter(Formatter<State> formatter) {
        this.formatter = formatter;
    }
    
    /**
     * For a state getText() it is identical to toString()
     * If this does not work use a formatter
     */
    @Override
    public final String getText() {
        if (formatter == null) {
            return toString();
        } else {
            return formatter.formatValue(this);
        }
    }
    
}
