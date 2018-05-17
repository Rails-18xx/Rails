using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    public class ModelImpl : Model
    {
        private StringState text;


        private ModelImpl(IItem parent, string id, string text) : base(parent, id)
        {
            this.text = StringState.Create(this, "text");

            this.text.Set(text);
        }

        public static ModelImpl Create(IItem parent, string id, string text)
        {
            return new ModelImpl(parent, id, text);
        }

        public void ChangeText(string text)
        {
            this.text.Set(text);
        }

        public GameState GameState
        {
            get
            {
                return text;
            }
        }

        override public string ToText()
        {
            return text.Value;
        }
    }
}
