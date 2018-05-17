using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Stateful version of a BiMap
 */

namespace GameLib.Net.Game.State
{
    public class BiDictionaryState<K, V> : MapState<K, V>
    {
        private BiDictionary<K, V> map;

        private BiDictionaryState(IItem parent, string id, IDictionary<K, V> map) : base(parent, id)
        {
            if (map == null)
            {
                this.map = new BiDictionary<K, V>();
            }
            else
            {
                this.map = new BiDictionary<K, V>(map);
            }
        }

        /**
         * creates an empty BiMapState
         * @return empty BiMapState
         */
        public static BiDictionaryState<K, V> Create(IItem parent, string id)
        {
            return new BiDictionaryState<K, V>(parent, id, null);
        }

        /**
         * creates an initialized (filled) BiMapState
         * @param map used for initialization
         * @return initialized BiMapState
         */
        public static BiDictionaryState<K, V> Create(IItem parent, string id, IDictionary<K, V> map)
        {
            return new BiDictionaryState<K, V>(parent, id, map);
        }

        override protected IDictionary<K, V> GetMap()
        {
            return map;
        }

        // TODO: Check if value is already in map
        //   public V put(K key, V value) 

        /**
         * creates an immutable copy of the biMap
         * @return immutable version of the biMap
         */
        new public BiDictionary<K, V> View()
        {
            return new BiDictionary<K, V>(map); //ImmutableBiMap.copyOf(map);
        }

        /**
         * creates an immutable copy of the values
         * @return immutable list of values
         */
        override public IReadOnlyCollection<V> ViewValues()
        {
            return new List<V>(map.Values);
        }
    }
}
