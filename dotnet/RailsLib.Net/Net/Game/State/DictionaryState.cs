using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
/**
 * A stateful version of a HashMap
 * 
 * It allows automatic iteration over it values
 */

namespace GameLib.Net.Game.State
{
    public class DictionaryState<K, V> : MapState<K, V>
    {
        private Dictionary<K, V> map;

        private DictionaryState(IItem parent, string id, IDictionary<K, V> map) : base(parent, id)
        {
            if (map == null)
            {
                this.map = new Dictionary<K, V>();
            }
            else
            {
                this.map = new Dictionary<K, V>(map);
            }
        }

        /**
         * creates an empty DictionaryState
         * @return empty DictionaryState
         */
        public static DictionaryState<K, V> Create(IItem parent, string id)
        {
            return new DictionaryState<K, V>(parent, id, null);
        }

        /**
         * creates an initialized (filled) DictionaryState
         * @param map used for initialization
         * @return initialized DictionaryState
         */
        public static DictionaryState<K, V> Create(IItem parent, String id, IDictionary<K, V> map)
        {
            return new DictionaryState<K, V>(parent, id, map);
        }

        override protected IDictionary<K, V> GetMap()
        {
            return map;
        }

        /**
         * creates an immutable copy of the values
         * @return immutable list of values
         */
        override public IReadOnlyCollection<V> ViewValues()
        {
            return new List<V>(map.Values); //ImmutableList.copyOf(map.values());
        }
    }
}
