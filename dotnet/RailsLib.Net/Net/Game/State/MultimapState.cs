using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace GameLib.Net.Game.State
{
    // #TODO_needs_lots_of_testing
    public abstract class MultimapState<K, V> : GameState, IEnumerable<V>
    {
        protected MultimapState(IItem parent, string id) : base(parent, id)
        {
        }

        // Helper function to return the Multimap from the classes below
        protected abstract Wintellect.PowerCollections.MultiDictionaryBase<K, V> GetMap();

        /**
         * Stores a key-value pair in the multimap
         * @param key key to store
         * @param value value to store
         * @return true if key-value pair is added, or false if the key value pair already exists
         */
        public bool Put(K key, V value)
        {
            if (GetMap().Contains(key, value)) return false;
            new MultimapChange<K, V>(this, key, value, true);
            return true;
        }

        public ICollection<V> Get(K key)
        {
            var hs = new List<V>(GetMap()[key]);
            return hs;  //ImmutableSet.copyOf(getMap().get(key));
        }
        public bool Remove(K key, V value)
        {
            if (!GetMap().Contains(key, value)) return false;
            new MultimapChange<K, V>(this, key, value, false);
            return true;
        }

        public ISet<V> RemoveAll(K key)
        {
            ISet<V> values = new HashSet<V>(Get(key));
            foreach (V value in values)
            {
                this.Remove(key, value);
            }
            return values;
        }

        public bool ContainsEntry(K key, V value)
        {
            return GetMap().Contains(key, value);
        }

        public bool ContainsKey(K key)
        {
            return GetMap().ContainsKey(key);
        }

        public bool ContainsValue(V value)
        {
            return GetMap().Values.Contains(value);
        }

        public int Count
        {
            get
            {
                return GetMap().Values.Count;
            }
        }

        public bool IsEmpty
        {
            get
            {
                return GetMap().Count == 0;
            }
        }

        public IReadOnlyCollection<K> KeySet()
        {
            return new ReadOnlyCollection<K>(new List<K>(GetMap().Keys));
            //return ImmutableHashSet.ToImmutableHashSet(new HashSet<K>(GetMap().Keys));   //ImmutableSet.copyOf(getMap().keySet());
        }

        /**
         * @return all values of the multimap
         */
        public IReadOnlyCollection<V> Values()
        {
            // ImmutableCollection.copyOf does not exist, uses List instead
            return new ReadOnlyCollection<V>(new List<V>(GetMap().Values));
            //return ImmutableHashSet.ToImmutableHashSet(new HashSet<V>(GetMap().Values)); //ImmutableList.copyOf(getMap().values());
        }

        /**
         * creates an immutable copy of the Multimap
         * @return immutable version of the Multimap
         */

        // #TODO_readonly_multimap
        //public ImmutableMultimap<K, V> view()
        //{
        //    return ImmutableMultimap.copyOf(getMap());
        //}
        public Wintellect.PowerCollections.MultiDictionaryBase<K, V> View()
        {
            //var ret = new Wintellect.PowerCollections.MultiDictionaryBase<K, V>(true);

            return Clone(); //ImmutableMultimap.copyOf(getMap());
        }

        abstract protected Wintellect.PowerCollections.MultiDictionaryBase<K, V> Clone();

        /**
         * @return an iterator over all values
         */
        public IEnumerator<V> GetEnumerator()
        {
            return new List<V>(Values()).GetEnumerator(); //ImmutableList.copyOf(getMap().values()).iterator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        override public string ToText()
        {
            return GetMap().ToString();
        }

        public void Change(K key, V value, bool addToMap)
        {
            if (addToMap)
            {
                GetMap().Add(key, value);
            }
            else
            {
                GetMap().Remove(key, value);
            }
        }
    }
}
