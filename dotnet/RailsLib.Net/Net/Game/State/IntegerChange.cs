using System;


/**
 * Change associated with IntegerState
 */

namespace GameLib.Net.Game.State
{
    public sealed class IntegerChange : Change
    {
        private readonly IntegerState state;
        private readonly int newValue;
        private readonly int oldValue;

        public IntegerChange(IntegerState state, int newValue)
        {
            this.state = state;
            this.newValue = newValue;
            this.oldValue = state.Value;
            base.Init(state);
        }

        override public void Execute()
        {
            state.Change(newValue);
        }

        override public void Undo()
        {
            state.Change(oldValue);
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
            return "Change for " + state + ": From " + oldValue + " to " + newValue;
        }
    }
}
