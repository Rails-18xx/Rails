using System;


namespace GameLib.Net.Game.State
{
    public sealed class SetChange<T> : Change
    {
        private readonly SetState<T> state;
        private readonly T element;
        private readonly bool addToSet;

        /**
         * Add/Remove element to/from the set
         */
        public SetChange(SetState<T> state, T element, bool addToSet)
        {
            this.state = state;
            this.element = element;
            this.addToSet = addToSet;
            base.Init(state);
        }

        public override void Execute()
        {
            state.Change(element, addToSet);
        }

        public override void Undo()
        {
            state.Change(element, !addToSet);
        }

        override public GameState GameState
        {
            get
            {
                return state;
            }
        }

        public bool IsAddToSet
        {
            get
            {
                return addToSet;
            }
        }

        public T Element
        {
            get
            {
                return element;
            }
        }

        override public string ToString()
        {
            if (addToSet)
            {
                return "Change for " + state + ": Add " + element;
            }
            else
            {
                return "Change for " + state + ": Remove " + element;
            }
        }

    }
}
