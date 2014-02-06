package net.sf.rails.game.state;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Model;
import net.sf.rails.game.state.State;
import net.sf.rails.game.state.StringState;

//An implementation only for testing
class ModelImpl extends Model {

    private final StringState text = StringState.create(this, "text");
    
    private ModelImpl(Item parent, String id, String text) {
        super(parent, id);
        this.text.set(text);
    }
    
    static ModelImpl create(Item parent, String id, String text) {
        return new ModelImpl(parent, id, text);
    }
    
    void changeText(String text) {
        this.text.set(text);
    }
    
    State getState() {
        return text;
    }
    
    @Override
    public String toText() {
        return text.value();
    }
}
