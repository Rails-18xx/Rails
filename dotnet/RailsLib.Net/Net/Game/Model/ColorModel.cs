using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

/**
 * ColourModel defines colors for UI components
 */


namespace GameLib.Net.Game.Model
{
    abstract public class ColorModel : State.Model
    {
        protected ColorModel(IItem parent, string id) : base(parent, id)
        {
            
        }

        public abstract Color Background { get; }

        public abstract Color Foreground { get; }
    }
}
