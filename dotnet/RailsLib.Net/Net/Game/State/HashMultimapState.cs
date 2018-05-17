using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Wintellect.PowerCollections;

namespace GameLib.Net.Game.State
{
    public class HashMultimapState<K, V> : MultimapState<K, V>
    {
        private readonly Wintellect.PowerCollections.MultiDictionary<K, V> map;

        private HashMultimapState(IItem parent, string id) : base(parent, id)
        {
            map = new Wintellect.PowerCollections.MultiDictionary<K, V>(true);
        }

        /** 
         * Creates an empty HashMultimapState 
         */
        public static HashMultimapState<K, V> Create(IItem parent, String id)
        {
            return new HashMultimapState<K, V>(parent, id);
        }

        protected override MultiDictionaryBase<K, V> Clone()
        {
            return map.Clone();
        }

        protected override MultiDictionaryBase<K, V> GetMap()
        {
            return map;
        }
    }
}
