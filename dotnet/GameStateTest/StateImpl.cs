using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    public class StateImpl : GameState
    {
        private string text;
    
    private StateImpl(IItem parent, string id, string text) : base(parent, id)
        {
            this.text = text;
        }

        public static StateImpl Create(IItem parent, string id, string text)
        {
            return new StateImpl(parent, id, text);
        }

    override public string ToText()
        {
            return text;
        }

    override public string ToString()
        {
            return text;
        }
    }
}
