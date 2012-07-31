package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class StateManager extends Manager implements DelayedItem {
    
    protected static Logger log =
        LoggerFactory.getLogger(StateManager.class.getPackage().getName());
    
    private final ChangeStack changeStack = ChangeStack.create(this);
    private final HashSetState<State> allStates = HashSetState.create(this, null);

    private final HashMultimapState<Observable, Observer> 
        observers = HashMultimapState.create(this, null);
    private final HashMultimapState<Observable, Model> 
        models = HashMultimapState.create(this, null);
    
    
    private final PortfolioManager portfolioManager = PortfolioManager.create(this, "Portfolios");
//  private final WalletManager walletManager = WalletManager.create(this, "walletManager");

    private StateManager(Root parent, String id) {
        super(parent, id);
    }

    static StateManager create(Root parent, String id){
        return new StateManager(parent, id);
    }
    /**
     * Register states (usually done automatically at state creation)
     */
    void registerState(State state) {
        allStates.add(state);
    }
    
    /**
     * De-Register states
     */
    boolean deRegisterState(State state) {
        return allStates.remove(state);
    }
    
    ImmutableSet<State> getAllStates() {
        return allStates.view();
    }

    /**
     * Adds the combination of observer to observable
     * @throws an IllegalArgumentException - if observer is already assigned to an observable
     */
    void addObserver(Observer observer, Observable observable) {
        checkArgument(!observers.containsValue(observer), "Observer can only be assigned to one Observable");
        observers.put(observable, observer);
    }
    
    boolean removeObserver(Observer observer, Observable observable) {
        return observers.remove(observable, observer);
    }
    
    public ImmutableSet<Observer> getObservers(Observable observable) {
        return observers.get(observable);
    }
    
    /**
     * Adds the combination of model to observable
     * @param Model the model that tracks the observable
     * @param Observable the observable to monitor
     */
    void addModel(Model model, Observable observable) {
        models.put(observable, model);
    }

    boolean removeModel(Model model, Observable observable) {
        return models.remove(observable, model);
    }
    
    public ImmutableSet<Model> getModels(Observable observable) {
        return models.get(observable);
    }

    /**
     * A set of states is given as input
     * and then calculates all observer to update in the correct sequence
     * 
     * It uses a topological sort based on DFS
     * 
     * @param states that have changed
     * @return sorted list of all models to be updated
     */
    private static enum Color {WHITE, GREY, BLACK};
    
    ImmutableList<Model> getModelsToUpdate(Collection<State> states) {
        // Topological sort
        // Initialize (we do not use WHITE explicitly, but implicit)
        Map<Observable, Color> colors = Maps.newHashMap();
        LinkedList<Model> topoList = Lists.newLinkedList();
        
        // For all states
        for (State s: states) {
            topoSort(s, colors, topoList);
        }
        log.debug("Observables to Update = " + topoList.toString());
        return ImmutableList.copyOf(topoList);
    }
    
    private void topoSort(Observable v, Map<Observable, Color> colors, LinkedList<Model> topoList) {
        colors.put(v, Color.GREY);
        for (Model m:v.getModels()) {
            if (!colors.containsKey(m)) {
                topoSort(m, colors, topoList);
            } else if (colors.get(m) == Color.GREY) {
                throw new IllegalStateException("Graph of Observables contains Cycle");
            }
        }
        colors.put(v, Color.BLACK);
        if (v instanceof Model) topoList.addFirst((Model)v);
    }
    
    
    void updateObservers(Set<State> states) {
        // all direct observers
        for (State s:states){
            for (Observer o:s.getObservers()) {
                o.update(s.observerText());
            }
        }
        
        // all indirect observers
        for (Model m:getModelsToUpdate(states)) {
            for (Observer o:m.getObservers()) {
                o.update(m.observerText());
            }
        }
    }
    
//    void registerReceiver(Triggerable receiver, State toState) {
//    }

    ChangeStack getChangeStack() {
        return changeStack;
    }
    
    PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }
    
}
