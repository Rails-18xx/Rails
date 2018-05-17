using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Game.State
{
    public sealed class GenericStateChange<T> : Change
    {
        private readonly GenericState<T> state;
        private readonly T previous, next;

        public GenericStateChange(GenericState<T> state, T o)
        {
            this.state = state;
            previous = state.Value;
            next = o;
            base.Init(state);
        }

        override public void Execute()
        {
            state.Change(next);
        }

        override public void Undo()
        {
            state.Change(previous);
        }

        public override GameState GameState
        {
            get
            {
                return state;
            }
        }

        override public string ToString()
        {
            return "Change for " + state + ": Replace " + previous + " by " + next;
        }
    }
}
