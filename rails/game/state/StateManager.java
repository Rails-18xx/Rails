package rails.game.state;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import rails.game.GameManager;
import rails.game.model.Observer;

// TODO: Add a reference counting to allow proper de-register
public final class StateManager extends AbstractItem {

    protected static Logger log =
        Logger.getLogger(StateManager.class.getPackage().getName());
    

    // private structure that combines observer and state
    private final static class StateObserverKey {
        private final State state;
        private final Observer observer;
        
        private StateObserverKey(State state, Observer observer) {
            this.state = state;
            this.observer = observer;
        }
        
        @Override
        public int hashCode() {
            return state.hashCode() * 31 + observer.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StateObserverKey)) return false;
            return ((StateObserverKey)obj).state.equals(this.state) && 
                ((StateObserverKey)obj).observer.equals(this.observer);
        }
        
    }
    
    private final ArrayListState<State> states;
    private final HashMultimapState<State, Observer> stateToObservers;
    private final HashMultimapState<Observer, State> observerToStates;
    private final HashMapState<StateObserverKey, Integer> stateObserverPriority;

    public StateManager() {
        states = new ArrayListState<State>(this, "states");
        stateToObservers = new HashMultimapState<State, Observer>(this, "stateToObservers");
        observerToStates = new HashMultimapState<Observer, State>(this, "observerToStates");
        stateObserverPriority = new HashMapState<StateObserverKey, Integer>(this, "stateObserverPriority");
    }
    
    void registerState(State state) {
        states.add(state);
    }
    
    void deRegisterState(State state) {
        states.remove(state);
        // TODO: Write the remaining code
    }
    
    private void registerObserverToState(Observer observer, State toState, int priority) {
        // first add the combination (the state object checks if it is already defined)
        stateToObservers.put(toState, observer);
        observerToStates.put(observer, toState);
        
        // then get the previous priority and set it to the lower of the twos
        StateObserverKey key = new StateObserverKey(toState, observer);
        int oldPriority = stateObserverPriority.get(key);
        if (priority != oldPriority) {
            stateObserverPriority.put(key, Math.min(priority, oldPriority));
        }
    }

    /**
     * register observer to a state
     */
    void registerObserver(Observer observer, State toState) {
        // never allow to register a State
        if (observer instanceof State) {
            throw new IllegalArgumentException("A State object should never be registered as observer");
        }
        registerObserverToState(observer, toState, 0);
    }
    
    /**
     * register observer to another observer
     */
    public void registerObserver(Observer observer, Observer toObserver) {
        // never allow to register a State
        if (observer instanceof State) {
            throw new IllegalArgumentException("A State object should never be registered as observer");
        }
        
        // register to the same states as the observed observer with decreased priority
        for (State state:observerToStates.get(toObserver)) {
            StateObserverKey key = new StateObserverKey(state, toObserver);
            registerObserverToState(observer, state, stateObserverPriority.get(key) + 1);
        }
    }
    
    
    public void deRegisterObserver(Observer observer) {
        // check if the observer listens to any state and remove the priority and the reverse relation
        for (State state:observerToStates.removeAll(observer)) {
            StateObserverKey key = new StateObserverKey(state, observer);
            stateObserverPriority.remove(key);
            stateToObservers.remove(state, observer);
        }
    }

    /**
     * A set of states is given as input (usually from the ChangeStack)
     * and then calculates all observer to update in the correct sequence
     * due to the defined priority
     */
    void updateObservers(Set<State> states) {
        Map<Observer, Integer> updateObservers = Maps.newHashMap();
        
        // build the map between observers and priority
        for (State state: states) {
            for (Observer observer:stateToObservers.get(state)) {
                if (updateObservers.containsKey(observer)) {
                    int newPriority = stateObserverPriority.get(new StateObserverKey(state, observer));
                    int oldPriority = updateObservers.get(observer);
                    updateObservers.put(observer, Math.min(newPriority, oldPriority));
                }
            }
        }
        // then sort all observers based on the priority
        Ordering<Observer> priorityOrder = Ordering.natural().onResultOf(Functions.forMap(updateObservers));
        for (Observer observer:ImmutableSortedMap.copyOf(updateObservers, priorityOrder).keySet()) {
            observer.update();
        }
            
    }
    
    void registerReceiver(Triggerable receiver, State toState) {
    }

    
    public static StateManager getInstance() {
        return GameManager.getInstance().getStateManager();
    }
        
    
    
    
}
