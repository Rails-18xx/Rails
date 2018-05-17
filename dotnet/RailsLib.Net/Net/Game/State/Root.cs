using GameLib.Net.Util;
using System;
using System.Collections.Generic;

/**
* Root is the top node of the context/item hierachy
*/

namespace GameLib.Net.Game.State
{
    public class Root : Context
    {
        public static String ID = "";
        private static String TEXT_ID = "root";

        private StateManager stateManager;
        private DictionaryState<string, IItem> items;

        // only used during creation
        private bool delayItems = true;
        private List<IItem> delayedItems = new List<IItem>();

        protected Root()
        {
            AddItem(this);
        }

        /**
         * @return a Root object with everything initialized (including sub-components like StateManager)
         */
        public static Root Create()
        {
            // precise sequence to avoid any uninitialized problems
            Root root = new Root();
            root.Init();
            return root;
        }

        protected void Init()
        {
            StateManager stateManager = StateManager.Create(this, "states");
            this.stateManager = stateManager;
            stateManager.Init();
            InitDelayedItems();
        }

        private void InitDelayedItems()
        {
            items = DictionaryState<string, IItem>.Create(this, "items");
            foreach (IItem item in delayedItems)
            {
                items.Put(item.FullURI, item);
            }
            delayItems = false;
        }

        public StateManager StateManager
        {
            get
            {
                return stateManager;
            }
        }

        // Item methods

        /**
         * @throws UnsupportedOperationsException
         * Not supported for Root
         */
        override public IItem Parent
        {
            get
            {
                throw new NotSupportedException();
            }
        }

        override public string Id
        {
            get
            {
                return "";
            }
        }

        /**
         * @return this
         */
        override public Context GetContext
        {
            get
            {
                return this;
            }
        }

        /**
         * @return this
         */
        override public Root GetRoot
        {
            get
            {
                return this;
            }
        }

        override public string URI
        {
            get
            {
                return "";
            }
        }

        override public string FullURI
        {
            get
            {
                return "";
            }
        }

        override public string ToText()
        {
            return TEXT_ID;
        }

        // Context methods
        override public IItem Locate(string uri)
        {
            // first try as fullURI
            IItem item = items.Get(uri);
            if (item != null) return item;
            // otherwise as local
            return items.Get(IItemConsts.SEP + uri);
        }

        // used by other context
        public IItem LocateFullURI(string uri)
        {
            return items.Get(uri);
        }

        public override void AddItem(IItem item)
        {
            // check if it has to be delayed
            if (delayItems)
            {
                delayedItems.Add(item);
                return;
            }

            // check if it already exists
            Precondition.CheckArgument(!items.ContainsKey(item.FullURI),
                    "Root already contains item with identical fullURI = " + item.FullURI);

            // all preconditions ok => add
            items.Put(item.FullURI, item);
        }

        public override void RemoveItem(IItem item)
        {
            // check if it already exists
            Precondition.CheckArgument(items.ContainsKey(item.FullURI),
                    "Root does not contain item with that fullURI = " + item.FullURI);

            // all preconditions ok => remove
            items.Remove(item.FullURI);
        }

       override public string ToString()
        {
            return TEXT_ID;
        }


    }
}
