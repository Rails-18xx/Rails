package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public final class StateManager extends Manager {
    
    protected static Logger log =
        LoggerFactory.getLogger(StateManager.class.getPackage().getName());
    
    private final ChangeStack changeStack = ChangeStack.create(this);
    private final HashSetState<State> allStates = HashSetState.create(this, null);

    private final HashMultimapState<Observable, Observer> 
        observers = HashMultimapState.create(this, null);
    private final HashMapState<Observer, Formatter<? extends Observable>> 
        formatters = HashMapState.create(this, null);
    private final HashMultimapState<Observable, Model> 
        models = HashMultimapState.create(this, null);
    
    
    private final PortfolioManager portfolioManager = PortfolioManager.create(this, null);
//  private final WalletManager walletManager = WalletManager.create(this, "walletManager");

    private StateManager(Root parent, String id) {
        super(parent, id);
    }

    static StateManager create(Root parent, String id){
        return new StateManager(parent, id);
    }
    /**
     * Register states 
     * Remark: Portfolios and Wallets get added from their respective managers automatically
     */
    void registerState(State state) {
        allStates.add(state);
//        if (state instanceof Portfolio) {
//            return portfolioManager.addPortfolio((Portfolio<?>) state);
//        } else if (state instanceof Wallet) {
//            return walletManager.addWallet((Wallet<?>) state);
//        }
    }
    
    /**
     * De-Register states 
     * Remark: Portfolios and Wallets are removed from their respective managers automatically
     */
    boolean deRegisterState(State state) {
        if (!allStates.remove(state)) return false;
//        if (state instanceof PortfolioMap) {
//            return portfolioManager.removePortfolio((PortfolioMap<?>) state);
//        } else if (state instanceof Wallet) {
//            return walletManager.removeWallet((Wallet<?>) state);
//        }
        return true;
    }

    /**
     * Adds the combination of observer to observable
     * @throws an IllegalArgumentException - if observer is already assigned to an observable
     */
    void addObserver(Observer observer, Observable observable) {
        checkArgument(!observers.containsValue(observer), "Observer can only be assigned to one Observable");
        observers.put(observable, observer);
    }
    
    /**
     * Adds the combination of observer to observable, using a Formatter
     * @throws an IllegalArgumentException - if observer is already assigned to an observable
     */
    <T extends Observable>  void addObserver(Observer observer, Formatter<T> formatter) {
        this.addObserver(observer, formatter.getObservable());
        formatters.put(observer, formatter);
    }
    
    boolean removeObserver(Observer observer, Observable observable) {
        formatters.remove(observer);
        return observers.remove(observable, observer);
    }
    
    public ImmutableSet<Observer> getObservers(Observable observable) {
        return observers.get(observable);
    }
    
    /**
     * Adds the combination of model to observable
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
     * It uses a topological sort algorithm (Kahn 1962)
     * 
     * @param states Set of states
     * @return sorted list of all observables (states and models)
     */
    List<Observable> getSortedObservables(Set<State> states) {

        // 1: define all models
        Set<Model> models = getModels(states);
        
        // 2: define graph
        Multimap<Model, Observable> edges = HashMultimap.create(); 
        
        // 2a: add edges that start from states
        for (State s:states) {
            for (Model m:s.getModels()) {
                edges.put(m, s);
            }
        }
        
        // 2b: add edges that start from models
        for (Model m1:models) {
            for (Model m2:m1.getModels()) {
                edges.put(m2, m1);
            }
        }

        // 3: run topological sort
        List<Observable> sortedList = Lists.newArrayList();
        List<Observable> startNodes = Lists.newArrayList();
        startNodes.addAll(states);
        
        while (!startNodes.isEmpty()) {
            // remove node n
            Observable n = startNodes.remove(0);
            // insert node into sortedList 
            sortedList.add(n);
            for (Model m:n.getModels()) {
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
    private Set<Observer> getObservers(Set<State> states){
        
        Set<Observer> observers = Sets.newHashSet();
        
        // all direct observers
        for (State s:states){
            observers.addAll(s.getObservers());
        }
        
        // all indirect observers
        for (Model m:getModels(states)){
            observers.addAll(m.getObservers());
        }
        
        return observers;
    }
    
    void updateObservers(Set<State> states) {
        for (Observable observable:getSortedObservables(states)) {
            for (Observer observer:observable.getObservers()) {
                // check if formatter is defined
                if (formatters.containsKey(observer)) {
                    observer.update(formatters.get(observer).observerText());
                } else {
                    // otherwise use observable text
                    observer.update(observable.observerText());
                }
            }
        }
    }
    
    /**
     * @param states Set of states
     * @return all models to be updated from states
     */
    Set<Model> getModels(Set<State> states) {
        
        Set<Model> allModels = Sets.newHashSet();
        
        // add all models updated from states directly
        for (State s:states) {
            allModels.addAll(s.getModels());
        }
        
        // then add models called indirectly
        ImmutableSet<Model> checkModels = ImmutableSet.copyOf(allModels);
        Set<Model> newModels = Sets.newHashSet();
        while (!checkModels.isEmpty()) {
            for (Model m1:checkModels) {
                for (Model m2:m1.getModels()) {
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

    ChangeStack getChangeStack() {
        return changeStack;
    }
    
    PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }
    
}
