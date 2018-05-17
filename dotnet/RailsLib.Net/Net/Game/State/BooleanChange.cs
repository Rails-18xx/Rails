using System;

/**
 * Change associated with BooleanState
 */
namespace GameLib.Net.Game.State
{
    public sealed class BooleanChange : Change
    {
        private readonly BooleanState state;
        private readonly bool newValue;
        private readonly bool oldValue;

        public BooleanChange(BooleanState state, bool newValue)
        {
            this.state = state;
            this.newValue = newValue;
            this.oldValue = state.Value;
            base.Init(state);
        }

        public override void Execute()
        {
            state.Change(newValue);
        }

        public override void Undo()
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

        override public String ToString()
        {
            return "Change for " + state + ": From " + oldValue + " to " + newValue;
        }
    }
}
