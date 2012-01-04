package rails.game.state;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import rails.game.GameManager;
import rails.game.model.Model;
import rails.game.model.Observer;

public final class StateManager extends AbstractItem {

    protected static Logger log =
        Logger.getLogger(StateManager.class.getPackage().getName());
    
    private final Set<State> allStates = new HashSet<State>();

    /**
     * Creates a StateManager (only possible for a root GameContext)
     * @param parent a root GameContext
     */
    StateManager(GameContext parent) {
        super("StateManager");

        if (parent.getId() != GameContext.ROOT) {
            throw new IllegalArgumentException("StateManager can only be created for a root GameContext");
        }
        // immediate initialization
        init(parent);
    }
    
    void registerState(State state) {
        allStates.add(state);
    }
    
    boolean deRegisterState(State state) {
        return allStates.remove(state);
    }

    /**
     * A set of states is given as input
     * and then calculates all observer to update in the correct sequence
     * 
     * It uses a topological sort algorithm (Kahn 1962)
     * 
     * @param states Set of states
     * @return sorted list of all observables (states and models)
     */
    List<Observable<?>> getSortedObservables(Set<State> states) {

        // 1: define all models
        Set<Model<?>> models = getModels(states);
        
        // 2: define graph
        Multimap<Model<?>, Observable<?>> edges = HashMultimap.create(); 
        
        // 2a: add edges that start from states
        for (State s:states) {
            for (Model<?> m:s.getModels()) {
                edges.put(m, s);
            }
        }
        
        // 2b: add edges that start from models
        for (Model<?> m1:models) {
            for (Model<?> m2:m1.getModels()) {
                edges.put(m2, m1);
            }
        }

        // 3: run topological sort
        List<Observable<?>> sortedList = Lists.newArrayList();
        List<Observable<?>> startNodes = Lists.newArrayList();
        startNodes.addAll(states);
        
        while (!startNodes.isEmpty()) {
            // remove node n
            Observable<?> n = startNodes.remove(0);
            // insert node into sortedList 
            sortedList.add(n);
            for (Model<?> m:n.getModels()) {
                edges.remove(m, n);
                // check if m is now a start node
                if (!edges.containsKey(m)) {
                    startNodes.add(m);
                }
            }
        }
        
        // if graph is not empty => cyclical graph
        if (!edges.isEmpty()) {
            log.debug("StateManager: Cyclical graph detected in State/Model relations.");
            // add remaining models to the end
            sortedList.addAll(edges.keySet());
        }
        
        return sortedList;
    }
    
    /**
     * @param states Set of states
     * @return all observers to be updated from states (either directly or via Models)
     */
    Set<Observer<?>> getObservers(Set<State> states){
        
        Set<Observer<?>> observers = Sets.newHashSet();
        
        // all direct observers
        for (State s:states){
            observers.addAll(s.getObservers());
        }
        
        // all indirect observers
        for (Model<?> m:getModels(states)){
            observers.addAll(m.getObservers());
        }
        
        return observers;
    }
    
    /**
     * @param states Set of states
     * @return all models to be updated from states
     */
    Set<Model<?>> getModels(Set<State> states) {
        
        Set<Model<?>> allModels = Sets.newHashSet();
        
        // add all models updated from states directly
        for (State s:states) {
            allModels.addAll(s.getModels());
        }
        
        // then add models called indirectly
        ImmutableSet<Model<?>> checkModels = ImmutableSet.copyOf(allModels);
        Set<Model<?>> newModels = Sets.newHashSet();
        while (!checkModels.isEmpty()) {
            for (Model<?> m1:checkModels) {
                for (Model<?> m2:m1.getModels()) {
                    if (!allModels.contains(m2)) {
                        allModels.add(m2);
                        newModels.add(m2);
                    }
                }
            }
            checkModels = ImmutableSet.copyOf(newModels);
            newModels.clear();
        }
        return allModels;
    }
    
    
//    void registerReceiver(Triggerable receiver, State toState) {
//    }

    
    public static StateManager getInstance() {
        return GameManager.getInstance().getStateManager();
    }
    
}
