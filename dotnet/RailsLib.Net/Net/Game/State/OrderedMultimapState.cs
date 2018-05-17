using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

namespace GameLib.Net.Game.State
{
    public class OrderedMultimapState<K, V> : MultimapState<K, V>
        where K : IComparable
        where V : IComparable
    {
        private OrderedMultiDictionary<K, V> map = new OrderedMultiDictionary<K, V>(true);

        private OrderedMultimapState(IItem parent, string id) : base(parent, id)
        {
            
        }

        /** 
         * Creates an empty TreeMultimapState 
         */
        public static OrderedMultimapState<K, V> Create(IItem parent, string id)
        {
            return new OrderedMultimapState<K, V>(parent, id);
        }

        protected override MultiDictionaryBase<K, V> Clone()
        {
            return map.Clone();
        }

        override protected MultiDictionaryBase<K, V> GetMap()
        {
            return map;
        }

    //    public ImmutableSortedSet<V> get(K key)
    //    {
    //        return ImmutableSortedSet.copyOf(map.get(key));
    //    }

    //    @Override
    //public ImmutableSortedSet<K> keySet()
    //    {
    //        return ImmutableSortedSet.copyOf(map.keySet());
    //    }

    //    @Override
    //public ImmutableSortedSet<V> values()
    //    {
    //        return ImmutableSortedSet.copyOf(map.values());
    //    }

    //    @Override
    //public ImmutableSetMultimap<K, V> view()
    //    {
    //        return ImmutableSetMultimap.copyOf(map);
    //    }
    }
}
