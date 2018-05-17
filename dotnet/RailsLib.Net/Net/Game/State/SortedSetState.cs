using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.State
{
    public class SortedSetState<T> : SetState<T> where T : IOwnable
    {
        private SortedSet<T> set;

        private SortedSetState(IItem parent, string id, ICollection<T> collection) : base(parent, id)
        {
            
            if (collection == null)
            {
                set = new SortedSet<T>();
            }
            else
            {
                set = new SortedSet<T>(collection);
            }
        }

        /**
         * @return empty TreeSetState
         */
        public static SortedSetState<T> Create(IItem parent, string id)
        {
            return new SortedSetState<T>(parent, id, null);
        }

        /**
         * @return prefilled TreeSetState
         */
        public static SortedSetState<T> Create(IItem parent, string id, ICollection<T> collection)
        {
            return new SortedSetState<T>(parent, id, collection);
        }

        override public ISet<T> GetSet()
        {
            return set;
        }
    }
}
