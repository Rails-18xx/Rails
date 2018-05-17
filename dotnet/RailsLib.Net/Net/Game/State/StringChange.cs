using System;
using System.Collections.Generic;
using System.Text;

/**
 * Change associated with StringState
 * @author freystef

 */
namespace GameLib.Net.Game.State
{
    public class StringChange : Change
    {
        private StringState state;
        private string newValue;
        private string oldValue;

        public StringChange(StringState state, string newValue)
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
        //override public StringState getState()
        //{
        //    return state;
        //}

        override public string ToString()
        {
            return "Change for " + state + ": From \"" + oldValue + "\" to \"" + newValue + "\"";
        }

    }
}
