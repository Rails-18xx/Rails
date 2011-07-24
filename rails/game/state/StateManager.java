package rails.game.state;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public final class StateManager {

    protected static Logger log =
        Logger.getLogger(StateManager.class.getPackage().getName());
    
    final private ArrayList<State> states = new ArrayList<State>();
    final private Multimap<State, Triggerable> receivers = HashMultimap.create();
    final private Multimap<State, Notifiable> updates = HashMultimap.create();
    
    void registerState(State state) {
        states.add(state);
        log.debug("StateManager: Registered state " + state.getId());
    }
    
    void registerModel(State state, Notifiable model) {
        updates.put(state, model);
    }
    
    void registerReceiver(State state, Triggerable receiver) {
        receivers.put(state, receiver);
    }
    
    
}
