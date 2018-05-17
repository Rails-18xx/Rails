using GameLib.Net.Common;
using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;

namespace GameLib.Net.Game.State
{
    public class StateManager : Manager
    {
        protected Logger<StateManager> log = new Logger<StateManager>();
    
    private readonly ChangeStack changeStack;

        private readonly HashSetState<GameState> allStates;
        private readonly HashMultimapState<Observable, Model> models;
        private readonly HashMultimapState<Observable, ITriggerable> triggers;


        // observers is not a state variable (as the have to register and de-register themselves)
        // gui elements do not have a state of their own (with respect to the game engine)
        private readonly Wintellect.PowerCollections.MultiDictionary<Observable, IObserver> observers;

        // initialized later in init()
        private PortfolioManager portfolioManager;
        private WalletManager walletManager;

        private StateManager(Root parent, string id) : base(parent, id)
        {
            this.changeStack = ChangeStack.Create(this);
            allStates = HashSetState<GameState>.Create(this, "allStates");
            models =    HashMultimapState<Observable, Model>.Create(this, "models");
            triggers =  HashMultimapState<Observable, ITriggerable>.Create(this, "triggers");
            observers = new Wintellect.PowerCollections.MultiDictionary<Observable, IObserver>(false);
        }

        public static StateManager Create(Root parent, String id)
        {
            return new StateManager(parent, id);
        }

        public void Init()
        {
            // manually register embedded states
            RegisterState(allStates);
            RegisterState(models);
            // create managers
            portfolioManager = PortfolioManager.Create(this, "Portfolios");
            walletManager = WalletManager.Create(this, "walletManager");
        }

        /**
         * Register states (usually called automatically at state creation)
         */
        public void RegisterState(GameState state)
        {
            allStates.Add(state);
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
        public IReadOnlyCollection<GameState> GetAllStates()
        {
            return allStates.View();
        }

        /**
         * Adds the combination of observer to observable
         * Usually this is one via addObserver of the observable
         * @throws an IllegalArgumentException - if observer is already assigned to an observable
         */
        public void AddObserver(IObserver observer, Observable observable)
        {
            Precondition.CheckArgument(!observers.ContainsValue(observer), "Observer can only be assigned to one Observable");
            observers.Add(observable, observer);
        }

        /** 
         * Remove combination of observer to observable 
         */
        public bool RemoveObserver(IObserver observer, Observable observable)
        {
            return observers.Remove(observable, observer);
        }

        /**
         * Set of all observers that observe the observable
         */
        public IReadOnlyCollection<IObserver> GetObservers(Observable observable)
        {
            return new ReadOnlyCollection<IObserver>(new List<IObserver>(observers[observable])); //ImmutableSet.copyOf(observers.get(observable));
        }

        /**
         * Adds the combination of model to observable
         * @param Model the model that is updated by the observable
         * @param Observable the observable to monitor
         */
        public void AddModel(Model model, Observable observable)
        {
            models.Put(observable, model);
        }

        public bool RemoveModel(Model model, Observable observable)
        {
            return models.Remove(observable, model);
        }

        public IReadOnlyCollection<Model> GetModels(Observable observable)
        {
            return (IReadOnlyCollection<Model>)models.Get(observable);
        }

        /**
         * Adds the combination of trigger to observable
         * @param Triggerable the trigger that tracks the observable
         * @param Observable the observable to monitor
         */
        public void AddTrigger(ITriggerable trigger, Observable observable)
        {
            triggers.Put(observable, trigger);
        }

        public bool RemoveTrigger(ITriggerable trigger, Observable observable)
        {
            return triggers.Remove(observable, trigger);
        }

        public IReadOnlyCollection<ITriggerable> GetTriggers(Observable observable)
        {
            return (IReadOnlyCollection<ITriggerable>)triggers.Get(observable);
        }

        public void InformTriggers(GameState state, Change change)
        {

            // Inform direct triggers
            foreach (ITriggerable t in GetTriggers(state))
            {
                t.Triggered(state, change);
                log.Debug("State " + state + " sends change to Trigger " + t);
            }

            // check if there are models
            IReadOnlyCollection<Model> initModels = GetModels(state);
            if (initModels.Count == 0) return;
            IReadOnlyCollection<Model> allModels = GetModelsToUpdate(initModels);

            // Inform indirect triggers
            foreach (Model m in allModels)
            {
                foreach (ITriggerable t in GetTriggers(m))
                {
                    t.Triggered(m, change);
                    log.Debug("Model " + m + " sends change to Trigger " + t);
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
        public IReadOnlyCollection<Model> GetModelsToUpdate<T>(IEnumerable<T> observables) where T : Observable
        {
            // Topological sort
            // Initialize (we do not use WHITE explicitly, but implicit)
            Dictionary<Observable, Color> colors = new Dictionary<Observable, Color>();
            LinkedList<Model> topoList = new LinkedList<Model>();

            // For all states
            foreach (Observable s in observables)
            {
                TopoSort(s, colors, topoList);
            }
            return new ReadOnlyCollection<Model>(new List<Model>(topoList)); //ImmutableList.copyOf(topoList);
        }

        private enum Color { WHITE, GREY, BLACK };
        private void TopoSort(Observable v, Dictionary<Observable, Color> colors, LinkedList<Model> topoList)
        {
            colors[v] = Color.GREY;
            foreach (Model m in GetModels(v))
            {
                if (!colors.ContainsKey(m))
                {
                    TopoSort(m, colors, topoList);
                }
                else if (colors[m] == Color.GREY)
                {
                    throw new InvalidOperationException("Graph of Observables contains Cycle");
                }
            }
            colors[v] = Color.BLACK;
            if (v is Model) topoList.AddFirst((Model)v);
        }

        public void UpdateObservers(IEnumerable<GameState> states)
        {
            // all direct observers
            foreach (GameState s in states)
            {
                IReadOnlyCollection<IObserver> observers = GetObservers(s);
                if (observers.Count == 0) continue;
                // cache StateText
                string stateText = s.ToText();
                foreach (IObserver o in observers)
                {
                    o.Update(stateText);
                    log.Debug("State " + s + " updates observer " + o);
                }
            }

            // all indirect observers
            foreach (Model m in GetModelsToUpdate(states))
            {
                IReadOnlyCollection<IObserver> observers = GetObservers(m);
                if (observers.Count == 0) continue;
                // cache ModelText
                string modelText = m.ToText();
                foreach (IObserver o in observers)
                {
                    o.Update(modelText);
                    log.Debug("Model " + m + " updates observer " + o);
                }
            }
        }

        // StateManager getters for sub-components
        //////////////////////////////////////////

        public ChangeStack ChangeStack
        {
            get
            {
                return changeStack;
            }
        }

        public PortfolioManager PortfolioManager
        {
            get
            {
                return portfolioManager;
            }
        }

        public WalletManager WalletManager
        {
            get
            {
                return walletManager;
            }
        }

    }
}
