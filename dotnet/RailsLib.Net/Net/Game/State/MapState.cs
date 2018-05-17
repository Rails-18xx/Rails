using GameLib.Net.Util;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;

/**
 * MapState is an abstract parent class for both HashMapState and HashBiMapState 
 */
namespace GameLib.Net.Game.State
{
    abstract public class MapState<K, V> : GameState, IEnumerable<V>
    {
        protected MapState(IItem parent, string id) : base(parent, id)
        {
            
        }

        protected abstract IDictionary<K, V> GetMap();

        /**
         * Add key,value pair to getMap()
         * @param key for mapping
         * @param value associated with key 
         * @return previous value associated with specified key, or null if there was no mapping for the key (or null was the value).
         */
        public V Put(K key, V value)
        {
            // check if the key is in the getMap()
            if (GetMap().ContainsKey(key))
            {
                V oldValue = GetMap()[key];
                // check if element already has the specified value
                if (!oldValue.Equals(value))
                {
                    new DictionaryChange<K, V>(this, key, value);
                }
                return oldValue;
            }
            else
            {
                // if not in getMap(), add tuple and return null
                new DictionaryChange<K, V>(this, key, value);
                return default(V);
            }
        }

        /**
         * Adds all (key,value) pairs
         * @param getMap() that gets added
         * @throws NullPointerException if getMap() is null
         */
        public void PutAll(IDictionary<K, V> map)
        {
            Precondition.CheckNotNull(map);
            foreach (K key in map.Keys)
            {
                Put(key, map[key]);
            }
        }

        /**
         * return value for specified key
         * @param key used to retrieve value
         * @return value associated with the key, null if getMap() does not contain key
         */
        public V Get(K key)
        {
            V ret;
            GetMap().TryGetValue(key, out ret);
            //return GetMap()[key];
            return ret;
        }

        /**
         * removes key from mapping
         * @param key to be removed from getMap()
         * @return value previously associated with key, null if getMap() did not contain key
         */
        public V Remove(K key)
        {
            // check if getMap() contains key
            if (!GetMap().ContainsKey(key)) return default(V);
            V old = GetMap()[key];
            new DictionaryChange<K, V>(this, key);
            return old;
        }

        /**
         * test if key is present in mapping
         * @param key whose presence is tested
         * @return true if key is present
         */
        public bool ContainsKey(K key)
        {
            return GetMap().ContainsKey(key);
        }

        /**
         * removes all mappings from the getMap()
         */
        public void Clear()
        {
            foreach (K key in new List<K>(GetMap().Keys))
            {
                Remove(key);
            }
        }

        /**
         * checks if getMap() is empty
         * @return true if getMap() is empty
         */
        public bool IsEmpty()
        {
            return GetMap().Count == 0;
        }

        /**
         * @return number of elements
         */
        public int Count
        {
            get
            {
                return GetMap().Count;
            }
        }

        /**
         * (re)initializes the state getMap() from another getMap()
         * @param getMap() used for initialization
         */
        public void InitFromMap(IDictionary<K, V> initMap)
        {
            // all from initMap get added
            PutAll(initMap);
            var a = GetMap().Keys.Except(initMap.Keys);
            // remove those only in current map
            foreach (K key in new List<K>(GetMap().Keys.Except(initMap.Keys)))
            {
                Remove(key);
            }
        }

        /**
         * creates an immutable copy of the getMap()
         * @return immutable version of the getMap()
         */
        public ReadOnlyDictionary<K, V> View()
        {
            return new ReadOnlyDictionary<K, V>(GetMap());//.ToImmutableDictionary();
        }

        /**
         * creates an immutable copy of the keyset
         * @return immutable keyset of the getMap()
         */
        public IReadOnlyCollection<K> ViewKeys()
        {
            return new ReadOnlyDictionary<K, V>(new Dictionary<K, V>(GetMap())).Keys; //ImmutableSet.copyOf(getMap().keySet());
        }

        public abstract IReadOnlyCollection<V> ViewValues();

        public IEnumerator<V> GetEnumerator()
        {
            return ViewValues().GetEnumerator();
        }

        public void Change(K key, V value, bool remove)
        {
            if (remove)
            {
                GetMap().Remove(key);
            }
            else
            {
                GetMap()[key] = value;
            }
        }

        override public string ToText()
        {
            return GetMap().ToString();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }
    }
}
