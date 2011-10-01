package rails.game.state;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rails.game.GameManager;
import rails.game.model.Observer;

// FIXME: Add the missing mechanism parts
// TODO: Add a reference counting to allow proper de-register

public final class StateManager extends AbstractItem {

    protected static Logger log =
        Logger.getLogger(StateManager.class.getPackage().getName());
    
    
    // private structure to combine observer with its priority
    private class ObsPriority{
        private final Observer observer;
        private final int priority;
        
        private ObsPriority(Observer observer, int priority) {
            this.observer = observer;
            this.priority = priority;
        }
    }

    // private structure to combine observer with its priority - and for the reverse lookup
    private class StatePriority{
        private final State state;
        private final int priority;
        
        private StatePriority(State state, int priority) {
            this.state = state;
            this.priority = priority;
            
        }
    }
    
    final private ArrayList<State> states = new ArrayList<State>();
    final private HashMultimapState<State, ObsPriority> stateToObservers;
    final private HashMultimapState<Observer, StatePriority> observerToStates;

    public StateManager() {
        stateToObservers = new HashMultimapState<State, ObsPriority>(this, "stateToObservers");
        observerToStates = new HashMultimapState<Observer, StatePriority>(this, "observerToStates");
    }
    
    void registerState(State state) {
        states.add(state);
        log.debug("StateManager: Registered state " + state.getId());
    }
    
    private void registerObserver(Observer observer, State toState, int priority) {
        // never allow to register a State
        if (observer instanceof State) {
            throw new AssertionError("A State object should never be registered as observer");
        }
        stateToObservers.put(toState, new ObsPriority(observer, priority));
        observerToStates.put(observer, new StatePriority(toState, priority));
    }

    /**
     * register observer to a state
     */
    void registerObserver(Observer observer, State toState) {
        registerObserver(observer, toState, 0);
    }
    
    /**
     * register observer to another observer
     */
    public void registerObserver(Observer observer, Observer toObserver) {
        // register to the same states as the observed observer with decreased priority
        for (StatePriority statePriority:observerToStates.get(observer)) {
            registerObserver(observer, statePriority.state, statePriority.priority + 1);
        }
    }
    
    /**
     * de-register observer to another observer
     */
    public void deRegisterObserver(Observer observer, Observer toObserver) {
        // register to the same states as the observed observer with decreased priority
        for (StatePriority statePriority:observerToStates.get(observer)) {
            registerObserver(observer, statePriority.state, statePriority.priority + 1);
        }
    }
    

    public void updateObservers() {
        // TODO: write that code
    }
    
    void registerReceiver(Triggerable receiver, State toState) {
    }

    
    public static StateManager getInstance() {
        return GameManager.getInstance().getStateManager();
    }
        
    
    
    
}
