using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace GameLib.Net.Game.State
{
    abstract public class SetState<T> : GameState, IEnumerable<T>
    {
        protected SetState(IItem parent, string id) : base(parent, id)
        {
        }

        public abstract ISet<T> GetSet();

        /**
         * add element
         * @param element
         */
        public void Add(T element)
        {
            new SetChange<T>(this, element, true);
        }

        /**
         * remove element
         * @param element to remove
         * @return true = was part of the HashSetState
         */
        public bool Remove(T element)
        {
            if (GetSet().Contains(element))
            {
                new SetChange<T>(this, element, false);
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * @param element
         * @return true = element exists in HashSetState
         */

        public bool Contains(T element)
        {
            return GetSet().Contains(element);
        }

        /**
         * removes all elements
         */
        public void Clear()
        {
            foreach (T element in new List<T>(GetSet()))
            {
                Remove(element);
            }
        }

        /**
         * @return immutable view of getSet()
         */
        public IReadOnlyCollection<T> View()
        {
            return new ReadOnlyCollection<T>(new List<T>(GetSet()));
            //return ImmutableHashSet.ToImmutableHashSet(new HashSet<T>(GetSet())); // ImmutableSet.copyOf(getSet());
        }

        /**
         * @return number of elements in HashSetState
         */
        public int Count
        {
            get
            {
                return GetSet().Count;
            }
        }

        /**
         * @return true if HashSetState is empty
         */
        public bool IsEmpty
        {
            get
            {
                return GetSet().Count == 0;
            }
        }

        public IEnumerator<T> GetEnumerator()
        {
            return new List<T>(GetSet()).GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        override public string ToText()
        {
            return GetSet().ToString();
        }

        public void Change(T element, bool addToSet)
        {
            if (addToSet)
            {
                GetSet().Add(element);
            }
            else
            {
                GetSet().Remove(element);
            }
        }
    }
}
