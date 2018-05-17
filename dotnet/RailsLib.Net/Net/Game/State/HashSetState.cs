using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Game.State
{
    public class HashSetState<T> : SetState<T>
    {
        private readonly HashSet<T> set;

        private HashSetState(IItem parent, string id, IEnumerable<T> collection) : base(parent, id)
        {
            if (collection == null)
            {
                set = new HashSet<T>();
            }
            else
            {
                set = new HashSet<T>(collection);
            }
        }

        /**
         * @return empty HashSetState
         */
        public static HashSetState<T> Create(IItem parent, string id)
        {
            return new HashSetState<T>(parent, id, null);
        }

        /**
         * @return prefilled HashSetState
         */
        public static HashSetState<T> Create(IItem parent, string id, IEnumerable<T> collection)
        {
            return new HashSetState<T>(parent, id, collection);
        }

        override public ISet<T> GetSet()
        {
            return set;
        }
    }
}
