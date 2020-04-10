package net.sf.rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class StateManager extends Manager{

    private static final Logger log = LoggerFactory.getLogger(StateManager.class);

    private final ChangeStack changeStack;

    private final HashSetState<State> allStates = HashSetState.create(this, "allStates");
    private final HashMultimapState<Observable, Model> models = HashMultimapState.create(this, "models");
    private final HashMultimapState<Observable, Triggerable> triggers = HashMultimapState.create(this, "triggers");


    // observers is not a state variable (as the have to register and de-register themselves)
    // gui eleemnts do not have a state of their own (with respect to the game engine)
    private final HashMultimap<Observable, Observer> observers =
            HashMultimap.create();

    // initialized later in init()
    private PortfolioManager portfolioManager;
    private WalletManager walletManager;

    private StateManager(Root parent, String id) {
        super(parent, id);
        this.changeStack = ChangeStack.create(this);
    }

    static StateManager create(Root parent, String id){
        return new StateManager(parent, id);
    }

    void init() {
        // manually register embedded states
        registerState(allStates);
        registerState(models);
        // create managers
        portfolioManager = PortfolioManager.create(this, "Portfolios");
        walletManager = WalletManager.create(this, "walletManager");
    }

    /**
     * Register states (usually called automatically at state creation)
     */
    void registerState(State state) {
        allStates.add(state);
    }

//    /**
//     * De-Register states
//     */
//    TODO: Add this
//    boolean deRegisterState(State state) {
//        return allStates.remove(state);
//    }

    /**
     * set of all states stored in the StateManager
     */
    ImmutableSet<State> getAllStates() {
        return allStates.view();
    }

    /**
     * Adds the combination of observer to observable
     * Usually this is one via addObserver of the observable
     * @throws an IllegalArgumentException - if observer is already assigned to an observable
     */
    synchronized void addObserver(Observer observer, Observable observable) {
        checkArgument(!observers.containsValue(observer), "Observer can only be assigned to one Observable");
        observers.put(observable, observer);
    }

    /**
     * Remove combination of observer to observable
     */
    boolean removeObserver(Observer observer, Observable observable) {
        return observers.remove(observable, observer);
    }

    /**
     * Set of all observers that observe the observable
     */
    ImmutableSet<Observer> getObservers(Observable observable) {
        return ImmutableSet.copyOf(observers.get(observable));
    }

    /**
     * Adds the combination of model to observable
     * @param Model the model that is updated by the observable
     * @param Observable the observable to monitor
     */
    void addModel(Model model, Observable observable) {
        models.put(observable, model);
    }

    boolean removeModel(Model model, Observable observable) {
        return models.remove(observable, model);
    }

    ImmutableSet<Model> getModels(Observable observable) {
        return models.get(observable);
    }

    /**
     * Adds the combination of trigger to observable
     * @param Triggerable the trigger that tracks the observable
     * @param Observable the observable to monitor
     */
    void addTrigger(Triggerable trigger, Observable observable) {
        triggers.put(observable, trigger);
    }

    boolean removeTrigger(Triggerable trigger, Observable observable) {
        return triggers.remove(observable, trigger);
    }

    ImmutableSet<Triggerable> getTriggers(Observable observable) {
        return triggers.get(observable);
    }

    void informTriggers(State state, Change change) {

        // Inform direct triggers
        for (Triggerable t:getTriggers(state)) {
            t.triggered(state, change);
            log.debug("State {} sends change to Trigger {}", state, t);
        }

        // check if there are models
        ImmutableSet<Model> initModels = getModels(state);
        if (initModels.isEmpty()) return;
        ImmutableList<Model> allModels = getModelsToUpdate(initModels);

        // Inform indirect triggers
        for (Model m:allModels) {
            for (Triggerable t:getTriggers(m)) {
                t.triggered(m, change);
                log.debug("Model {} sends change to Trigger {}", m, t);
            }
        }
    }

    /**
     * A set of observables is given as input
     * and then calculates all observer to update in the correct sequence
     *
     * It uses a topological sort based on DFS
     *
     * @param observables that have been updated
     * @return sorted list of all models to be updated
     */
    ImmutableList<Model> getModelsToUpdate(Collection<? extends Observable> observables) {
        // Topological sort
        // Initialize (we do not use WHITE explicitly, but implicit)
        final Map<Observable, Color> colors = Maps.newHashMap();
        final LinkedList<Model> topoList = Lists.newLinkedList();

        // For all states
        for (Observable s: observables) {
            topoSort(s, colors, topoList);
        }
        return ImmutableList.copyOf(topoList);
    }

    private static enum Color {WHITE, GREY, BLACK};
    private void topoSort(final Observable v, final Map<Observable, Color> colors, final LinkedList<Model> topoList) {
        colors.put(v, Color.GREY);
        for (Model m:getModels(v)) {
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
            Set<Observer> observers = getObservers(s);
            if (observers.isEmpty()) continue;
            // cache StateText
            String stateText = s.toText();
            for (Observer o:observers) {
                o.update(stateText);
                log.debug("State {} updates observer {}", s, o);
            }
        }

        // all indirect observers
        for (Model m:getModelsToUpdate(states)) {
            Set<Observer> observers = getObservers(m);
            if (observers.isEmpty()) continue;
            // cache ModelText
            String modelText = m.toText();
            for (Observer o:observers) {
                o.update(modelText);
                log.debug("Model {} updates observer {}", m, o);
            }
        }
    }

    // StateManager getters for sub-components
    //////////////////////////////////////////

    public ChangeStack getChangeStack() {
        return changeStack;
    }

    PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }

    public WalletManager getWalletManager() {
        return walletManager;
    }

}
