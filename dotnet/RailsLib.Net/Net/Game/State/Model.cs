using System;


namespace GameLib.Net.Game.State
{
    abstract public class Model : Observable
    {
        protected Model(IItem parent, string id) : base(parent, id)
        {

        }
    }
}
