using System;

/**
 * Change associated with ArrayListState
 */

namespace GameLib.Net.Game.State
{
    public sealed class ListChange<T> : Change
    {
        private readonly ListState<T> state;
        private readonly T item;
    private readonly int index;
        private readonly bool addToList;

    /**
     * Add object at the specified index
     */
    public ListChange(ListState<T> state, T o, int index)
        {
            this.state = state;
            this.item = o;
            this.index = index;
            this.addToList = true;
            base.Init(state);
        }

        /**
         * Remove object at the specified index
         */
        public ListChange(ListState<T> state, int index)
        {
            this.state = state;
            this.item = state.Get(index);
            this.index = index;
            this.addToList = false;
            base.Init(state);
        }

        override public void Execute()
        {
            state.Change(item, index, addToList);
        }

        override public void Undo()
        {
            state.Change(item, index, !addToList);
        }

    override public GameState GameState
        {
            get
            {
                return state;
            }
        }

    override public string ToString()
        {
            if (addToList)
            {
                return "Change for " + state + ": Add " + item + " at index " + index;
            }
            else
            {
                return "Change for " + state + ": Remove " + item + " at index " + index;
            }
        }
    }
}
