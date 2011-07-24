package rails.game.state;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import rails.game.model.View;

abstract class AbstractState implements State {
    
    private final String id;
    private final GameContext root;
    
    private final List<View<String>> views = new ArrayList<View<String>>();
    private final Formatter formatter;
    
    public AbstractState(Item owner, String id) {
        this(owner, id, null);
    }
    
    public AbstractState(Item owner, String id, Formatter formatter) {
        
        if (owner.getId() != null) {
           this.id = owner.getId() + "." + id;
        } else {
           this.id = id;
        }
        
        // define formatter
        this.formatter = formatter;

        // pass along the root
        if (owner.getRoot() instanceof GameContext) {
            this.root = (GameContext) owner.getRoot();
        } else {
            throw new InvalidParameterException("Invalid owner: States can only be created inside a GameContext hierachy");
        }
        
        // add to StateManager
        root.getStateManager().registerState(this);
    }
    
    // methods for item
    public String getId() {
        return id;
    }

    public Context getRoot() {
        return root;
    }
    
    
    // methods for model
    public void addView(View<String> view) {
        views.add(view);
    }
    
    public void removeView(View<String> view) {
        views.remove(view);
    }
    
    public void notifyModel() {
        String data = getData();
        for (View<String> view:views) {
            view.update(data);
        }
    }
    
    public String getData() {
        if (formatter == null) {
            return toString();
        } else {
            return formatter.formatData(this);
        }
    }
    
    
    // methods for state
    public void addModel(Notifiable model) {
        root.getStateManager().registerModel(this, model);
    }
    
    public void addReceiver(Triggerable receiver) {
        root.getStateManager().registerReceiver(this, receiver);
    }
    
}
