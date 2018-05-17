using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;

/**
 * A stateful version of an List<T>
 * TODO: Add all methods of List interface
 */

namespace GameLib.Net.Game.State
{
    public class ListState<T> : GameState, IEnumerable<T>
    {
        private readonly List<T> list;

        private ListState(IItem parent, string id, IEnumerable<T> collection) : base(parent, id)
        {
            if (collection == null) list = new List<T>();
            else list = new List<T>(collection);
        }

        /** 
         * Creates empty ArrayListState 
         */
        public static ListState<T> Create(IItem parent, string id)
        {
            return new ListState<T>(parent, id, null);
        }

        /**
         * Creates a prefilled ArrayListState
         */
        public static ListState<T> Create(IItem parent, string id, IEnumerable<T> collection)
        {
            return new ListState<T>(parent, id, collection);
        }

        /**
         * Appends the specified element to the end of the list
         * @param element to be appended
         * @return true (similar to the general contract of Collection.add)
         */
        public bool Add(T element)
        {
            new ListChange<T>(this, element, list.Count);
            return true;
        }

        /**
         * Inserts specified element at the specified position.
         * @param element to be added
         * @throws IndexOutOfBoundsException if index is out of range
         */
        public void Add(int index, T element)
        {
            if (index < 0 || index > list.Count) throw new IndexOutOfRangeException();
            // if bounds ok, generate change
            new ListChange<T>(this, element, index);
        }

        public bool Remove(T element)
        {
            // check first if element exists
            if (!list.Contains(element)) return false;
            new ListChange<T>(this, list.IndexOf(element));
            return true;
        }

        /**
         * remove element at index position
         * @param index position
         * @return element removed
         * @throws IndexOutOfBoundsException if index is out of range
         */
        public T Remove(int index)
        {
            if (index < 0 || index > list.Count) throw new IndexOutOfRangeException();
            T element = list[index];
            // if bounds ok, generate change
            new ListChange<T>(this, index);
            return element;
        }

        /**
         * move element to a new index position in the list
         * Remark: index position relative to the list after removal of the element
         * @param element the specified element 
         * @param index of the new position
         * @return true if the list contained the specified element
         * @throws IndexOutOfBoundsException if the new index is out of range (0 <= index < size) 
         */
        public bool Move(T element, int index)
        {
            if (index < 0 || index > list.Count - 1) throw new IndexOutOfRangeException();
            // if bounds ok, start move
            bool remove = Remove(element);
            if (remove)
            { // only if element exists, execute move
                Add(index, element);
            }
            return remove;
        }

        public bool Contains(T element)
        {
            return list.Contains(element);
        }

        /**
         * removes all elements 
         */
        public void Clear()
        {
            foreach (T element in new List<T>(list))
            {
                Remove(element);
            }
        }

        /**
         * make the list identical to the argument list
         */
        public void SetTo(IEnumerable<T> newList)
        {
            int index = 0;
            List<T> copyList = new List<T>(list);
            foreach (T element in newList)
            {
                if (index < copyList.Count)
                {
                    if (element.Equals(copyList[index]))
                    {
                        // elements are equal, no change required
                        index++; continue;
                    }
                    else
                    {
                        // elements are unequal, so remove old element
                        new ListChange<T>(this, index);
                    }
                }
                new ListChange<T>(this, element, index);
                index++;
            }
            // remove all remaining elements if original list is larger
            for (; index < copyList.Count; index++)
            {
                new ListChange<T>(this, index);
            }
        }

        /**
         * creates an immutable view of the list
         * @return immutable copy
         */
        public IReadOnlyCollection<T> View()
        {
            return new ReadOnlyCollection<T>(new List<T>(list));
        }

        public int Count
        {
            get
            {
                return list.Count;
            }
        }

        public bool IsEmpty
        {
            get
            {
                return Count == 0;
            }
        }

        public int IndexOf(T o)
        {
            return list.IndexOf(o);
        }

        public T Get(int index)
        {
            return list[index];
        }

        /**
         * creates an iterator derived from the ImmutableCopy of the ArrayListState
         * @return a suitable iterator for ArrayListState
         */
        public IEnumerator<T> GetEnumerator()
        {
            return new List<T>(View()).GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return new List<T>(View()).GetEnumerator();
        }

        override public String ToText()
        {
            return list.ToString();
        }

        public void Change(T o, int index, bool addToList)
        {
            if (addToList)
            {
                list.Insert(index, o);
            }
            else
            {
                list.RemoveAt(index);
            }
        }
    }
}
