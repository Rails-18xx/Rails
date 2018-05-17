using GameLib.Net.Util;
using System;
using System.Collections.Generic;

/**
 * Requirement:
 * The observable object has to call each observer per update() if the object has changed.
 */
namespace GameLib.Net.Game.State
{
    abstract public class Observable : IItem
    {
        // fields for Item implementation
        private string id;
        private IItem parent;
        private Context context;

        /**
         * @param parent parent node in item hierarchy (cannot be null)
         * @param id id of the observable
         * If id is null it creates an "unobservable" observable
         * This is required for the creation of states that are themselves stateless
         */

        protected Observable(IItem parent, string id)
        {
            Precondition.CheckNotNull(parent, "Parent cannot be null");
            Precondition.CheckNotNull(id, "Id cannot be null");

            // defined standard fields
            this.parent = parent;
            this.id = id;

            if (parent is Context)
            {
                context = (Context)parent;
            }
            else
            {
                // recursive definition
                context = parent.GetContext;
            }

            context.AddItem(this);
        }

        // has to be delayed as at the time of initialization the complete link is not yet defined
        internal StateManager StateManager
        {
            get
            {
                return context.GetRoot.StateManager;
            }
        }

        public void AddObserver(IObserver o)
        {
            StateManager.AddObserver(o, this);
        }

        public bool RemoveObserver(IObserver o)
        {
            return StateManager.RemoveObserver(o, this);
        }

        public IReadOnlyCollection<IObserver> GetObservers()
        {
            return StateManager.GetObservers(this);
        }

        public void AddModel(Model m)
        {
            StateManager.AddModel(m, this);
        }

        public bool RemoveModel(Model m)
        {
            return StateManager.RemoveModel(m, this);
        }

        public IReadOnlyCollection<Model> GetModels()
        {
            return StateManager.GetModels(this);
        }

        public void AddTrigger(ITriggerable m)
        {
            StateManager.AddTrigger(m, this);
        }

        public bool RemoveTrigger(ITriggerable m)
        {
            return StateManager.RemoveTrigger(m, this);
        }

        public IReadOnlyCollection<ITriggerable> GetTriggers()
        {
            return StateManager.GetTriggers(this);
        }

        /**
         * Text to delivered to Observers
         * Default is defined to be identical with toString()
         * @return text for observers
         */
        virtual public string ToText()
        {
            return this.ToString();
        }

        // Item methods

        public string Id
        {
            get
            {
                return id;
            }
        }

        public IItem Parent
        {
            get
            {
                return parent;
            }
        }

        public Context GetContext
        {
            get
            {
                return context;
            }
        }

        public Root GetRoot
        {
            get
            {
                // forward it to the context
                return context.GetRoot;
            }
        }

        public string URI
        {
            get
            {
                if (parent is Context)
                {
                    return id;
                }
                else
                {
                    // recursive definition
                    return parent.URI + IItemConsts.SEP + id;
                }
            }
        }

        public string FullURI
        {
            get
            {
                // recursive definition
                return parent.FullURI + IItemConsts.SEP + id;
            }
        }

        override public string ToString()
        {
            //return Objects.toStringHelper(this).add("uri", getFullURI()).toString();
            return $"{this.GetType().Name}{{uri={FullURI}}}";
        }

    }
}
